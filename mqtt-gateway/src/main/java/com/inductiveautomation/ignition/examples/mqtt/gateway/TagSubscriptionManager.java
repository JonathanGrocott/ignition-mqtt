package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Manages tag subscriptions and handles tag value polling.
 * 
 * Uses a polling mechanism to periodically read tag values and publish changes to MQTT.
 * This is simpler than event-driven subscriptions and works reliably with the Ignition 8.3 SDK.
 */
public class TagSubscriptionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TagSubscriptionManager.class);
    
    private final GatewayContext gatewayContext;
    private final MqttPublisherManager publisherManager;
    private final MqttTopicMapper topicMapper;
    private final JsonPayloadBuilder payloadBuilder;
    private final ModuleStatistics statistics;
    
    private TagPublishConfig config;
    private final Map<TagPath, QualifiedValue> lastPublishedValues = new ConcurrentHashMap<>();
    private final List<TagPath> monitoredTags = Collections.synchronizedList(new ArrayList<>());
    
    private ScheduledExecutorService pollExecutor;
    private ScheduledFuture<?> pollTask;
    
    /**
     * Creates a new tag subscription manager
     * 
     * @param context Gateway context
     * @param publisherManager MQTT publisher manager
     * @param statistics Module statistics tracker
     */
    public TagSubscriptionManager(GatewayContext context, MqttPublisherManager publisherManager, ModuleStatistics statistics) {
        this.gatewayContext = context;
        this.publisherManager = publisherManager;
        this.statistics = statistics;
        this.topicMapper = new MqttTopicMapper();
        this.payloadBuilder = new JsonPayloadBuilder();
    }
    
    /**
     * Starts monitoring tags based on the configuration
     * 
     * @param config Tag publishing configuration
     */
    public void start(TagPublishConfig config) {
        this.config = config;
        
        if (!config.isEnabled()) {
            logger.info("Tag publishing is disabled");
            return;
        }
        
        long pollRateMs = config.getPollRateMs();
        logger.info("Starting tag subscription manager with poll rate: {}ms", pollRateMs);
        
        // Discover tags to monitor
        List<TagPath> tags = discoverTags();
        
        if (tags.isEmpty()) {
            logger.warn("No tags found to monitor. Check your configuration.");
            return;
        }
        
        monitoredTags.addAll(tags);
        logger.info("Monitoring {} tags", monitoredTags.size());
        
        // Start polling
        startPolling();
    }
    
    /**
     * Starts the polling task
     */
    private void startPolling() {
        if (pollExecutor != null) {
            logger.warn("Poll executor already running");
            return;
        }
        
        pollExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MQTT-Tag-Poller");
            t.setDaemon(true);
            return t;
        });
        
        long pollRateMs = config.getPollRateMs();
        pollTask = pollExecutor.scheduleAtFixedRate(
            this::pollTags,
            0,  // Initial delay
            pollRateMs,
            TimeUnit.MILLISECONDS
        );
        
        logger.info("Tag polling started with interval: {}ms", pollRateMs);
    }
    
    /**
     * Polls all monitored tags and publishes changes
     */
    private void pollTags() {
        try {
            GatewayTagManager tagManager = gatewayContext.getTagManager();
            
            // Read all tags at once
            CompletableFuture<List<QualifiedValue>> future = tagManager.readAsync(monitoredTags);
            List<QualifiedValue> values = future.get(5, TimeUnit.SECONDS);
            
            statistics.incrementTagReadsSuccessful();
            
            // Process each tag value
            for (int i = 0; i < monitoredTags.size() && i < values.size(); i++) {
                TagPath tagPath = monitoredTags.get(i);
                QualifiedValue newValue = values.get(i);
                
                if (shouldPublish(tagPath, newValue)) {
                    publishTagValue(tagPath, newValue);
                    lastPublishedValues.put(tagPath, newValue);
                }
            }
            
        } catch (TimeoutException e) {
            logger.warn("Timeout reading tags: {}", e.getMessage());
            statistics.incrementTagReadsFailed();
        } catch (InterruptedException e) {
            logger.debug("Tag polling interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error polling tags", e);
            statistics.incrementTagReadsFailed();
        }
    }
    
    /**
     * Discovers tags to monitor based on configuration
     */
    private List<TagPath> discoverTags() {
        List<TagPath> tagPaths = new ArrayList<>();
        
        // Browse entire providers
        for (String providerName : config.getTagProviders()) {
            try {
                List<TagPath> providerTags = browseTagsRecursive(providerName, "");
                tagPaths.addAll(providerTags);
                logger.info("Found {} tags in provider '{}'", providerTags.size(), providerName);
            } catch (Exception e) {
                logger.error("Error browsing provider '{}': {}", providerName, e.getMessage(), e);
            }
        }
        
        // Browse specific folders
        for (String folderPath : config.getTagFolders()) {
            try {
                TagPath parsedPath = TagPathParser.parse(folderPath);
                List<TagPath> folderTags = browseTagsRecursive(
                    parsedPath.getSource(), 
                    parsedPath.toStringPartial()
                );
                tagPaths.addAll(folderTags);
                logger.info("Found {} tags in folder '{}'", folderTags.size(), folderPath);
            } catch (Exception e) {
                logger.error("Error browsing folder '{}': {}", folderPath, e.getMessage(), e);
            }
        }
        
        // Remove duplicates
        return tagPaths.stream()
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Recursively browses tags in a provider/folder
     * 
     * @param providerName The tag provider name
     * @param path The path within the provider
     * @return List of tag paths (leaf tags only, not folders)
     */
    private List<TagPath> browseTagsRecursive(String providerName, String path) {
        List<TagPath> tags = new ArrayList<>();
        GatewayTagManager tagManager = gatewayContext.getTagManager();
        
        try {
            TagPath browsePath = TagPathParser.parse(providerName, path);
            
            // Browse at this level
            Results<NodeDescription> results = tagManager.browseAsync(
                browsePath,
                BrowseFilter.NONE
            ).get(10, TimeUnit.SECONDS);
            
            for (NodeDescription node : results.getResults()) {
                TagPath nodePath = node.getFullPath();
                
                if (node.hasChildren()) {
                    // This is a folder, browse recursively
                    List<TagPath> childTags = browseTagsRecursive(
                        providerName, 
                        nodePath.toStringPartial()
                    );
                    tags.addAll(childTags);
                } else {
                    // This is a leaf tag, add it
                    tags.add(nodePath);
                }
            }
            
        } catch (TimeoutException e) {
            logger.warn("Timeout browsing tags at {}/{}", providerName, path);
        } catch (InterruptedException e) {
            logger.debug("Tag browsing interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error browsing tags at {}/{}: {}", 
                        providerName, path, e.getMessage());
        }
        
        return tags;
    }
    
    /**
     * Determines if a tag value should be published
     */
    private boolean shouldPublish(TagPath tagPath, QualifiedValue newValue) {
        QualifiedValue lastValue = lastPublishedValues.get(tagPath);
        
        // First read - always publish
        if (lastValue == null) {
            return true;
        }
        
        // Check quality change
        if (config.isPublishOnQualityChange()) {
            QualityCode lastQuality = lastValue.getQuality();
            QualityCode newQuality = newValue.getQuality();
            
            if (!Objects.equals(lastQuality, newQuality)) {
                logger.trace("Quality changed for {}: {} -> {}", tagPath, lastQuality, newQuality);
                return true;
            }
        }
        
        // Check value deadband
        double deadband = config.getValueDeadband();
        if (deadband > 0.0) {
            if (!exceedsDeadband(lastValue.getValue(), newValue.getValue(), deadband)) {
                return false;
            }
        } else {
            // No deadband - publish if value changed at all
            if (Objects.equals(lastValue.getValue(), newValue.getValue())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if the value change exceeds the deadband threshold
     */
    private boolean exceedsDeadband(Object oldValue, Object newValue, double deadband) {
        // For numeric values, check if change exceeds deadband
        if (oldValue instanceof Number && newValue instanceof Number) {
            double oldNum = ((Number) oldValue).doubleValue();
            double newNum = ((Number) newValue).doubleValue();
            double delta = Math.abs(newNum - oldNum);
            return delta > deadband;
        }
        
        // For non-numeric, any change exceeds deadband
        return !Objects.equals(oldValue, newValue);
    }
    
    /**
     * Publishes a tag value to MQTT
     */
    private void publishTagValue(TagPath tagPath, QualifiedValue value) {
        try {
            // Map tag to topic
            String topic = topicMapper.mapTagToTopic(tagPath);
            
            // Check for custom topic override
            Map<String, String> topicOverrides = config.getTopicOverrides();
            if (topicOverrides != null && topicOverrides.containsKey(tagPath.toStringFull())) {
                String customTopic = topicOverrides.get(tagPath.toStringFull());
                topic = topicMapper.applyTopicOverride(tagPath, customTopic);
            }
            
            // Build payload
            String payload = payloadBuilder.buildPayload(tagPath, value, config.isIncludeMetadata());
            
            // Publish to MQTT
            publisherManager.publish(topic, payload);
            
            logger.trace("Published {}: {} to {}", tagPath, value.getValue(), topic);
            
        } catch (Exception e) {
            logger.error("Error publishing tag {}: {}", tagPath, e.getMessage());
        }
    }
    
    /**
     * Shuts down the tag subscription manager
     */
    public void shutdown() {
        logger.info("Shutting down tag subscription manager");
        
        if (pollTask != null) {
            pollTask.cancel(false);
        }
        
        if (pollExecutor != null) {
            pollExecutor.shutdown();
            try {
                if (!pollExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    pollExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                pollExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        monitoredTags.clear();
        lastPublishedValues.clear();
        
        logger.info("Tag subscription manager shut down");
    }
    
    /**
     * Gets the number of tags currently being monitored
     */
    public int getMonitoredTagCount() {
        return monitoredTags.size();
    }
}
