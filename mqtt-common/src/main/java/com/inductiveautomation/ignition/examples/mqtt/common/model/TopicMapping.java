package com.inductiveautomation.ignition.examples.mqtt.common.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for mapping tag paths to custom MQTT topic prefixes.
 * This allows users to map specific tag providers/folders to custom UNS topic paths.
 * 
 * Example:
 *   sourcePattern: "[default]Site1/Area2/Line3"
 *   topicPrefix: "enterprise/nashville/assembly/line3"
 *   
 *   Tag: [default]Site1/Area2/Line3/Temperature
 *   MQTT Topic: enterprise/nashville/assembly/line3/temperature
 */
public class TopicMapping {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("sourcePattern")
    private String sourcePattern;
    
    @SerializedName("topicPrefix")
    private String topicPrefix;
    
    @SerializedName("enabled")
    private boolean enabled;
    
    @SerializedName("brokerId")
    private Long brokerId;
    
    /**
     * Default constructor
     */
    public TopicMapping() {
        this.id = UUID.randomUUID().toString();
        this.sourcePattern = "";
        this.topicPrefix = "";
        this.enabled = true;
        this.brokerId = null;
    }
    
    /**
     * Constructor with all parameters
     */
    public TopicMapping(String id, String sourcePattern, String topicPrefix, boolean enabled, Long brokerId) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.sourcePattern = sourcePattern;
        this.topicPrefix = topicPrefix;
        this.enabled = enabled;
        this.brokerId = brokerId;
    }
    
    /**
     * Constructor with all parameters except brokerId (for backward compatibility)
     */
    public TopicMapping(String id, String sourcePattern, String topicPrefix, boolean enabled) {
        this(id, sourcePattern, topicPrefix, enabled, null);
    }
    
    /**
     * Constructor with source and topic (auto-generates ID, enabled by default)
     */
    public TopicMapping(String sourcePattern, String topicPrefix) {
        this(null, sourcePattern, topicPrefix, true);
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSourcePattern() {
        return sourcePattern;
    }
    
    public void setSourcePattern(String sourcePattern) {
        this.sourcePattern = sourcePattern;
    }
    
    public String getTopicPrefix() {
        return topicPrefix;
    }
    
    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Long getBrokerId() {
        return brokerId;
    }
    
    public void setBrokerId(Long brokerId) {
        this.brokerId = brokerId;
    }
    
    /**
     * Validates the mapping configuration
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (brokerId == null) {
            throw new IllegalArgumentException("Topic mapping must be assigned to a broker");
        }
        if (sourcePattern == null || sourcePattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Source pattern cannot be empty");
        }
        if (topicPrefix == null || topicPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic prefix cannot be empty");
        }
        // Check that source pattern looks like a valid tag path
        if (!sourcePattern.contains("[") || !sourcePattern.contains("]")) {
            throw new IllegalArgumentException(
                "Source pattern must include provider name in brackets, e.g., [default]Folder/Path"
            );
        }
    }
    
    /**
     * Checks if a tag path matches this mapping's source pattern
     */
    public boolean matches(String tagPath) {
        if (tagPath == null || !enabled) {
            return false;
        }
        return tagPath.startsWith(sourcePattern);
    }
    
    /**
     * Applies this mapping to a tag path, returning the custom topic
     * 
     * @param tagPath The full tag path (e.g., "[default]Site1/Area2/Line3/Temperature")
     * @return The custom topic path, or null if mapping doesn't match
     */
    public String applyMapping(String tagPath) {
        if (!matches(tagPath)) {
            return null;
        }
        
        // Extract the remainder after the source pattern
        String remainder = tagPath.substring(sourcePattern.length());
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }
        
        // Combine topic prefix with remainder
        if (remainder.isEmpty()) {
            return topicPrefix;
        } else {
            return topicPrefix + "/" + remainder;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicMapping that = (TopicMapping) o;
        return enabled == that.enabled &&
               Objects.equals(id, that.id) &&
               Objects.equals(sourcePattern, that.sourcePattern) &&
               Objects.equals(topicPrefix, that.topicPrefix) &&
               Objects.equals(brokerId, that.brokerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, sourcePattern, topicPrefix, enabled, brokerId);
    }
    
    @Override
    public String toString() {
        return "TopicMapping{" +
               "id='" + id + '\'' +
               ", sourcePattern='" + sourcePattern + '\'' +
               ", topicPrefix='" + topicPrefix + '\'' +
               ", enabled=" + enabled +
               ", brokerId=" + brokerId +
               '}';
    }
    
    /**
     * Creates a copy of this mapping
     */
    public TopicMapping copy() {
        return new TopicMapping(id, sourcePattern, topicPrefix, enabled, brokerId);
    }
}
