package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway;

import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.browsing.Results;
import com.inductiveautomation.ignition.common.tags.model.event.TagChangeEvent;
import com.inductiveautomation.ignition.common.tags.model.event.TagChangeListener;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.config.types.TagObjectType;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugDeviceMapping;
import com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugPublishConfig;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.MetricDataType;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class SparkplugTagSubscriptionManager {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugTagSubscriptionManager.class);
    private static final String NODE_KEY = "__node__";

    private final GatewayContext gatewayContext;
    private final SparkplugPublisherManager publisherManager;
    private final SparkplugPublishConfig publishConfig;
    private final MqttBrokerConfig brokerConfig;
    private final SparkplugPayloadBuilder payloadBuilder = new SparkplugPayloadBuilder();
    private final SparkplugBdSeqManager bdSeqManager = new SparkplugBdSeqManager();
    private final SparkplugTagChangeListener tagChangeListener = new SparkplugTagChangeListener();

    private final List<TagPath> monitoredTags = new ArrayList<>();
    private final java.util.Map<TagPath, String> tagDeviceMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<TagPath, String> tagMetricMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<TagPath, EdgeKey> tagEdgeMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<EdgeKey, java.util.Map<String, List<TagPath>>> deviceTagsMap =
        new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<EdgeKey, SparkplugSequence> sequenceMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<EdgeKey, Long> bdSeqMap = new java.util.concurrent.ConcurrentHashMap<>();

    public SparkplugTagSubscriptionManager(
        GatewayContext gatewayContext,
        SparkplugPublisherManager publisherManager,
        SparkplugPublishConfig publishConfig,
        MqttBrokerConfig brokerConfig
    ) {
        this.gatewayContext = gatewayContext;
        this.publisherManager = publisherManager;
        this.publishConfig = publishConfig;
        this.brokerConfig = brokerConfig;
    }

    public void start() {
        if (!publishConfig.isEnabled()) {
            return;
        }

        monitoredTags.clear();
        tagDeviceMap.clear();
        tagMetricMap.clear();
        tagEdgeMap.clear();
        deviceTagsMap.clear();
        sequenceMap.clear();
        bdSeqMap.clear();

        List<TagPath> tags = browseTags();
        if (tags.isEmpty()) {
            logger.warn("No Sparkplug tags found for config {}", publishConfig.getName());
            return;
        }

        monitoredTags.addAll(tags);
        subscribeToTags();

        EdgeKey primaryEdge = getPrimaryEdgeKey();
        if (primaryEdge == null) {
            logger.warn("Sparkplug publish config has no group/edge mapping: {}", publishConfig.getName());
            return;
        }

        long currentBdSeq = bdSeqMap.computeIfAbsent(primaryEdge, ignored -> bdSeqManager.next());
        SparkplugSequence sequence = sequenceMap.computeIfAbsent(primaryEdge, ignored -> new SparkplugSequence());

        byte[] deathPayload = payloadBuilder.buildPayload(sequence.next(),
            Collections.singletonList(buildBdSeqMetric(currentBdSeq))
        );

        publisherManager.setCommandListener(this::handleCommand);
        publisherManager.connect(
            brokerConfig,
            SparkplugTopicBuilder.nodeDeath(primaryEdge.groupId, primaryEdge.edgeNodeId),
            deathPayload,
            brokerConfig.getQos(),
            true
        );

        if (sequenceMap.size() > 1) {
            logger.warn("Multiple Sparkplug edge nodes detected; using single MQTT client for LWT and commands");
        }

        for (EdgeKey edgeKey : sequenceMap.keySet()) {
            publisherManager.subscribe(
                SparkplugTopicBuilder.nodeCommand(edgeKey.groupId, edgeKey.edgeNodeId),
                brokerConfig.getQos()
            );
            publisherManager.subscribe(
                SparkplugTopicBuilder.deviceCommandWildcard(edgeKey.groupId, edgeKey.edgeNodeId),
                brokerConfig.getQos()
            );
        }

        publishBirthMessages();
    }

    public void shutdown() {
        for (EdgeKey edgeKey : sequenceMap.keySet()) {
            try {
                SparkplugSequence sequence = sequenceMap.get(edgeKey);
                Long bdSeq = bdSeqMap.get(edgeKey);
                if (sequence == null || bdSeq == null) {
                    continue;
                }
                byte[] payload = payloadBuilder.buildPayload(sequence.next(),
                    Collections.singletonList(buildBdSeqMetric(bdSeq))
                );
                publisherManager.publish(
                    SparkplugTopicBuilder.nodeDeath(edgeKey.groupId, edgeKey.edgeNodeId),
                    payload,
                    brokerConfig.getQos(),
                    true
                );
            } catch (Exception e) {
                logger.warn("Failed to publish Sparkplug NDEATH: {}", e.getMessage());
            }
        }
        if (!monitoredTags.isEmpty()) {
            try {
                GatewayTagManager tagManager = gatewayContext.getTagManager();
                List<TagChangeListener> listeners = new ArrayList<>();
                for (int i = 0; i < monitoredTags.size(); i++) {
                    listeners.add(tagChangeListener);
                }
                CompletableFuture<Void> unsubscribeFuture = tagManager.unsubscribeAsync(monitoredTags, listeners);
                unsubscribeFuture.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Failed to unsubscribe Sparkplug tags: {}", e.getMessage());
            }
        }
        monitoredTags.clear();
        tagDeviceMap.clear();
        tagMetricMap.clear();
        tagEdgeMap.clear();
        deviceTagsMap.clear();
        sequenceMap.clear();
        bdSeqMap.clear();
    }

    private void subscribeToTags() {
        if (monitoredTags.isEmpty()) {
            return;
        }
        try {
            GatewayTagManager tagManager = gatewayContext.getTagManager();
            List<TagChangeListener> listeners = new ArrayList<>();
            for (int i = 0; i < monitoredTags.size(); i++) {
                listeners.add(tagChangeListener);
            }
            CompletableFuture<Void> subscriptionFuture = tagManager.subscribeAsync(monitoredTags, listeners);
            subscriptionFuture.get(10, TimeUnit.SECONDS);
            logger.info("Subscribed to {} Sparkplug tags for config {}", monitoredTags.size(), publishConfig.getName());
        } catch (Exception e) {
            logger.error("Failed to subscribe to Sparkplug tags", e);
        }
    }

    private void publishBirthMessages() {
        java.util.Map<TagPath, QualifiedValue> values = readCurrentValues(monitoredTags);

        for (EdgeKey edgeKey : sequenceMap.keySet()) {
            if (edgeKey == null) {
                continue;
            }
            SparkplugSequence sequence = sequenceMap.get(edgeKey);
            if (sequence == null) {
                continue;
            }
            Long bdSeq = bdSeqMap.get(edgeKey);
            if (bdSeq == null) {
                bdSeq = bdSeqManager.next();
                bdSeqMap.put(edgeKey, bdSeq);
            }

            List<TagPath> nodeTags = tagsForEdge(edgeKey, null);
            List<Metric> nodeMetrics = buildMetricsForTags(values, nodeTags);
            nodeMetrics.add(buildBdSeqMetric(bdSeq));

            sequence.reset();
            byte[] payload = payloadBuilder.buildPayload(sequence.next(), nodeMetrics);
            publisherManager.publish(
                SparkplugTopicBuilder.nodeBirth(edgeKey.groupId, edgeKey.edgeNodeId),
                payload,
                brokerConfig.getQos(),
                true
            );

            java.util.Map<String, List<TagPath>> deviceMap = deviceTagsMap.get(edgeKey);
            if (deviceMap == null) {
                continue;
            }
            for (String deviceId : deviceMap.keySet()) {
                if (deviceId == null || deviceId.trim().isEmpty()) {
                    continue;
                }
                List<Metric> deviceMetrics = buildMetricsForTags(values, deviceMap.get(deviceId));
                deviceMetrics.add(buildBdSeqMetric(bdSeq));
                byte[] devicePayload = payloadBuilder.buildPayload(sequence.next(), deviceMetrics);
                publisherManager.publish(
                    SparkplugTopicBuilder.deviceBirth(edgeKey.groupId, edgeKey.edgeNodeId, deviceId),
                    devicePayload,
                    brokerConfig.getQos(),
                    true
                );
            }
        }
    }

    private void publishDeviceBirth(EdgeKey edgeKey, String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return;
        }
        java.util.Map<String, List<TagPath>> deviceMap = deviceTagsMap.get(edgeKey);
        if (deviceMap == null) {
            return;
        }
        List<TagPath> deviceTags = deviceMap.get(deviceId);
        if (deviceTags == null || deviceTags.isEmpty()) {
            return;
        }
        Long bdSeq = bdSeqMap.get(edgeKey);
        if (bdSeq == null) {
            bdSeq = bdSeqManager.next();
            bdSeqMap.put(edgeKey, bdSeq);
        }
        SparkplugSequence sequence = sequenceMap.get(edgeKey);
        if (sequence == null) {
            sequence = new SparkplugSequence();
            sequenceMap.put(edgeKey, sequence);
        }
        java.util.Map<TagPath, QualifiedValue> values = readCurrentValues(deviceTags);
        List<Metric> deviceMetrics = buildMetricsForTags(values, deviceTags);
        deviceMetrics.add(buildBdSeqMetric(bdSeq));
        byte[] devicePayload = payloadBuilder.buildPayload(sequence.next(), deviceMetrics);
        publisherManager.publish(
            SparkplugTopicBuilder.deviceBirth(edgeKey.groupId, edgeKey.edgeNodeId, deviceId),
            devicePayload,
            brokerConfig.getQos(),
            true
        );
    }

    private void publishEdgeBirth(EdgeKey edgeKey) {
        if (edgeKey == null) {
            return;
        }
        java.util.Map<TagPath, QualifiedValue> values = readCurrentValues(monitoredTags);
        SparkplugSequence sequence = sequenceMap.get(edgeKey);
        if (sequence == null) {
            sequence = new SparkplugSequence();
            sequenceMap.put(edgeKey, sequence);
        }
        Long bdSeq = bdSeqMap.get(edgeKey);
        if (bdSeq == null) {
            bdSeq = bdSeqManager.next();
            bdSeqMap.put(edgeKey, bdSeq);
        }

        List<TagPath> nodeTags = tagsForEdge(edgeKey, null);
        List<Metric> nodeMetrics = buildMetricsForTags(values, nodeTags);
        nodeMetrics.add(buildBdSeqMetric(bdSeq));

        sequence.reset();
        byte[] payload = payloadBuilder.buildPayload(sequence.next(), nodeMetrics);
        publisherManager.publish(
            SparkplugTopicBuilder.nodeBirth(edgeKey.groupId, edgeKey.edgeNodeId),
            payload,
            brokerConfig.getQos(),
            true
        );

        java.util.Map<String, List<TagPath>> deviceMap = deviceTagsMap.get(edgeKey);
        if (deviceMap == null) {
            return;
        }
        for (String deviceId : deviceMap.keySet()) {
            if (deviceId == null || deviceId.trim().isEmpty()) {
                continue;
            }
            List<Metric> deviceMetrics = buildMetricsForTags(values, deviceMap.get(deviceId));
            deviceMetrics.add(buildBdSeqMetric(bdSeq));
            byte[] devicePayload = payloadBuilder.buildPayload(sequence.next(), deviceMetrics);
            publisherManager.publish(
                SparkplugTopicBuilder.deviceBirth(edgeKey.groupId, edgeKey.edgeNodeId, deviceId),
                devicePayload,
                brokerConfig.getQos(),
                true
            );
        }
    }

    private List<TagPath> browseTags() {
        List<TagPath> tagPaths = new ArrayList<>();
        for (SparkplugDeviceMapping mapping : publishConfig.getDeviceMappings()) {
            if (mapping == null || !mapping.isEnabled()) {
                continue;
            }
            try {
                TagPath parsedPath = TagPathParser.parse(mapping.getSourcePattern());
                EdgeKey edgeKey = resolveEdgeKey(mapping);
                if (edgeKey == null || edgeKey.groupId == null || edgeKey.edgeNodeId == null) {
                    logger.warn("Skipping Sparkplug mapping with missing group/edge: {}", mapping.getSourcePattern());
                    continue;
                }
                List<TagPath> mappingTags = browseTagsRecursive(
                    parsedPath.getSource(),
                    parsedPath.toStringPartial()
                );
                tagPaths.addAll(mappingTags);
                String deviceKey = normalizeDeviceKey(mapping.getDeviceId());
                for (TagPath tagPath : mappingTags) {
                    tagDeviceMap.put(tagPath, deviceKey);
                    tagMetricMap.put(tagPath, resolveMetricName(tagPath, mapping.getSourcePattern()));
                    tagEdgeMap.put(tagPath, edgeKey);
                    deviceTagsMap
                        .computeIfAbsent(edgeKey, ignored -> new java.util.concurrent.ConcurrentHashMap<>())
                        .computeIfAbsent(deviceKey, ignored -> new ArrayList<>())
                        .add(tagPath);
                    sequenceMap.computeIfAbsent(edgeKey, ignored -> new SparkplugSequence());
                    bdSeqMap.computeIfAbsent(edgeKey, ignored -> bdSeqManager.next());
                }
            } catch (Exception e) {
                logger.warn("Error browsing Sparkplug tags for mapping {}: {}",
                    mapping.getSourcePattern(), e.getMessage());
            }
        }

        return tagPaths.stream().distinct().collect(Collectors.toList());
    }

    private List<TagPath> browseTagsRecursive(String providerName, String path) {
        List<TagPath> tags = new ArrayList<>();
        GatewayTagManager tagManager = gatewayContext.getTagManager();

        try {
            TagPath browsePath = TagPathParser.parse(providerName, path);
            Results<NodeDescription> results = tagManager.browseAsync(
                browsePath,
                BrowseFilter.NONE
            ).get(10, TimeUnit.SECONDS);

            Collection<NodeDescription> nodes = results != null ? results.getResults() : null;
            if (nodes == null) {
                return browseTagsFromConfig(providerName, browsePath);
            }

            for (NodeDescription node : nodes) {
                TagPath nodePath = node.getFullPath();
                if (nodePath == null) {
                    logger.warn("Browse returned node with null path under {}/{} (name: {})", providerName, path, node.getName());
                    continue;
                }
                TagObjectType objectType = node.getObjectType();
                boolean isUdtInstance = objectType == TagObjectType.UdtInstance;
                boolean shouldBrowseChildren = node.hasChildren() || isUdtInstance;

                if (shouldBrowseChildren) {
                    tags.addAll(browseTagsRecursive(providerName, nodePath.toStringPartial()));
                    continue;
                }

                if (objectType == null || objectType == TagObjectType.AtomicTag || objectType == TagObjectType.Node) {
                    tags.add(nodePath);
                }
            }
        } catch (TimeoutException e) {
            logger.warn("Timeout browsing Sparkplug tags at {}/{}", providerName, path);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("Error browsing Sparkplug tags at {}/{}: {}", providerName, path, e.getMessage());
        }

        return tags;
    }

    private List<TagPath> browseTagsFromConfig(String providerName, TagPath browsePath) {
        List<TagPath> tags = new ArrayList<>();
        TagProvider provider = gatewayContext.getTagManager().getTagProvider(providerName);
        if (provider == null) {
            logger.warn("Tag provider not found for Sparkplug config fallback: {}", providerName);
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
                TagObjectType type = configModel.getType();
                if (type == TagObjectType.AtomicTag) {
                    tags.add(configModel.getPath());
                }
            }
        } catch (TimeoutException e) {
            logger.warn("Timeout reading Sparkplug tag configs at {}/{}", providerName, browsePath.toStringPartial());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("Error reading Sparkplug tag configs at {}/{}: {}", providerName, browsePath.toStringPartial(), e.getMessage());
        }
        return tags;
    }

    private class SparkplugTagChangeListener implements TagChangeListener {
        @Override
        public void tagChanged(TagChangeEvent event) {
            if (event == null) {
                return;
            }
            TagPath tagPath = event.getTagPath();
            QualifiedValue value = event.getValue();
            if (tagPath == null || value == null) {
                return;
            }
            String deviceId = resolveDeviceId(tagPath);
            EdgeKey edgeKey = tagEdgeMap.get(tagPath);
            if (edgeKey == null) {
                return;
            }
            SparkplugSequence sequence = sequenceMap.computeIfAbsent(edgeKey, ignored -> new SparkplugSequence());

            Metric metric = buildMetric(tagPath, value);
            if (metric == null) {
                return;
            }

            byte[] payload = payloadBuilder.buildPayload(sequence.next(), Collections.singletonList(metric));
            if (deviceId == null || deviceId.isEmpty()) {
                publisherManager.publish(
                    SparkplugTopicBuilder.nodeData(edgeKey.groupId, edgeKey.edgeNodeId),
                    payload,
                    brokerConfig.getQos(),
                    brokerConfig.isRetained()
                );
            } else {
                publisherManager.publish(
                    SparkplugTopicBuilder.deviceData(edgeKey.groupId, edgeKey.edgeNodeId, deviceId),
                    payload,
                    brokerConfig.getQos(),
                    brokerConfig.isRetained()
                );
            }
        }

        @Override
        public boolean isLightweight() {
            return true;
        }
    }

    private String resolveDeviceId(TagPath tagPath) {
        String deviceId = tagDeviceMap.get(tagPath);
        if (NODE_KEY.equals(deviceId)) {
            return null;
        }
        return deviceId;
    }

    private Metric buildMetric(TagPath tagPath, QualifiedValue value) {
        MetricDataType dataType = dataType(value.getValue());
        String metricName = tagMetricMap.getOrDefault(tagPath, metricName(tagPath));
        Object metricValue = normalizeValue(dataType, value.getValue());
        try {
            return new Metric.MetricBuilder(
                metricName,
                dataType,
                metricValue
            ).timestamp(metricTimestamp(value)).createMetric();
        } catch (Exception e) {
            logger.warn("Failed to build Sparkplug metric for {}: {}", tagPath, e.getMessage());
            return null;
        }
    }

    private String metricName(TagPath tagPath) {
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

    private MetricDataType dataType(Object value) {
        if (value == null) {
            return MetricDataType.String;
        }
        if (value instanceof Boolean) {
            return MetricDataType.Boolean;
        }
        if (value instanceof Date) {
            return MetricDataType.DateTime;
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return MetricDataType.Int32;
        }
        if (value instanceof Long) {
            return MetricDataType.Int64;
        }
        if (value instanceof Float) {
            return MetricDataType.Float;
        }
        if (value instanceof Double) {
            return MetricDataType.Double;
        }
        return MetricDataType.String;
    }

    private Object normalizeValue(MetricDataType dataType, Object value) {
        if (value == null) {
            return null;
        }
        if (dataType == MetricDataType.Int32) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return value;
        }
        if (dataType == MetricDataType.Int64) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return value;
        }
        if (dataType == MetricDataType.Float) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return value;
        }
        if (dataType == MetricDataType.Double) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return value;
        }
        if (dataType == MetricDataType.Boolean) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        }
        if (dataType == MetricDataType.DateTime) {
            return value;
        }
        return value;
    }

    private Metric buildBdSeqMetric(long bdSeq) {
        try {
            return new Metric.MetricBuilder(
                "bdSeq",
                MetricDataType.UInt64,
                java.math.BigInteger.valueOf(bdSeq)
            ).timestamp(new Date()).createMetric();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build bdSeq metric", e);
        }
    }

    private java.util.Map<TagPath, QualifiedValue> readCurrentValues(List<TagPath> tagPaths) {
        if (tagPaths == null || tagPaths.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            GatewayTagManager tagManager = gatewayContext.getTagManager();
            CompletableFuture<List<QualifiedValue>> future = tagManager.readAsync(tagPaths);
            List<QualifiedValue> values = future.get(10, TimeUnit.SECONDS);
            java.util.Map<TagPath, QualifiedValue> result = new java.util.HashMap<>();
            for (int i = 0; i < tagPaths.size(); i++) {
                TagPath tagPath = tagPaths.get(i);
                QualifiedValue value = i < values.size() ? values.get(i) : null;
                result.put(tagPath, value);
            }
            return result;
        } catch (Exception e) {
            logger.warn("Failed to read Sparkplug tag values: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<Metric> buildMetricsForTags(
        java.util.Map<TagPath, QualifiedValue> values,
        List<TagPath> tagPaths
    ) {
        if (tagPaths == null || tagPaths.isEmpty()) {
            return new ArrayList<>();
        }
        List<Metric> metrics = new ArrayList<>();
        for (TagPath tagPath : tagPaths) {
            QualifiedValue value = values.get(tagPath);
            if (value == null) {
                continue;
            }
            Metric metric = buildMetric(tagPath, value);
            if (metric != null) {
                metrics.add(metric);
            }
        }
        return metrics;
    }

    private Date metricTimestamp(QualifiedValue value) {
        if (value.getTimestamp() != null) {
            return value.getTimestamp();
        }
        return new Date();
    }

    private void handleCommand(String topic, byte[] payload) {
        if (topic == null || payload == null) {
            return;
        }
        if (!isRebirthCommand(payload)) {
            return;
        }
        if (topic.contains("/NCMD/")) {
            logger.info("Received Sparkplug NCMD rebirth request");
            EdgeKey edgeKey = extractEdgeKey(topic);
            if (edgeKey != null) {
                publishEdgeBirth(edgeKey);
            } else {
                publishBirthMessages();
            }
            return;
        }
        if (topic.contains("/DCMD/")) {
            String deviceId = extractDeviceId(topic);
            EdgeKey edgeKey = extractEdgeKey(topic);
            if (deviceId != null && edgeKey != null) {
                logger.info("Received Sparkplug DCMD rebirth request for device {}", deviceId);
                publishDeviceBirth(edgeKey, deviceId);
            }
        }
    }

    private boolean isRebirthCommand(byte[] payloadBytes) {
        try {
            SparkplugBPayloadDecoder decoder = new SparkplugBPayloadDecoder();
            SparkplugBPayload payload = decoder.buildFromByteArray(payloadBytes, null);
            if (payload == null || payload.getMetrics() == null) {
                return false;
            }
            for (Metric metric : payload.getMetrics()) {
                if (metric == null || metric.getName() == null) {
                    continue;
                }
                String name = metric.getName();
                if (
                    "Node Control/Rebirth".equalsIgnoreCase(name) ||
                    "Device Control/Rebirth".equalsIgnoreCase(name) ||
                    name.endsWith("Rebirth") ||
                    name.endsWith("REBIRTH")
                ) {
                    Object value = metric.getValue();
                    if (value instanceof Boolean) {
                        return (Boolean) value;
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to decode Sparkplug command payload: {}", e.getMessage());
        }
        return false;
    }

    private String extractDeviceId(String topic) {
        String[] parts = topic.split("/");
        if (parts.length < 5) {
            return null;
        }
        return parts[4];
    }

    private EdgeKey extractEdgeKey(String topic) {
        String[] parts = topic.split("/");
        if (parts.length < 4) {
            return null;
        }
        return new EdgeKey(parts[1], parts[3]);
    }

    private String resolveMetricName(TagPath tagPath, String sourcePattern) {
        if (sourcePattern == null || sourcePattern.isEmpty()) {
            return metricName(tagPath);
        }
        String full = tagPath.toStringFull();
        if (!full.startsWith(sourcePattern)) {
            return metricName(tagPath);
        }
        String remainder = full.substring(sourcePattern.length());
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }
        if (remainder.isEmpty()) {
            return metricName(tagPath);
        }
        return remainder;
    }

    private EdgeKey resolveEdgeKey(SparkplugDeviceMapping mapping) {
        String groupId = mapping.getGroupId();
        String edgeNodeId = mapping.getEdgeNodeId();
        if (groupId == null || groupId.trim().isEmpty()) {
            groupId = publishConfig.getGroupId();
        }
        if (edgeNodeId == null || edgeNodeId.trim().isEmpty()) {
            edgeNodeId = publishConfig.getEdgeNodeId();
        }
        if (groupId == null || groupId.trim().isEmpty() || edgeNodeId == null || edgeNodeId.trim().isEmpty()) {
            return null;
        }
        return new EdgeKey(groupId, edgeNodeId);
    }

    private EdgeKey getPrimaryEdgeKey() {
        if (!sequenceMap.isEmpty()) {
            return sequenceMap.keySet().iterator().next();
        }
        if (publishConfig.getGroupId() != null && publishConfig.getEdgeNodeId() != null) {
            return new EdgeKey(publishConfig.getGroupId(), publishConfig.getEdgeNodeId());
        }
        return null;
    }

    private List<TagPath> tagsForEdge(EdgeKey edgeKey, String deviceId) {
        if (edgeKey == null) {
            return Collections.emptyList();
        }
        java.util.Map<String, List<TagPath>> deviceMap = deviceTagsMap.get(edgeKey);
        if (deviceMap == null) {
            return Collections.emptyList();
        }
        if (deviceId == null) {
            return deviceMap.getOrDefault(NODE_KEY, Collections.emptyList());
        }
        return deviceMap.getOrDefault(deviceId, Collections.emptyList());
    }

    private String normalizeDeviceKey(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return NODE_KEY;
        }
        return deviceId;
    }

    private static class EdgeKey {
        private final String groupId;
        private final String edgeNodeId;

        private EdgeKey(String groupId, String edgeNodeId) {
            this.groupId = groupId;
            this.edgeNodeId = edgeNodeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EdgeKey edgeKey = (EdgeKey) o;
            return java.util.Objects.equals(groupId, edgeKey.groupId) &&
                java.util.Objects.equals(edgeNodeId, edgeKey.edgeNodeId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(groupId, edgeNodeId);
        }
    }
}
