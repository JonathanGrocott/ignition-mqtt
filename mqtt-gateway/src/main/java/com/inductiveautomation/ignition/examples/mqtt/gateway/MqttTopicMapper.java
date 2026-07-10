package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Maps Ignition tag paths to MQTT topics.
 * 
 * Converts tag paths like "[default]Site/Area/Line/Device/Temperature" 
 * to MQTT topics like "default/site/area/line/device/temperature"
 */
public class MqttTopicMapper {
    
    private List<TopicMapping> topicMappings = new ArrayList<>();
    private volatile List<TopicMapping> enabledMappingsSorted = new ArrayList<>();
    
    /**
     * Sets the topic mappings to use for custom tag-to-topic transformations
     */
    public void setTopicMappings(List<TopicMapping> mappings) {
        this.topicMappings = mappings != null ? new ArrayList<>(mappings) : new ArrayList<>();
        rebuildIndex(this.topicMappings);
    }
    
    /**
     * Gets the current topic mappings
     */
    public List<TopicMapping> getTopicMappings() {
        return topicMappings;
    }

    /**
     * Finds the best matching enabled mapping for a full tag path.
     * Returns the first route from the all-matches API for backward compatibility.
     */
    public TopicMapping findBestMapping(String fullPath) {
        if (fullPath == null) {
            return null;
        }

        List<TopicMapping> matches = findMatchingMappings(fullPath);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * Finds all enabled mappings that match a full tag path.
     *
     * Results are sorted from most-specific source pattern to least-specific source pattern,
     * preserving configuration order for mappings with the same source pattern length. Exact
     * duplicate route definitions are collapsed so copy/paste duplicates do not publish twice.
     */
    public List<TopicMapping> findMatchingMappings(String fullPath) {
        if (fullPath == null) {
            return Collections.emptyList();
        }

        Map<String, TopicMapping> matches = new LinkedHashMap<>();
        for (TopicMapping mapping : enabledMappingsSorted) {
            if (!mapping.matches(fullPath)) {
                continue;
            }
            matches.putIfAbsent(buildRouteDefinitionKey(mapping), mapping);
        }

        return new ArrayList<>(matches.values());
    }
    
    /**
     * Converts a tag path to an MQTT topic using direct path mapping.
     * 
     * Rules:
     * - Provider name becomes first level (brackets removed)
     * - Path separators (/) are preserved
     * - All lowercase
     * - Invalid MQTT characters are replaced with underscores
     * 
     * Examples:
     * - [default]Folder/Tag -> default/folder/tag
     * - [edge]Sensors/Temperature -> edge/sensors/temperature
     * - [default]Site 1/Area 2/Tag -> default/site_1/area_2/tag
     * 
     * @param tagPath The Ignition tag path
     * @return The MQTT topic string
     */
    public String mapTagToTopic(TagPath tagPath) {
        if (tagPath == null) {
            throw new IllegalArgumentException("Tag path cannot be null");
        }
        
        StringBuilder topic = new StringBuilder();
        
        // Add provider (without brackets)
        String provider = tagPath.getSource();
        if (provider != null && !provider.isEmpty()) {
            topic.append(sanitizeTopicSegment(provider, false));
        } else {
            topic.append("default");
        }
        
        // Add path components
        String itemPath = tagPath.toStringPartial();
        if (itemPath != null && !itemPath.isEmpty()) {
            String[] pathParts = itemPath.split("/");
            for (String part : pathParts) {
                if (!part.isEmpty()) {
                    topic.append("/");
                    topic.append(sanitizeTopicSegment(part, false));
                }
            }
        }
        
        return topic.toString();
    }
    
    /**
     * Maps a tag path to an MQTT topic using a specific mapping.
     * 
     * This method applies a pre-selected mapping to transform the tag path to a topic.
     * It's useful when you've already identified the correct mapping and want to avoid
     * duplicate mapping searches.
     * 
     * Example:
     *   Mapping: "[default]Site1/Area2" -> "enterprise/nashville/assembly"
     *   Tag: [default]Site1/Area2/Line3/Temperature
     *   Result: enterprise/nashville/assembly/line3/temperature
     * 
     * @param tagPath The Ignition tag path
     * @param mapping The specific mapping to apply
     * @return The MQTT topic string (with mapping applied)
     */
    public String mapTagToTopicWithMapping(TagPath tagPath, TopicMapping mapping) {
        if (tagPath == null) {
            throw new IllegalArgumentException("Tag path cannot be null");
        }
        
        if (mapping == null) {
            return mapTagToTopic(tagPath);
        }
        
        String fullPath = tagPath.toStringFull();

        if (mapping.getPublishMode() == com.inductiveautomation.ignition.examples.mqtt.common.model.TopicPublishMode.SINGLE_TOPIC) {
            return sanitizeTopicSegment(mapping.getTopicPrefix(), true);
        }
        
        // Apply the mapping transformation
        String remainder = fullPath.substring(mapping.getSourcePattern().length());
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }
        
        String topic = mapping.getTopicPrefix();
        if (!remainder.isEmpty()) {
            // Sanitize the remainder portion
            topic = topic + "/" + sanitizeTopicSegment(remainder, mapping.isPreserveTopicCase());
        }
        
        return topic;
    }
    
