package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.config.types.TagObjectType;
import com.inductiveautomation.ignition.common.tags.config.properties.WellKnownTagProps;
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

            Collection<NodeDescription> nodes = results != null ? results.getResults() : null;
            if (nodes == null) {
                logger.warn(
                    "Browse returned no results for {}/{} - attempting config fallback",
                    providerName,
                    path
                );
                return browseTagsFromConfig(providerName, browsePath);
            }

            for (NodeDescription node : nodes) {
                TagPath nodePath = node.getFullPath();
                if (nodePath == null) {
                    logger.warn(
                        "Browse returned node with null path under {}/{} (name: {})",
                        providerName,
                        path,
                        node.getName()
                    );
                    continue;
                }

                TagObjectType objectType = node.getObjectType();
                boolean isUdtInstance = objectType == TagObjectType.UdtInstance;
                boolean shouldBrowseChildren = node.hasChildren() || isUdtInstance;

                if (shouldBrowseChildren) {
                    // Browse into folders and UDT instances to discover member tags
                    List<TagPath> childTags = browseTagsRecursive(
                        providerName,
                        nodePath.toStringPartial()
                    );
                    tags.addAll(childTags);
                    continue;
                }

                // Only include leaf nodes that represent actual tags
                if (objectType == null || objectType == TagObjectType.AtomicTag || objectType == TagObjectType.Node) {
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

    private List<TagPath> browseTagsFromConfig(String providerName, TagPath browsePath) {
        List<TagPath> tags = new ArrayList<>();
        TagProvider provider = gatewayContext.getTagManager().getTagProvider(providerName);
        if (provider == null) {
            logger.warn("Tag provider not found for config fallback: {}", providerName);
            return tags;
        }
        try {
            CompletableFuture<List<TagConfigurationModel>> future =
                provider.getTagConfigsAsync(Collections.singletonList(browsePath), true, false);
            List<TagConfigurationModel> configs = future.get(10, TimeUnit.SECONDS);
            if (configs == null) {
                return tags;
            }
            for (TagConfigurationModel configModel : configs) {
                Object tagType = configModel.getTagProperties().get(WellKnownTagProps.TagType);
                if (tagType == TagObjectType.AtomicTag) {
                    tags.add(configModel.getPath());
                }
            }
        } catch (TimeoutException e) {
            logger.warn("Timeout reading tag configs at {}/{}", providerName, browsePath.toStringPartial());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("Error reading tag configs at {}/{}: {}", providerName, browsePath.toStringPartial(), e.getMessage());
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
                CompletableFuture<List<TagConfigurationModel>> future = provider.getTagConfigsAsync(providerTags, false, false);
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

        // Fill in missing properties by reading tag property paths (captures inherited UDT defaults)
        loadMissingPropertyValues(tags, fields);
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
            Set<String> requestedKeys = new HashSet<>();
            for (Map.Entry<String, Boolean> entry : fields.getProperties().entrySet()) {
                if (!Boolean.TRUE.equals(entry.getValue())) {
                    continue;
                }
                requestedKeys.add(entry.getKey());
                com.inductiveautomation.ignition.common.config.Property<?> prop = TagPropertyResolver.getPropertyMap().get(entry.getKey());
                if (prop == null) {
                    continue;
                }
                Object propValue = getTagPropertyValue(configModel, prop, entry.getKey());
                if (propValue == null) {
                    propValue = resolveFallbackProperty(entry.getKey(), configModel);
                }
                if (propValue != null) {
                    result.put(entry.getKey(), propValue);
                }
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

    private Object resolveFallbackProperty(String key, TagConfigurationModel configModel) {
        if (configModel == null || key == null) {
            return null;
        }
        switch (key) {
            case "name":
                return deriveTagName(configModel.getPath());
            case "tagGroup":
                return configModel.getTagProperties().get(WellKnownTagProps.TagGroup);
            case "dataType":
                return readTagPropByName(configModel, "DataType");
            default:
                return readTagPropByKey(configModel, key);
        }
    }

    private Object getTagPropertyValue(
        TagConfigurationModel configModel,
        com.inductiveautomation.ignition.common.config.Property<?> prop,
        String key
    ) {
        if (configModel == null || prop == null) {
            return null;
        }
        try {
            Object value = readPropertyByName(configModel.getTagProperties(), prop, key);
            if (value != null) {
                return value;
            }
            com.inductiveautomation.ignition.common.config.BoundPropertySet inherited =
                configModel.getInheritedConfiguration();
            if (inherited != null) {
                value = readPropertyByName(inherited, prop, key);
                if (value != null) {
                    return value;
                }
            }
            com.inductiveautomation.ignition.common.tags.config.TagConfiguration local =
                configModel.getLocalConfiguration();
            if (local != null) {
                com.inductiveautomation.ignition.common.config.BoundPropertySet localProps = local.getTagProperties();
                if (localProps != null) {
                    return readPropertyByName(localProps, prop, key);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Object readPropertyByName(
        com.inductiveautomation.ignition.common.config.BoundPropertySet props,
        com.inductiveautomation.ignition.common.config.Property<?> prop,
        String key
    ) {
        if (props == null || prop == null) {
            return null;
        }
        Object value = props.get(prop);
        if (value != null) {
            return resolveBoundValue(props, value);
        }
        String propName = prop.getName();
        String keyName = key != null ? key : null;
        String target = propName != null ? propName.toLowerCase() : null;
        String keyTarget = keyName != null ? keyName.toLowerCase() : null;
        for (com.inductiveautomation.ignition.common.config.Property<?> candidate : props.getProperties()) {
            if (candidate == null) {
                continue;
            }
            String candidateName = candidate.getName();
            if (candidateName == null) {
                continue;
            }
            String candidateLower = candidateName.toLowerCase();
            if ((target != null && candidateLower.equals(target)) ||
                (keyTarget != null && candidateLower.equals(keyTarget))) {
                Object candidateValue = props.get(candidate);
                if (candidateValue != null) {
                    return resolveBoundValue(props, candidateValue);
                }
            }
        }
        return null;
    }

    private Object resolveBoundValue(
        com.inductiveautomation.ignition.common.config.BoundPropertySet props,
        Object value
    ) {
        if (!(value instanceof com.inductiveautomation.ignition.common.config.BoundValue)) {
            return value;
        }
        com.inductiveautomation.ignition.common.config.BoundValue bound =
            (com.inductiveautomation.ignition.common.config.BoundValue) value;
        if (bound.getBindType() == null || !"parameter".equalsIgnoreCase(bound.getBindType())) {
            return bound.toString();
        }
        String binding = bound.getBinding();
        if (binding == null) {
            return bound.toString();
        }
        com.inductiveautomation.ignition.common.config.BoundPropertySet parameters =
            props.get(WellKnownTagProps.Parameters);
        if (parameters == null) {
            return bound.toString();
        }
        for (com.inductiveautomation.ignition.common.config.Property<?> paramProp : parameters.getProperties()) {
            if (paramProp == null || paramProp.getName() == null) {
                continue;
            }
            if (binding.equalsIgnoreCase(paramProp.getName())) {
                Object paramValue = parameters.get(paramProp);
                return paramValue != null ? paramValue : bound.toString();
            }
        }
        return bound.toString();
    }

    private Object readTagPropByKey(TagConfigurationModel configModel, String key) {
        String propName = mapKeyToTagPropName(key);
        if (propName == null) {
            return null;
        }
        return readTagPropByName(configModel, propName);
    }

    private String mapKeyToTagPropName(String key) {
        if (key == null) {
            return null;
        }
        switch (key) {
            case "opcServer":
                return "OPCServer";
            case "opcItemPath":
                return "OPCItemPath";
            case "sourceTagPath":
                return "SourceTagPath";
            case "deadband":
                return "Deadband";
            case "deadbandMode":
                return "DeadbandMode";
            case "scaleMode":
                return "ScaleMode";
            case "rawLow":
                return "RawLow";
            case "rawHigh":
                return "RawHigh";
            case "scaledLow":
                return "ScaledLow";
            case "scaledHigh":
                return "ScaledHigh";
            case "clampMode":
                return "ClampMode";
            case "scaleFactor":
                return "ScaleFactor";
            case "engUnit":
                return "EngUnit";
            case "engLow":
                return "EngLow";
            case "engHigh":
                return "EngHigh";
            case "engLimitMode":
                return "EngLimitMode";
            case "formatString":
                return "FormatString";
            case "valueSource":
                return "ValueSource";
            case "executionMode":
                return "ExecutionMode";
            case "queryType":
                return "QueryType";
            case "datasource":
                return "SQLBindingDatasource";
            default:
                return null;
        }
    }

    private Object readTagPropByName(TagConfigurationModel configModel, String propName) {
        if (configModel == null || propName == null) {
            return null;
        }
        try {
            java.lang.reflect.Field field = com.inductiveautomation.ignition.common.sqltags.model.TagProp.class.getField(propName);
            Object prop = field.get(null);
            if (prop instanceof com.inductiveautomation.ignition.common.config.Property<?>) {
                return configModel.getTagProperties().get((com.inductiveautomation.ignition.common.config.Property<?>) prop);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void loadMissingPropertyValues(
        List<TagPath> tags,
        com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig fields
    ) {
        if (tags == null || tags.isEmpty() || fields == null || fields.getProperties() == null) {
            return;
        }
        Map<TagPath, Map<String, Object>> missingByTag = new HashMap<>();
        for (TagPath tagPath : tags) {
            if (tagPath == null) {
                continue;
            }
            Map<String, Object> existing = tagPropertyCache.get(tagPath);
            for (Map.Entry<String, Boolean> entry : fields.getProperties().entrySet()) {
                if (!Boolean.TRUE.equals(entry.getValue())) {
                    continue;
                }
                String key = entry.getKey();
                if (key == null || key.isEmpty()) {
                    continue;
                }
                if (existing != null && existing.containsKey(key)) {
                    continue;
                }
                String propName = mapKeyToTagPropName(key);
                if (propName == null) {
                    continue;
                }
                missingByTag
                    .computeIfAbsent(tagPath, ignored -> new HashMap<>())
                    .put(key, propName);
            }
        }

        if (missingByTag.isEmpty()) {
            return;
        }

        List<TagPath> propertyPaths = new ArrayList<>();
        List<PropertyLookup> lookups = new ArrayList<>();
        for (Map.Entry<TagPath, Map<String, Object>> entry : missingByTag.entrySet()) {
            TagPath basePath = entry.getKey();
            String baseFull = basePath.toStringFull();
            for (Map.Entry<String, Object> propEntry : entry.getValue().entrySet()) {
                String key = propEntry.getKey();
                String propName = (String) propEntry.getValue();
                try {
                    TagPath propertyPath = TagPathParser.parse(baseFull + "." + propName);
                    propertyPaths.add(propertyPath);
                    lookups.add(new PropertyLookup(basePath, key));
                } catch (Exception e) {
                    logger.debug("Failed to parse property path for {}.{}: {}", baseFull, propName, e.getMessage());
                }
            }
        }

        if (propertyPaths.isEmpty()) {
            return;
        }

        try {
            GatewayTagManager tagManager = gatewayContext.getTagManager();
            List<QualifiedValue> values = tagManager.readAsync(propertyPaths).get(10, TimeUnit.SECONDS);
            if (values == null) {
                return;
            }
            for (int i = 0; i < values.size() && i < lookups.size(); i++) {
                QualifiedValue value = values.get(i);
                PropertyLookup lookup = lookups.get(i);
                if (lookup == null || lookup.tagPath == null || lookup.key == null) {
                    continue;
                }
                Object propValue = value != null ? value.getValue() : null;
                if (propValue == null) {
                    continue;
                }
                tagPropertyCache
                    .computeIfAbsent(lookup.tagPath, ignored -> new HashMap<>())
                    .putIfAbsent(lookup.key, propValue);
            }
        } catch (TimeoutException e) {
            logger.warn("Timeout reading tag property paths for missing values");
        } catch (Exception e) {
            logger.warn("Failed to read tag property paths: {}", e.getMessage());
        }
    }

    private static final class PropertyLookup {
        private final TagPath tagPath;
        private final String key;

        private PropertyLookup(TagPath tagPath, String key) {
            this.tagPath = tagPath;
            this.key = key;
        }
    }

    private Map<String, Object> applyPropertyFallbacks(
        TagPath tagPath,
        QualifiedValue value,
        Map<String, Object> properties,
        com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig payloadFields
    ) {
        if (payloadFields == null || payloadFields.getProperties() == null || payloadFields.getProperties().isEmpty()) {
            return properties;
        }
        Map<String, Object> enriched = properties != null ? new HashMap<>(properties) : new HashMap<>();
        if (shouldIncludeProperty(payloadFields, "name") && !enriched.containsKey("name")) {
            String name = deriveTagName(tagPath);
            if (name != null) {
                enriched.put("name", name);
            }
        }
        if (shouldIncludeProperty(payloadFields, "dataType") && !enriched.containsKey("dataType")) {
            String dataType = deriveDataTypeName(value != null ? value.getValue() : null);
            if (dataType != null) {
                enriched.put("dataType", dataType);
            }
        }
        return enriched.isEmpty() ? properties : enriched;
    }

    private boolean shouldIncludeProperty(
        com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig payloadFields,
        String key
    ) {
        if (payloadFields == null || payloadFields.getProperties() == null || key == null) {
            return false;
        }
        return Boolean.TRUE.equals(payloadFields.getProperties().get(key));
    }

    private String deriveTagName(TagPath tagPath) {
        if (tagPath == null) {
            return null;
        }
        String partial = tagPath.toStringPartial();
        if (partial == null || partial.isEmpty()) {
            return tagPath.toStringFull();
        }
        int lastSlash = partial.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < partial.length() - 1) {
            return partial.substring(lastSlash + 1);
        }
        return partial;
    }

    private String deriveDataTypeName(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return "Int4";
        }
        if (value instanceof Long) {
            return "Int8";
        }
        if (value instanceof Float) {
            return "Float4";
        }
        if (value instanceof Double) {
            return "Float8";
        }
        if (value instanceof Boolean) {
            return "Boolean";
        }
        if (value instanceof String) {
            return "String";
        }
        if (value instanceof java.util.Date) {
            return "DateTime";
        }
        return value.getClass().getSimpleName();
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
        Map<String, Object> enrichedProperties = applyPropertyFallbacks(tagPath, value, properties, payloadFields);
        int batchWindowMs = mapping.getBatchWindowMs();
        if (batchWindowMs <= 0) {
            List<JsonPayloadBuilder.MetricPayload> metrics = Collections.singletonList(
                new JsonPayloadBuilder.MetricPayload(tagPath, value, enrichedProperties)
            );
            String payload = payloadBuilder.buildBatchPayload(metrics, payloadFields);
            boolean published = multiBrokerManager.publish(brokerId, topic, payload);
            if (published) {
                statistics.incrementBatchPublished(1);
            } else {
                logger.warn("Failed to publish batch payload to broker {}", brokerId);
            }
            return;
        }

        BatchAccumulator accumulator = getBatchAccumulator(mapping, brokerId, topic);
        accumulator.setPayloadFields(payloadFields);
        int size = accumulator.addMetric(tagPath, value, enrichedProperties);
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
            com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping matchedMapping =
                topicMapper.findBestMapping(fullPath);
            
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
            
            // Per-tag match logging is intentionally omitted to reduce gateway log volume.
            
            // Map tag to topic using the matched mapping (not searching again)
            String topic = topicMapper.mapTagToTopicWithMapping(tagPath, matchedMapping);

            // Build payload
            Map<String, Object> properties = tagPropertyCache.get(tagPath);
            com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig payloadFields =
                config.getPayloadFieldsForMapping(matchedMapping);
            Map<String, Object> enrichedProperties = applyPropertyFallbacks(tagPath, value, properties, payloadFields);

            if (matchedMapping.getPublishMode() == com.inductiveautomation.ignition.examples.mqtt.common.model.TopicPublishMode.SINGLE_TOPIC) {
                enqueueBatchMetric(matchedMapping, brokerId, topic, tagPath, value, enrichedProperties, payloadFields);
                return;
            }

            String payload = payloadBuilder.buildPayload(tagPath, value, payloadFields, enrichedProperties);

            // Publish to the SPECIFIC broker assigned to this mapping
            boolean published = multiBrokerManager.publish(brokerId, topic, payload);

            if (!published) {
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
