package com.inductiveautomation.ignition.examples.mqtt.gateway.records;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import simpleorm.dataset.SFieldFlags;

/**
 * Stores tag publishing configuration.
 * Links to MqttBrokerConfigRecord to associate tag configurations with specific brokers.
 * 
 * Note: Tag providers, folders, and topic overrides are stored as JSON strings
 * since PersistentRecord doesn't support complex collection types natively.
 */
public class MqttTagConfigRecord extends PersistentRecord {
    
    public static final RecordMeta<MqttTagConfigRecord> META = 
        new RecordMeta<>(MqttTagConfigRecord.class, "MqttTagConfig");
    
    public static final IdentityField Id = new IdentityField(META);
    
    // Foreign key reference to broker configuration
    // This creates a one-to-many relationship: one broker can have multiple tag configs
    public static final LongField BrokerConfigId = 
        new LongField(META, "BrokerConfigId", SFieldFlags.SMANDATORY);
    
    // Configuration name/description
    public static final StringField Name = new StringField(META, "Name", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);
    
    // Enable/disable this tag configuration
    public static final BooleanField Enabled = new BooleanField(META, "Enabled")
        .setDefault(true);
    
    // Tag providers to monitor (stored as JSON array string: ["default", "edge"])
    // Using StringField with max length for JSON storage
    public static final StringField TagProviders = new StringField(META, "TagProviders", 2000)
        .setDefault("[]");
    
    // Tag folders to monitor (stored as JSON array string: ["[default]TestTags", "[default]Production"])
    public static final StringField TagFolders = new StringField(META, "TagFolders", 4000)
        .setDefault("[]");
    
    // Topic mappings (stored as JSON array: [{"sourcePattern":"[default]","topicPrefix":"enterprise/site1","enabled":true}])
    public static final StringField TopicMappings = new StringField(META, "TopicMappings", 4000)
        .setDefault("[]");
    
    // Topic overrides (stored as JSON object string: {"[default]Tag1": "custom/topic/path"})
    public static final StringField TopicOverrides = new StringField(META, "TopicOverrides", 4000)
        .setDefault("{}");
    
    // Custom payload template (Mustache/Handlebars template, null = use default)
    public static final StringField PayloadTemplate = new StringField(META, "PayloadTemplate", 4000);
    
    // Whether to include metadata (quality, timestamp) in MQTT payload
    public static final BooleanField IncludeMetadata = new BooleanField(META, "IncludeMetadata")
        .setDefault(true);
    
    // Value deadband - only publish if value changes by more than this amount
    public static final DoubleField ValueDeadband = new DoubleField(META, "ValueDeadband")
        .setDefault(0.0);
    
    // Whether to publish when tag quality changes (even if value unchanged)
    public static final BooleanField PublishOnQualityChange = new BooleanField(META, "PublishOnQualityChange")
        .setDefault(true);
    
    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
    
    // Convenience getters/setters
    
    public Long getId() {
        return getLong(Id);
    }
    
    public Long getBrokerConfigId() {
        return getLong(BrokerConfigId);
    }
    
    public void setBrokerConfigId(Long brokerConfigId) {
        setLong(BrokerConfigId, brokerConfigId);
    }
    
    public String getName() {
        return getString(Name);
    }
    
    public void setName(String name) {
        setString(Name, name);
    }
    
    public boolean isEnabled() {
        return getBoolean(Enabled);
    }
    
    public void setEnabled(boolean enabled) {
        setBoolean(Enabled, enabled);
    }
    
    public String getTagProvidersJson() {
        return getString(TagProviders);
    }
    
    public void setTagProvidersJson(String json) {
        setString(TagProviders, json);
    }
    
    public String getTagFoldersJson() {
        return getString(TagFolders);
    }
    
    public void setTagFoldersJson(String json) {
        setString(TagFolders, json);
    }
    
    public String getTopicMappingsJson() {
        return getString(TopicMappings);
    }
    
    public void setTopicMappingsJson(String json) {
        setString(TopicMappings, json);
    }
    
    public String getTopicOverridesJson() {
        return getString(TopicOverrides);
    }
    
    public void setTopicOverridesJson(String json) {
        setString(TopicOverrides, json);
    }
    
    public String getPayloadTemplate() {
        return getString(PayloadTemplate);
    }
    
    public void setPayloadTemplate(String template) {
        setString(PayloadTemplate, template);
    }
    
    public boolean isIncludeMetadata() {
        return getBoolean(IncludeMetadata);
    }
    
    public void setIncludeMetadata(boolean include) {
        setBoolean(IncludeMetadata, include);
    }
    
    public double getValueDeadband() {
        return getDouble(ValueDeadband);
    }
    
    public void setValueDeadband(double deadband) {
        setDouble(ValueDeadband, deadband);
    }
    
    public boolean isPublishOnQualityChange() {
        return getBoolean(PublishOnQualityChange);
    }
    
    public void setPublishOnQualityChange(boolean publish) {
        setBoolean(PublishOnQualityChange, publish);
    }
}
