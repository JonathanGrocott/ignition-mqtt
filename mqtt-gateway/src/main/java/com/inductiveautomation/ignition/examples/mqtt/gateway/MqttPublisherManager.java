package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.examples.mqtt.common.model.ConnectionState;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.inductiveautomation.ignition.examples.mqtt.common.MqttModuleConstants.*;

/**
 * Manages MQTT client connections and publishes messages to the broker.
 * 
 * This class handles:
 * - Connection lifecycle (connect, disconnect)
 * - Automatic reconnection with exponential backoff
 * - Publishing messages with QoS and retain settings
 * - Connection health monitoring
 * - Thread-safe operations
 */
public class MqttPublisherManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttPublisherManager.class);
    
    private final ModuleStatistics statistics;
    private final AtomicReference<MqttBrokerConfig> config = new AtomicReference<>();
    private final AtomicReference<ConnectionState> connectionState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private volatile boolean slowReconnectMode = false;
    
    private volatile MqttClient mqttClient;
    private ScheduledExecutorService reconnectScheduler;
    private volatile boolean shouldBeConnected = false;
    
    /**
     * Initializes the MQTT publisher manager
     */
    public MqttPublisherManager(ModuleStatistics statistics) {
        this.statistics = statistics;
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MQTT-Reconnect-Thread");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Configures and starts the MQTT connection
     * 
     * @param config The broker configuration
     */
    public synchronized void connect(MqttBrokerConfig config) {
        if (config == null) {
            logger.warn("Cannot connect: configuration is null");
            return;
        }
        
        try {
            config.validate();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid MQTT configuration: {}", e.getMessage());
            setConnectionState(ConnectionState.ERROR);
            return;
        }
        
        this.config.set(config);
        this.shouldBeConnected = true;
        
        logger.info("Initiating MQTT connection to {}", config.getBrokerUrl());
        doConnect();
    }
    
    /**
     * Internal method to perform the actual connection
     */
    private synchronized void doConnect() {
        MqttBrokerConfig cfg = config.get();
        if (cfg == null) {
            logger.warn("Cannot connect: no configuration available");
            return;
        }
        
        setConnectionState(ConnectionState.CONNECTING);
        statistics.incrementConnectionAttempts();
        
        try {
            // Close existing client if present
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            
            // Create new MQTT client
            mqttClient = new MqttClient(
                cfg.getBrokerUrl(),
                cfg.getClientId(),
                new MemoryPersistence()
            );
            
            // Set up callback for connection events
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.warn("MQTT connection lost for client '{}' to {}: {}", 
                        cfg.getClientId(), cfg.getBrokerUrl(), cause.getMessage());
                    setConnectionState(ConnectionState.RECONNECTING);
                    scheduleReconnect();
                }
                
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // We're not subscribing to topics in this module
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    logger.trace("Message delivery complete: {}", token.getMessageId());
                }
            });
            
            // Build connection options
            MqttConnectOptions options = buildConnectOptions(cfg);
            
            // Connect to broker
            mqttClient.connect(options);
            
            // Success!
            setConnectionState(ConnectionState.CONNECTED);
            reconnectAttempts.set(0);
            slowReconnectMode = false;
            statistics.recordSuccessfulConnection();
            logger.info("Successfully connected to MQTT broker: {} (client ID: '{}')", 
                cfg.getBrokerUrl(), cfg.getClientId());
            
        } catch (MqttException e) {
            logger.error("Failed to connect to MQTT broker: {} - {}", 
                        cfg.getBrokerUrl(), e.getMessage(), e);
            statistics.incrementConnectionFailures();
            setConnectionState(ConnectionState.RECONNECTING);
            scheduleReconnect();
        }
    }
    
    /**
     * Builds MQTT connection options from configuration
     */
    private MqttConnectOptions buildConnectOptions(MqttBrokerConfig cfg) {
        MqttConnectOptions options = new MqttConnectOptions();
        
        // Authentication
        if (cfg.getUsername() != null && !cfg.getUsername().isEmpty()) {
            options.setUserName(cfg.getUsername());
        }
        if (cfg.getPassword() != null && !cfg.getPassword().isEmpty()) {
            options.setPassword(cfg.getPassword().toCharArray());
        }
        
        // Connection settings
        options.setCleanSession(cfg.isCleanSession());
        options.setConnectionTimeout(cfg.getConnectionTimeout());
        options.setKeepAliveInterval(cfg.getKeepAlive());
        options.setAutomaticReconnect(false); // We handle reconnection ourselves
        
        // TODO: Add TLS support when cfg.isUseTls() is true
        // This will require certificate configuration in future phases
        
        return options;
    }
    
    /**
     * Schedules a reconnection attempt with exponential backoff
     */
    private void scheduleReconnect() {
        if (!shouldBeConnected) {
            logger.debug("Not scheduling reconnect - should not be connected");
            return;
        }

        if (slowReconnectMode) {
            long slowDelayMs = getSlowReconnectDelayMs();
            logger.info("Scheduling slow reconnection attempt in {} ms", slowDelayMs);
            reconnectScheduler.schedule(() -> {
                if (shouldBeConnected) {
                    logger.info("Attempting slow reconnection");
                    doConnect();
                }
            }, slowDelayMs, TimeUnit.MILLISECONDS);
            return;
        }

        int attempts = reconnectAttempts.incrementAndGet();

        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            slowReconnectMode = true;
            long slowDelayMs = getSlowReconnectDelayMs();
            logger.warn(
                "Exceeded maximum reconnection attempts ({}). Switching to slow retry every {} ms.",
                MAX_RECONNECT_ATTEMPTS,
                slowDelayMs
            );
            reconnectScheduler.schedule(() -> {
                if (shouldBeConnected) {
                    logger.info("Attempting slow reconnection");
                    doConnect();
                }
            }, slowDelayMs, TimeUnit.MILLISECONDS);
            return;
        }

        // Exponential backoff: 1s, 2s, 4s, 8s, 16s, up to MAX_RECONNECT_DELAY
        long delay = Math.min(
            MAX_RECONNECT_DELAY,
            INITIAL_RECONNECT_DELAY * (long) Math.pow(2, attempts - 1)
        );

        logger.info("Scheduling reconnection attempt {} in {} ms", attempts, delay);

        reconnectScheduler.schedule(() -> {
            if (shouldBeConnected) {
                logger.info("Attempting reconnection (attempt {})", attempts);
                doConnect();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private long getSlowReconnectDelayMs() {
        MqttBrokerConfig cfg = config.get();
        int intervalSeconds = cfg != null ? cfg.getSlowReconnectIntervalSeconds() : 0;
        if (intervalSeconds <= 0) {
            intervalSeconds = DEFAULT_SLOW_RECONNECT_INTERVAL_SECONDS;
        }
        return TimeUnit.SECONDS.toMillis(intervalSeconds);
    }
    
    /**
     * Publishes a message to the MQTT broker
     * 
     * @param topic The MQTT topic
     * @param payload The message payload
     * @return true if published successfully, false otherwise
     */
    public boolean publish(String topic, String payload) {
        MqttBrokerConfig cfg = config.get();
        if (cfg == null) {
            logger.warn("Cannot publish: no configuration");
            return false;
        }
        
        return publish(topic, payload, cfg.getQos(), cfg.isRetained());
    }
    
    /**
     * Publishes a message to the MQTT broker with specific QoS and retain settings
     * 
     * @param topic The MQTT topic
     * @param payload The message payload
     * @param qos Quality of Service level (0, 1, or 2)
     * @param retained Whether the message should be retained
     * @return true if published successfully, false otherwise
     */
    public boolean publish(String topic, String payload, int qos, boolean retained) {
        if (!isConnected()) {
            logger.debug("Cannot publish to topic '{}': not connected", topic);
            statistics.incrementMessagesFailedToPublish();
            return false;
        }
        
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(qos);
            message.setRetained(retained);
            
            mqttClient.publish(topic, message);
            statistics.incrementMessagesPublished();
            
            logger.trace("Published message to topic '{}' (QoS: {}, Retained: {})", 
                        topic, qos, retained);
            return true;
            
        } catch (MqttException e) {
            logger.error("Failed to publish to topic '{}': {}", topic, e.getMessage(), e);
            statistics.incrementMessagesFailedToPublish();
            return false;
        }
    }
    
    /**
     * Disconnects from the MQTT broker
     */
    public synchronized void disconnect() {
        logger.info("Disconnecting from MQTT broker");
        shouldBeConnected = false;
        slowReconnectMode = false;
        
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect(5000); // 5 second timeout
                logger.info("Successfully disconnected from MQTT broker");
            } catch (MqttException e) {
                logger.warn("Error during disconnect: {}", e.getMessage());
            }
        }
        
        setConnectionState(ConnectionState.DISCONNECTED);
        reconnectAttempts.set(0);
    }
    
    /**
     * Shuts down the publisher manager and releases resources
     */
    public synchronized void shutdown() {
        logger.info("Shutting down MQTT Publisher Manager");
        
        disconnect();
        
        if (mqttClient != null) {
            try {
                mqttClient.close();
            } catch (MqttException e) {
                logger.warn("Error closing MQTT client: {}", e.getMessage());
            }
            mqttClient = null;
        }
        
        if (reconnectScheduler != null && !reconnectScheduler.isShutdown()) {
            reconnectScheduler.shutdown();
            try {
                if (!reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    reconnectScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                reconnectScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Checks if currently connected to the broker
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connectionState.get() == ConnectionState.CONNECTED &&
               mqttClient != null && mqttClient.isConnected();
    }
    
    /**
     * Gets the current connection state
     * 
     * @return The current connection state
     */
    public ConnectionState getConnectionState() {
        return connectionState.get();
    }
    
    /**
     * Gets the current configuration
     * 
     * @return The broker configuration, or null if not configured
     */
    public MqttBrokerConfig getConfig() {
        return config.get();
    }
    
    /**
     * Gets the number of reconnection attempts
     * 
     * @return The reconnection attempt count
     */
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }
    
    /**
     * Gets the broker URL from the current configuration
     * 
     * @return The broker URL, or null if not configured
     */
    public String getBrokerUrl() {
        MqttBrokerConfig cfg = config.get();
        return cfg != null ? cfg.getBrokerUrl() : null;
    }
    
    /**
     * Gets the module statistics
     * 
     * @return The statistics object
     */
    public ModuleStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Sets the connection state and logs the change
     */
    private void setConnectionState(ConnectionState newState) {
        ConnectionState oldState = connectionState.getAndSet(newState);
        if (oldState != newState) {
            logger.debug("Connection state changed: {} -> {}", 
                        oldState.getDisplayName(), newState.getDisplayName());
        }
    }
}
