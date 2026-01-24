package com.inductiveautomation.ignition.examples.mqtt.common.model;

/**
 * Represents the current state of the MQTT connection
 */
public enum ConnectionState {
    /**
     * Not connected and not attempting to connect
     */
    DISCONNECTED("Disconnected"),
    
    /**
     * Attempting to establish connection
     */
    CONNECTING("Connecting"),
    
    /**
     * Successfully connected to broker
     */
    CONNECTED("Connected"),
    
    /**
     * Connection failed, will retry
     */
    RECONNECTING("Reconnecting"),
    
    /**
     * Connection error, stopped trying
     */
    ERROR("Error");
    
    private final String displayName;
    
    ConnectionState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isConnected() {
        return this == CONNECTED;
    }
    
    public boolean canPublish() {
        return this == CONNECTED;
    }
}
