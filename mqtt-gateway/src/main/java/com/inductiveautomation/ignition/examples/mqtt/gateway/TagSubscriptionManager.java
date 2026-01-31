package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Manages tag subscriptions using event-driven tag change notifications.
 * 
 * Uses the Ignition 8.3 SDK's TagChangeListener API to receive real-time tag value changes
 * and publish them to MQTT brokers. Supports multi-broker architecture where each topic
 * mapping can be assigned to a specific broker.
 */
public class TagSubscriptionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TagSubscriptionManager.class);
    
    private final GatewayContext gatewayContext;
    private final MultiBrokerManager multiBrokerManager;
    private final MqttTopicMapper topicMapper;
    private final JsonPayloadBuilder payloadBuilder;
    private final ModuleStatistics statistics;

    private volatile ScheduledExecutorService batchScheduler;
    private final Map<String, BatchAccumulator> batchAccumulators = new ConcurrentHashMap<>();
    
    private TagPublishConfig config;
    private final Map<TagPath, QualifiedValue> lastPublishedValues = new ConcurrentHashMap<>();
    private final Map<TagPath, Map<String, Object>> tagPropertyCache = new ConcurrentHashMap<>();
    private final List<TagPath> monitoredTags = Collections.synchronizedList(new ArrayList<>());
    
    // Tag change listener for event-driven subscriptions
    private final MqttTagChangeListener tagChangeListener = new MqttTagChangeListener();
    
    // Track subscription futures for cleanup
    private final List<CompletableFuture<Void>> subscriptionFutures = new ArrayList<>();
    
    /**
     * Creates a new tag subscription manager
     * 
     * @param context Gateway context
     * @param multiBrokerManager Multi-broker manager
     * @param statistics Module statistics tracker
     */
    public TagSubscriptionManager(GatewayContext context, MultiBrokerManager multiBrokerManager, ModuleStatistics statistics) {
        this.gatewayContext = context;
        this.multiBrokerManager = multiBrokerManager;
        this.statistics = statistics;
        this.topicMapper = new MqttTopicMapper();
        this.payloadBuilder = new JsonPayloadBuilder();
        this.batchScheduler = createBatchScheduler();
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

        ensureBatchScheduler();
        
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

        loadTagProperties(tags, buildCombinedPayloadFields(config));
        
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

    private void loadTagProperties(List<TagPath> tags, com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig fields) {
        tagPropertyCache.clear();
        if (tags == null || tags.isEmpty()) {
            return;
        }
        if (fields == null || fields.getProperties() == null || fields.getProperties().isEmpty()) {
            return;
        }
        logger.info("Loading tag properties for {} tags", tags.size());

        Map<String, List<TagPath>> tagsByProvider = tags.stream()
            .collect(Collectors.groupingBy(TagPath::getSource));

        for (Map.Entry<String, List<TagPath>> entry : tagsByProvider.entrySet()) {
            String providerName = entry.getKey();
            List<TagPath> providerTags = entry.getValue();
            TagProvider provider = gatewayContext.getTagManager().getTagProvider(providerName);
            if (provider == null) {
                logger.warn("Tag provider not found: {}", providerName);
                continue;
            }
            try {
                CompletableFuture<List<TagConfigurationModel>> future = provider.getTagConfigsAsync(providerTags, false, true);
                List<TagConfigurationModel> configs = future.get(10, TimeUnit.SECONDS);
                for (TagConfigurationModel configModel : configs) {
                    TagPath tagPath = configModel.getPath();
                    Map<String, Object> properties = readTagProperties(configModel, fields);
                    if (properties != null && !properties.isEmpty()) {
                        tagPropertyCache.put(tagPath, properties);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to load tag properties for provider {}: {}", providerName, e.getMessage());
            }
        }
    }

    private Map<String, Object> readTagProperties(TagConfigurationModel configModel, com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig fields) {
        if (fields == null || fields.getProperties() == null || fields.getProperties().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            java.util.Set<com.inductiveautomation.ignition.common.config.Property<?>> tagProps = TagPropertyResolver.getSelectedTagProps(fields);
            if (tagProps.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, Boolean> entry : fields.getProperties().entrySet()) {
                if (!Boolean.TRUE.equals(entry.getValue())) {
                    continue;
                }
                com.inductiveautomation.ignition.common.config.Property<?> prop = TagPropertyResolver.getPropertyMap().get(entry.getKey());
                if (prop == null) {
                    continue;
                }
                Object propValue = configModel.getTagProperties().get(prop);
                result.put(entry.getKey(), propValue);
            }
            return result;
        } catch (Exception e) {
            logger.warn("Failed to read properties for tag {}: {}", configModel.getPath(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    private com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig buildCombinedPayloadFields(
        TagPublishConfig config
    ) {
        com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig combined =
            config.getPayloadFieldsOrDefault().copy();
        if (config.getTopicMappings() == null) {
            return combined;
        }
        for (com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping : config.getTopicMappings()) {
            if (mapping == null || mapping.isUseDefaultPayloadFields()) {
                continue;
            }
            com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig fields = mapping.getPayloadFields();
            if (fields == null || fields.getProperties() == null) {
                continue;
            }
            for (Map.Entry<String, Boolean> entry : fields.getProperties().entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    combined.getProperties().put(entry.getKey(), true);
                }
            }
        }
        return combined;
    }

    private void enqueueBatchMetric(
        com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping,
        Long brokerId,
        String topic,
        TagPath tagPath,
        QualifiedValue value,
        Map<String, Object> properties,
        com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig payloadFields
    ) {
        int batchWindowMs = mapping.getBatchWindowMs();
        if (batchWindowMs <= 0) {
            List<JsonPayloadBuilder.MetricPayload> metrics = Collections.singletonList(
                new JsonPayloadBuilder.MetricPayload(tagPath, value, properties)
            );
            String payload = payloadBuilder.buildBatchPayload(metrics, payloadFields);
            boolean published = multiBrokerManager.publish(brokerId, topic, payload);
            if (published) {
                statistics.incrementBatchPublished(1);
                logger.info("Published batch payload (1 metric) to {} on broker {}", topic, brokerId);
            } else {
                logger.warn("Failed to publish batch payload to broker {}", brokerId);
            }
            return;
        }

        BatchAccumulator accumulator = getBatchAccumulator(mapping, brokerId, topic);
        accumulator.setPayloadFields(payloadFields);
        int size = accumulator.addMetric(tagPath, value, properties);
        int maxBatchSize = Math.max(1, mapping.getMaxBatchSize());
        if (size >= maxBatchSize) {
            accumulator.flush();
            return;
        }
        accumulator.scheduleFlush(batchWindowMs);
    }

    private BatchAccumulator getBatchAccumulator(
        com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping,
        Long brokerId,
        String topic
    ) {
        String mappingId = mapping.getId();
        String key = mappingId != null ? mappingId : mapping.getSourcePattern() + "::" + mapping.getTopicPrefix();
        return batchAccumulators.computeIfAbsent(
            key,
            ignored -> new BatchAccumulator(mapping, brokerId, topic)
        );
    }

    private class BatchAccumulator {
        private final com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping;
        private final Long brokerId;
        private final String topic;
        private final Map<TagPath, JsonPayloadBuilder.MetricPayload> metrics = new ConcurrentHashMap<>();
        private final AtomicBoolean flushScheduled = new AtomicBoolean(false);
        private volatile ScheduledFuture<?> scheduledFlush;
        private volatile com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig payloadFields;

        private BatchAccumulator(
            com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping,
            Long brokerId,
            String topic
        ) {
            this.mapping = mapping;
            this.brokerId = brokerId;
            this.topic = topic;
        }

        private void setPayloadFields(com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig payloadFields) {
            this.payloadFields = payloadFields;
        }

        private int addMetric(TagPath tagPath, QualifiedValue value, Map<String, Object> properties) {
            metrics.put(tagPath, new JsonPayloadBuilder.MetricPayload(tagPath, value, properties));
            return metrics.size();
        }

        private void scheduleFlush(int batchWindowMs) {
            if (!flushScheduled.compareAndSet(false, true)) {
                return;
            }
            try {
                ScheduledExecutorService scheduler = ensureBatchScheduler();
                scheduledFlush = scheduler.schedule(() -> {
                    try {
                        flush();
                    } catch (Exception e) {
                        logger.warn("Batch flush failed for mapping {}: {}", mapping.getSourcePattern(), e.getMessage());
                    }
                }, batchWindowMs, TimeUnit.MILLISECONDS);
            } catch (RuntimeException e) {
                flushScheduled.set(false);
                logger.warn("Failed to schedule batch flush for mapping {}: {}", mapping.getSourcePattern(), e.getMessage());
            }
        }

        private void flush() {
            List<JsonPayloadBuilder.MetricPayload> batch;
            synchronized (this) {
                if (metrics.isEmpty()) {
                    flushScheduled.set(false);
                    return;
                }
                batch = new ArrayList<>(metrics.values());
                metrics.clear();
                flushScheduled.set(false);
                scheduledFlush = null;
            }

            com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig fields =
                payloadFields != null ? payloadFields : config.getPayloadFieldsForMapping(mapping);
            try {
                String payload = payloadBuilder.buildBatchPayload(batch, fields);
                boolean published = multiBrokerManager.publish(brokerId, topic, payload);
                if (published) {
                    statistics.incrementBatchPublished(batch.size());
                    logger.info("Published batch payload ({} metrics) to {} on broker {}", batch.size(), topic, brokerId);
                } else {
                    logger.warn("Failed to publish batch payload to broker {}", brokerId);
                }
            } catch (Exception e) {
                logger.warn("Failed to publish batch payload for mapping {}: {}", mapping.getSourcePattern(), e.getMessage());
            }
        }
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
            
            // Find matching enabled topic mapping with longest source pattern (most specific)
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
                logger.debug("Skipping tag {} - no enabled mapping matches (fullPath: {})", tagPath, fullPath);
                return;
            }
            
            // Get the broker ID from the mapping
            Long brokerId = matchedMapping.getBrokerId();
            if (brokerId == null) {
                logger.warn("Skipping tag {} - mapping has no broker assigned", tagPath);
                return;
            }
            
            logger.info("Matched tag {} to mapping: source={}, topicPrefix={}, brokerId={}", 
                fullPath, matchedMapping.getSourcePattern(), matchedMapping.getTopicPrefix(), brokerId);
            
            // Map tag to topic using the matched mapping (not searching again)
            String topic = topicMapper.mapTagToTopicWithMapping(tagPath, matchedMapping);

            // Build payload
            Map<String, Object> properties = tagPropertyCache.get(tagPath);
            com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig payloadFields =
                config.getPayloadFieldsForMapping(matchedMapping);

            if (matchedMapping.getPublishMode() == com.inductiveautomation.ignition.examples.mqtt.common.model.TopicPublishMode.SINGLE_TOPIC) {
                enqueueBatchMetric(matchedMapping, brokerId, topic, tagPath, value, properties, payloadFields);
                return;
            }

            String payload = payloadBuilder.buildPayload(tagPath, value, payloadFields, properties);

            logger.info("Publishing to broker {}: topic={}, payload={}", brokerId, topic, payload);

            // Publish to the SPECIFIC broker assigned to this mapping
            boolean published = multiBrokerManager.publish(brokerId, topic, payload);

            if (published) {
                logger.info("Published {}: {} to {} on broker {}", 
                    tagPath, value.getValue(), topic, brokerId);
            } else {
                logger.warn("Failed to publish {} to broker {}", tagPath, brokerId);
            }
            
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

        try {
            batchScheduler.shutdownNow();
        } catch (Exception e) {
            logger.debug("Failed to shutdown batch scheduler", e);
        }
        batchScheduler = null;
        batchAccumulators.clear();
        
        monitoredTags.clear();
        lastPublishedValues.clear();
        tagPropertyCache.clear();
        
        logger.info("Tag subscription manager shut down");
    }

    private ScheduledExecutorService createBatchScheduler() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "mqtt-uns-batch-publisher");
            thread.setDaemon(true);
            return thread;
        });
    }

    private synchronized ScheduledExecutorService ensureBatchScheduler() {
        if (batchScheduler == null || batchScheduler.isShutdown() || batchScheduler.isTerminated()) {
            batchScheduler = createBatchScheduler();
        }
        return batchScheduler;
    }
    
    /**
     * Gets the number of tags currently being monitored
     */
    public int getMonitoredTagCount() {
        return monitoredTags.size();
    }
}
