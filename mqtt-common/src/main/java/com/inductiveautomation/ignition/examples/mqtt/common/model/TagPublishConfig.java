package com.inductiveautomation.ignition.examples.mqtt.common.model;

import com.google.gson.annotations.SerializedName;

import java.util.*;
import java.util.stream.Collectors;

import static com.inductiveautomation.ignition.examples.mqtt.common.MqttModuleConstants.DEFAULT_VALUE_DEADBAND;

/**
 * Configuration for tag publishing settings.
 * Defines which tags to monitor and publish, along with trigger conditions.
 */
public class TagPublishConfig {
    
    @SerializedName("enabled")
    private boolean enabled;
    
    @SerializedName("tagProviders")
    private List<String> tagProviders;
    
    @SerializedName("tagFolders")
    private List<String> tagFolders;
    
    @SerializedName("topicOverrides")
    private Map<String, String> topicOverrides;
    
    @SerializedName("payloadTemplate")
    private String payloadTemplate;

    @SerializedName("payloadFields")
    private PayloadFieldConfig payloadFields;
    
    @SerializedName("includeMetadata")
    private boolean includeMetadata;
    
    @SerializedName("valueDeadband")
    private double valueDeadband;
    
    @SerializedName("publishOnQualityChange")
    private boolean publishOnQualityChange;
    
    @SerializedName("topicMappings")
    private List<TopicMapping> topicMappings;
    
    /**
     * Default constructor with sensible defaults
     */
    public TagPublishConfig() {
        this.enabled = true;
        this.tagProviders = new ArrayList<>();
        this.tagFolders = new ArrayList<>();
        this.topicOverrides = new HashMap<>();
        this.payloadTemplate = null; // null = use default
        this.payloadFields = new PayloadFieldConfig();
        this.includeMetadata = true;
        this.valueDeadband = DEFAULT_VALUE_DEADBAND;
        this.publishOnQualityChange = true;
        this.topicMappings = new ArrayList<>();
    }
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public List<String> getTagProviders() {
        return tagProviders;
    }
    
    public void setTagProviders(List<String> tagProviders) {
        this.tagProviders = tagProviders != null ? tagProviders : new ArrayList<>();
    }
    
    public List<String> getTagFolders() {
        return tagFolders;
    }
    
    public void setTagFolders(List<String> tagFolders) {
        this.tagFolders = tagFolders != null ? tagFolders : new ArrayList<>();
    }
    
    public Map<String, String> getTopicOverrides() {
        return topicOverrides;
    }
    
    public void setTopicOverrides(Map<String, String> topicOverrides) {
        this.topicOverrides = topicOverrides != null ? topicOverrides : new HashMap<>();
    }
    
    public String getPayloadTemplate() {
        return payloadTemplate;
    }
    
    public void setPayloadTemplate(String payloadTemplate) {
        this.payloadTemplate = payloadTemplate;
    }

    public PayloadFieldConfig getPayloadFields() {
        return payloadFields;
    }

    public void setPayloadFields(PayloadFieldConfig payloadFields) {
        this.payloadFields = payloadFields;
    }

    /**
     * Returns payload field configuration, falling back to legacy metadata settings when missing.
     */
    public PayloadFieldConfig getPayloadFieldsOrDefault() {
        if (payloadFields != null) {
            return payloadFields;
        }
        PayloadFieldConfig defaults = new PayloadFieldConfig();
        if (includeMetadata) {
            defaults.getProperties().put("dataType", true);
        }
        return defaults;
    }
    