    /**
     * Maps a tag path to an MQTT topic, applying custom topic mappings if available.
     * 
     * This method first checks if any enabled topic mapping matches the tag path.
     * If a match is found, it applies the mapping to transform the topic.
     * Otherwise, it falls back to the default mapping.
     * 
     * Example with mapping:
     *   Mapping: "[default]Site1/Area2" -> "enterprise/nashville/assembly"
     *   Tag: [default]Site1/Area2/Line3/Temperature
     *   Result: enterprise/nashville/assembly/line3/temperature
     * 
     * @param tagPath The Ignition tag path
     * @return The MQTT topic string (with mappings applied if available)
     */
    public String mapTagToTopicWithMappings(TagPath tagPath) {
        if (tagPath == null) {
            throw new IllegalArgumentException("Tag path cannot be null");
        }
        
        String fullPath = tagPath.toStringFull();

        TopicMapping matchedMapping = findBestMapping(fullPath);
        if (matchedMapping != null) {
            return mapTagToTopicWithMapping(tagPath, matchedMapping);
        }
        
        // No mapping found, use default topic generation
        return mapTagToTopic(tagPath);
    }

    private void rebuildIndex(List<TopicMapping> mappings) {
        List<TopicMapping> enabled = new ArrayList<>();

        if (mappings != null) {
            for (TopicMapping mapping : mappings) {
                if (mapping == null || !mapping.isEnabled()) {
                    continue;
                }
                enabled.add(mapping);
            }
        }

        enabled.sort(Comparator.comparingInt((TopicMapping mapping) -> {
            String source = mapping.getSourcePattern();
            return source != null ? source.length() : 0;
        }).reversed());

        enabledMappingsSorted = Collections.unmodifiableList(enabled);
    }

    private String buildRouteDefinitionKey(TopicMapping mapping) {
        return String.join(
            "\u0000",
            String.valueOf(mapping.getBrokerId()),
            nullToEmpty(mapping.getSourcePattern()),
            nullToEmpty(mapping.getTopicPrefix()),
            String.valueOf(mapping.getPublishMode()),
            String.valueOf(mapping.isPreserveTopicCase()),
            String.valueOf(mapping.getBatchWindowMs()),
            String.valueOf(mapping.getMaxBatchSize()),
            String.valueOf(mapping.isUseDefaultPayloadFields()),
            buildPayloadFieldsKey(mapping)
        );
    }

    private String buildPayloadFieldsKey(TopicMapping mapping) {
        com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig fields =
            mapping.getPayloadFields();
        if (fields == null) {
            return "";
        }
        Map<String, Boolean> properties = fields.getProperties() != null
            ? new TreeMap<>(fields.getProperties())
            : Collections.emptyMap();
        return String.join(
            "|",
            String.valueOf(fields.isIncludeQuality()),
            String.valueOf(fields.isIncludeQualityCode()),
            String.valueOf(fields.isIncludeTagPath()),
            properties.toString()
        );
    }

    String buildEffectiveRouteKey(Long brokerId, String topic, TopicMapping mapping) {
        return String.join(
            "\u0000",
            String.valueOf(brokerId),
            topic != null ? topic : "",
            String.valueOf(mapping.getPublishMode())
        );
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
    
    /**
     * Sanitizes a single topic segment by:
     * - Converting to lowercase
     * - Replacing spaces with underscores
     * - Removing or replacing invalid MQTT characters
     * 
     * @param segment The topic segment to sanitize
     * @return Sanitized segment
     */
    public String sanitizeTopicSegment(String segment) {
        return sanitizeTopicSegment(segment, false);
    }

    public String sanitizeTopicSegment(String segment, boolean preserveCase) {
        if (segment == null || segment.isEmpty()) {
            return "";
        }

        String sanitized = segment
            .replace(" ", "_")  // Spaces to underscores
            .replace("[", "")   // Remove brackets
            .replace("]", "")
            .replace("#", "")   // Remove MQTT wildcards
            .replace("+", "");

        if (!preserveCase) {
            sanitized = sanitized.toLowerCase();
        }

        return preserveCase
            ? sanitized.replaceAll("[^A-Za-z0-9_/\\-]", "_")
            : sanitized.replaceAll("[^a-z0-9_/\\-]", "_");
    }
    
    /**
     * Validates an MQTT topic string.
     * 
     * @param topic The topic to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            return false;
        }
        
        // Check for wildcards (not allowed in publish topics)
        if (topic.contains("#") || topic.contains("+")) {
            return false;
        }
        
        // Check length (some brokers have limits)
        if (topic.length() > 65535) {
            return false;
        }
        
        // Check for double slashes or leading/trailing slashes
        if (topic.contains("//") || topic.startsWith("/") || topic.endsWith("/")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Applies a custom topic override if provided, otherwise uses default mapping.
     * 
     * @param tagPath The tag path
     * @param customTopic Custom topic override (can be null)
     * @return The MQTT topic to use
     */
    public String applyTopicOverride(TagPath tagPath, String customTopic) {
        if (customTopic != null && !customTopic.trim().isEmpty()) {
            String sanitized = sanitizeTopicSegment(customTopic);
            if (isValidTopic(sanitized)) {
                return sanitized;
            }
        }
        return mapTagToTopic(tagPath);
    }
}
