package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.tags.model.TagPath;

/**
 * Maps Ignition tag paths to MQTT topics.
 * 
 * Converts tag paths like "[default]Site/Area/Line/Device/Temperature" 
 * to MQTT topics like "default/site/area/line/device/temperature"
 */
public class MqttTopicMapper {
    
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
            topic.append(sanitizeTopicSegment(provider));
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
                    topic.append(sanitizeTopicSegment(part));
                }
            }
        }
        
        return topic.toString();
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
        if (segment == null || segment.isEmpty()) {
            return "";
        }
        
        return segment
            .toLowerCase()
            .replace(" ", "_")  // Spaces to underscores
            .replace("[", "")   // Remove brackets
            .replace("]", "")
            .replace("#", "")   // Remove MQTT wildcards
            .replace("+", "")
            .replaceAll("[^a-z0-9_/\\-]", "_"); // Replace other invalid chars
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
