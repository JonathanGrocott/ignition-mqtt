package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.event.TagChangeEvent;
import com.inductiveautomation.ignition.common.tags.model.event.TagChangeListener;
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
 * Manages tag subscriptions using event-driven tag change notifications.
 * 
 * Uses the Ignition 8.3 SDK's TagChangeListener API to receive real-time tag value changes
 * and publish them to MQTT. This is more efficient and responsive than polling.
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
    
    // Tag change listener for event-driven subscriptions
    private final MqttTagChangeListener tagChangeListener = new MqttTagChangeListener();
    
    // Track subscription futures for cleanup
    private final List<CompletableFuture<Void>> subscriptionFutures = new ArrayList<>();
    
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
        
        logger.info("Starting event-driven tag subscription manager");
        
        // Set topic mappings on the mapper
        if (config.getTopicMappings() != null && !config.getTopicMappings().isEmpty()) {
            topicMapper.setTopicMappings(config.getTopicMappings());
            logger.info("Loaded {} topic mappings", config.getTopicMappings().size());
        }
        
        // Discover tags to monitor
        List<TagPath> tags = discoverTags();
        
        if (tags.isEmpty()) {
            logger.warn("No tags found to monitor. Check your configuration.");
            return;
        }
        
        monitoredTags.addAll(tags);
        logger.info("Discovered {} tags to monitor", monitoredTags.size());
        
        // Subscribe to tag changes
        subscribeToTags();
    }
    
    /**
     * Subscribes to tag changes using the event-driven API
     */
    private void subscribeToTags() {
        if (monitoredTags.isEmpty()) {
            logger.warn("No tags to subscribe to");
            return;
        }
        
        try {
            GatewayTagManager tagManager = gatewayContext.getTagManager();
            
            // Create a list of listeners (one per tag)
            List<TagChangeListener> listeners = new ArrayList<>();
            for (int i = 0; i < monitoredTags.size(); i++) {
                listeners.add(tagChangeListener);
            }
            
            // Subscribe to all tags at once
            logger.info("Subscribing to {} tags using event-driven API", monitoredTags.size());
            CompletableFuture<Void> subscriptionFuture = tagManager.subscribeAsync(monitoredTags, listeners);
            subscriptionFutures.add(subscriptionFuture);
            
            // Wait for subscription to complete
            subscriptionFuture.get(30, TimeUnit.SECONDS);
            
            logger.info("Successfully subscribed to {} tags", monitoredTags.size());
            
        } catch (TimeoutException e) {
            logger.error("Timeout subscribing to tags: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.warn("Tag subscription interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Failed to subscribe to tags", e);
        }
    }
    
    /**
     * Tag change listener that handles tag value change events
     */
    private class MqttTagChangeListener implements TagChangeListener {
        
        @Override
        public void tagChanged(TagChangeEvent event) {
            try {
                TagPath tagPath = event.getTagPath();
                QualifiedValue newValue = event.getValue();
                
                // Skip initial value callback if we've already published this tag
                // (initial callbacks are sent when subscription is first created)
                if (event.isInitial() && lastPublishedValues.containsKey(tagPath)) {
                    logger.trace("Skipping initial value for already-published tag: {}", tagPath);
                    return;
                }
                
                // Increment tag read statistics
                statistics.incrementTagReadsSuccessful();
                
                // Check if we should publish this change
                if (shouldPublish(tagPath, newValue)) {
                    publishTagValue(tagPath, newValue);
                    lastPublishedValues.put(tagPath, newValue);
                }
                
            } catch (Exception e) {
                logger.error("Error handling tag change event: {}", e.getMessage(), e);
                statistics.incrementTagReadsFailed();
            }
        }
        
        @Override
        public boolean isLightweight() {
            // Return true to avoid leasing tags
            // This is more efficient for read-only subscriptions
            return true;
        }
    }
    
    /**
     * Discovers tags to monitor based on topic mappings configuration
     */
    private List<TagPath> discoverTags() {
        List<TagPath> tagPaths = new ArrayList<>();
        
        // Browse tags based on enabled topic mappings
        if (config.getTopicMappings() != null) {
            for (com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping : config.getTopicMappings()) {
                if (!mapping.isEnabled()) {
                    logger.debug("Skipping disabled mapping: {}", mapping.getSourcePattern());
                    continue;
                }
                
                try {
                    // Parse the source pattern to extract provider and path
                    String sourcePattern = mapping.getSourcePattern();
                    TagPath parsedPath = TagPathParser.parse(sourcePattern);
                    
                    List<TagPath> mappingTags = browseTagsRecursive(
                        parsedPath.getSource(), 
                        parsedPath.toStringPartial()
                    );
                    tagPaths.addAll(mappingTags);
                    logger.info("Found {} tags for mapping '{}' -> '{}'", 
                        mappingTags.size(), 
                        mapping.getSourcePattern(), 
                        mapping.getTopicPrefix());
                } catch (Exception e) {
                    logger.error("Error browsing tags for mapping '{}': {}", 
                        mapping.getSourcePattern(), e.getMessage(), e);
                }
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
            String fullPath = tagPath.toStringFull();
            
            // Check if tag matches any enabled topic mapping
            com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping matchedMapping = null;
            if (config.getTopicMappings() != null) {
                matchedMapping = config.getTopicMappings().stream()
                    .filter(com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping::isEnabled)
                    .filter(mapping -> mapping.matches(fullPath))
                    .max((m1, m2) -> Integer.compare(
                        m1.getSourcePattern().length(), 
                        m2.getSourcePattern().length()
                    ))
                    .orElse(null);
            }
            
            // Only publish if tag matches an enabled mapping
            if (matchedMapping == null) {
                logger.trace("Skipping tag {} - no enabled mapping matches", tagPath);
                return;
            }
            
            // Map tag to topic (with custom mappings applied)
            String topic = topicMapper.mapTagToTopicWithMappings(tagPath);
            
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
        
        // Unsubscribe from all tags
        if (!monitoredTags.isEmpty()) {
            try {
                GatewayTagManager tagManager = gatewayContext.getTagManager();
                
                // Create list of listeners for unsubscribe (same listener for all tags)
                List<TagChangeListener> listeners = new ArrayList<>();
                for (int i = 0; i < monitoredTags.size(); i++) {
                    listeners.add(tagChangeListener);
                }
                
                logger.info("Unsubscribing from {} tags", monitoredTags.size());
                CompletableFuture<Void> unsubscribeFuture = tagManager.unsubscribeAsync(monitoredTags, listeners);
                
                // Wait for unsubscribe to complete
                unsubscribeFuture.get(10, TimeUnit.SECONDS);
                logger.info("Successfully unsubscribed from all tags");
                
            } catch (TimeoutException e) {
                logger.warn("Timeout unsubscribing from tags: {}", e.getMessage());
            } catch (InterruptedException e) {
                logger.warn("Tag unsubscribe interrupted");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error unsubscribing from tags", e);
            }
        }
        
        // Cancel any pending subscription futures
        for (CompletableFuture<Void> future : subscriptionFutures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        subscriptionFutures.clear();
        
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
