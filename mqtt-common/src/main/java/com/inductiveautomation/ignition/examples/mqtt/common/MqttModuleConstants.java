package com.inductiveautomation.ignition.examples.mqtt.common;

/**
 * Constants used throughout the MQTT UNS Publisher module.
 */
public class MqttModuleConstants {
    
    /**
     * Module ID as defined in build.gradle.kts
     */
    public static final String MODULE_ID = "com.inductiveautomation.mqtt.uns";
    
    /**
     * Module display name
     */
    public static final String MODULE_NAME = "MQTT UNS Publisher";
    
    /**
     * Default MQTT broker URL
     */
    public static final String DEFAULT_BROKER_URL = "tcp://localhost:1883";
    
    /**
     * Default MQTT client ID prefix
     */
    public static final String DEFAULT_CLIENT_ID_PREFIX = "ignition-mqtt-";
    
    /**
     * Default QoS level
     */
    public static final int DEFAULT_QOS = 1;
    
    /**
     * Default keep alive interval (seconds)
     */
    public static final int DEFAULT_KEEP_ALIVE = 60;
    
    /**
     * Default connection timeout (seconds)
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 30;
    
    /**
     * Maximum reconnection attempts before giving up
     */
    public static final int MAX_RECONNECT_ATTEMPTS = 10;
    
    /**
     * Initial reconnection delay (milliseconds)
     */
    public static final long INITIAL_RECONNECT_DELAY = 1000;
    
    /**
     * Maximum reconnection delay (milliseconds)
     */
    public static final long MAX_RECONNECT_DELAY = 30000;
    
    /**
     * Default value deadband for change detection
     */
    public static final double DEFAULT_VALUE_DEADBAND = 0.0;
    
    private MqttModuleConstants() {
        // Prevent instantiation
    }
}
