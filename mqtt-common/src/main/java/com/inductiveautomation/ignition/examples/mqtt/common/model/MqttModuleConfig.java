package com.inductiveautomation.ignition.examples.mqtt.common.model;

/**
 * Combined configuration model for the MQTT UNS Publisher module.
 * Contains both broker connection settings and tag publishing configuration.
 */
public class MqttModuleConfig {
    
    private MqttBrokerConfig broker;
    private TagPublishConfig tags;
    
    /**
     * Default constructor for JSON deserialization
     */
    public MqttModuleConfig() {
    }
    
    /**
     * Constructor with both configurations
     */
    public MqttModuleConfig(MqttBrokerConfig broker, TagPublishConfig tags) {
        this.broker = broker;
        this.tags = tags;
    }
    
    /**
     * Get the MQTT broker configuration
     */
    public MqttBrokerConfig getBroker() {
        return broker;
    }
    
    /**
     * Set the MQTT broker configuration
     */
    public void setBroker(MqttBrokerConfig broker) {
        this.broker = broker;
    }
    
    /**
     * Get the tag publishing configuration
     */
    public TagPublishConfig getTags() {
        return tags;
    }
    
    /**
     * Set the tag publishing configuration
     */
    public void setTags(TagPublishConfig tags) {
        this.tags = tags;
    }
    
    /**
     * Validate the complete configuration
     */
    public void validate() throws IllegalArgumentException {
        if (broker != null) {
            broker.validate();
        }
        if (tags != null) {
            tags.validate();
        }
    }
    
    /**
     * Check if broker configuration is present and valid
     */
    public boolean hasBrokerConfig() {
        return broker != null;
    }
    
    /**
     * Check if tag configuration is present and valid
     */
    public boolean hasTagConfig() {
        return tags != null;
    }
    
    @Override
    public String toString() {
        return "MqttModuleConfig{" +
                "broker=" + (broker != null ? broker.toString() : "null") +
                ", tags=" + (tags != null ? tags.toString() : "null") +
                '}';
    }
}