    public boolean isIncludeMetadata() {
        return includeMetadata;
    }
    
    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }
    
    public double getValueDeadband() {
        return valueDeadband;
    }
    
    public void setValueDeadband(double valueDeadband) {
        this.valueDeadband = valueDeadband;
    }
    
    public boolean isPublishOnQualityChange() {
        return publishOnQualityChange;
    }
    
    public void setPublishOnQualityChange(boolean publishOnQualityChange) {
        this.publishOnQualityChange = publishOnQualityChange;
    }
    
    public List<TopicMapping> getTopicMappings() {
        return topicMappings;
    }
    
    public void setTopicMappings(List<TopicMapping> topicMappings) {
        this.topicMappings = topicMappings != null ? topicMappings : new ArrayList<>();
    }
    
    /**
     * Adds a tag provider to monitor
     */
    public void addTagProvider(String provider) {
        if (provider != null && !provider.trim().isEmpty()) {
            if (!tagProviders.contains(provider)) {
                tagProviders.add(provider);
            }
        }
    }
    
    /**
     * Adds a tag folder to monitor (with provider prefix)
     */
    public void addTagFolder(String folder) {
        if (folder != null && !folder.trim().isEmpty()) {
            if (!tagFolders.contains(folder)) {
                tagFolders.add(folder);
            }
        }
    }
    
    /**
     * Adds a topic override for a specific tag path
     */
    public void addTopicOverride(String tagPath, String mqttTopic) {
        if (tagPath != null && mqttTopic != null) {
            topicOverrides.put(tagPath, mqttTopic);
        }
    }
    
    /**
     * Gets the custom topic for a tag path, or null if using default
     */
    public String getTopicOverride(String tagPath) {
        return topicOverrides.get(tagPath);
    }
    
    /**
     * Adds a topic mapping for source pattern -> topic prefix transformation
     */
    public void addTopicMapping(TopicMapping mapping) {
        if (mapping != null) {
            topicMappings.add(mapping);
        }
    }
    
    /**
     * Removes a topic mapping by ID
     */
    public void removeTopicMapping(String id) {
        topicMappings.removeIf(mapping -> mapping.getId().equals(id));
    }
    
    /**
     * Finds the first enabled mapping that matches the given tag path
     */
    public TopicMapping findMatchingMapping(String tagPath) {
        if (tagPath == null) {
            return null;
        }
        // Sort by source pattern length (longest first) to match most specific patterns first
        return topicMappings.stream()
            .filter(TopicMapping::isEnabled)
            .filter(mapping -> mapping.matches(tagPath))
            .max(Comparator.comparingInt(m -> m.getSourcePattern().length()))
            .orElse(null);
    }
    
    /**
     * Validates the configuration
     */
    public void validate() {
        if (enabled && tagProviders.isEmpty() && tagFolders.isEmpty() && topicMappings.isEmpty()) {
            throw new IllegalArgumentException(
                "Tag publishing is enabled but no providers, folders, or topic mappings are configured"
            );
        }
        
        if (valueDeadband < 0) {
            throw new IllegalArgumentException("Value deadband cannot be negative");
        }
    }
    
    @Override
    public String toString() {
        return "TagPublishConfig{" +
               "enabled=" + enabled +
               ", tagProviders=" + tagProviders +
               ", tagFolders=" + tagFolders +
               ", includeMetadata=" + includeMetadata +
               ", valueDeadband=" + valueDeadband +
               ", publishOnQualityChange=" + publishOnQualityChange +
               '}';
    }
    
    /**
     * Creates a copy of this configuration
     */
    public TagPublishConfig copy() {
        TagPublishConfig copy = new TagPublishConfig();
        copy.enabled = this.enabled;
        copy.tagProviders = new ArrayList<>(this.tagProviders);
        copy.tagFolders = new ArrayList<>(this.tagFolders);
        copy.topicOverrides = new HashMap<>(this.topicOverrides);
        copy.payloadTemplate = this.payloadTemplate;
        copy.payloadFields = this.payloadFields != null ? this.payloadFields.copy() : null;
        copy.includeMetadata = this.includeMetadata;
        copy.valueDeadband = this.valueDeadband;
        copy.publishOnQualityChange = this.publishOnQualityChange;
        copy.topicMappings = this.topicMappings.stream()
            .map(TopicMapping::copy)
            .collect(Collectors.toList());
        return copy;
    }
}
