package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.examples.mqtt.common.model.ConnectionState;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple MQTT broker connections for multi-broker publishing.
 * 
 * This class handles:
 * - Managing multiple MqttPublisherManager instances (one per broker)
 * - Connecting/disconnecting brokers by ID
 * - Routing publish calls to specific brokers
 * - Tracking connection state for each broker
 * - Thread-safe operations
 */
public class MultiBrokerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiBrokerManager.class);
    
    private final ModuleStatistics statistics;
    private final Map<Long, MqttPublisherManager> publishers = new ConcurrentHashMap<>();
    private final Map<Long, MqttBrokerConfig> brokerConfigs = new ConcurrentHashMap<>();
    
    /**
     * Initializes the multi-broker manager
     * 
     * @param statistics Module statistics tracker
     */
    public MultiBrokerManager(ModuleStatistics statistics) {
        this.statistics = statistics;
        logger.info("MultiBrokerManager initialized");
    }
    
    /**
     * Connects to a broker by ID with the given configuration
     * 
     * @param brokerId The broker ID
     * @param config The broker configuration
     */
    public synchronized void connectBroker(Long brokerId, MqttBrokerConfig config) {
        if (brokerId == null) {
            logger.warn("Cannot connect broker: brokerId is null");
            return;
        }
        
        if (config == null) {
            logger.warn("Cannot connect broker {}: config is null", brokerId);
            return;
        }
        
        logger.info("Connecting broker {} '{}' with client ID '{}' to {}", 
            brokerId, config.getName(), config.getClientId(), config.getBrokerUrl());
        
        // Store config
        brokerConfigs.put(brokerId, config);
        
        // Check if publisher already exists
        MqttPublisherManager publisher = publishers.get(brokerId);
        
        if (publisher != null) {
            // Disconnect existing and reconnect with new config
            logger.info("Broker {} already exists, reconnecting with new config", brokerId);
            publisher.disconnect();
            publisher.connect(config);
        } else {
            // Create new publisher
            publisher = new MqttPublisherManager(statistics);
            publishers.put(brokerId, publisher);
            publisher.connect(config);
        }
        
        logger.info("Broker {} '{}' connection initiated (client ID: '{}')", 
            brokerId, config.getName(), config.getClientId());
    }
    
    /**
     * Disconnects a broker by ID
     * 
     * @param brokerId The broker ID
     */
    public synchronized void disconnectBroker(Long brokerId) {
        if (brokerId == null) {
            logger.warn("Cannot disconnect broker: brokerId is null");
            return;
        }
        
        logger.info("Disconnecting broker {}", brokerId);
        
        MqttPublisherManager publisher = publishers.get(brokerId);
        if (publisher != null) {
            publisher.disconnect();
            logger.info("Broker {} disconnected", brokerId);
        } else {
            logger.debug("Broker {} not found in active publishers", brokerId);
        }
        
        brokerConfigs.remove(brokerId);
    }
    
    /**
     * Removes a broker and shuts down its publisher
     * 
     * @param brokerId The broker ID
     */
    public synchronized void removeBroker(Long brokerId) {
        if (brokerId == null) {
            logger.warn("Cannot remove broker: brokerId is null");
            return;
        }
        
        logger.info("Removing broker {}", brokerId);
        
        MqttPublisherManager publisher = publishers.remove(brokerId);
        if (publisher != null) {
            publisher.shutdown();
            logger.info("Broker {} removed and shut down", brokerId);
        }
        
        brokerConfigs.remove(brokerId);
    }
    
    /**
     * Publishes a message to a specific broker
     * 
     * @param brokerId The broker ID
     * @param topic The MQTT topic
     * @param payload The message payload
     * @return true if published successfully, false otherwise
     */
    public boolean publish(Long brokerId, String topic, String payload) {
        if (brokerId == null) {
            logger.warn("Cannot publish: brokerId is null for topic '{}'", topic);
            return false;
        }
        
        MqttPublisherManager publisher = publishers.get(brokerId);
        if (publisher == null) {
            logger.warn("Cannot publish to topic '{}': broker {} not found (have {} publishers)", 
                topic, brokerId, publishers.size());
            logger.warn("Available broker IDs: {}", publishers.keySet());
            statistics.incrementMessagesFailedToPublish();
            return false;
        }
        
        logger.info("Forwarding publish to broker {}: topic={}", brokerId, topic);
        return publisher.publish(topic, payload);
    }
    
    /**
     * Gets the connection state for a specific broker
     * 
     * @param brokerId The broker ID
     * @return The connection state, or DISCONNECTED if broker not found
     */
    public ConnectionState getConnectionState(Long brokerId) {
        if (brokerId == null) {
            return ConnectionState.DISCONNECTED;
        }
        
        MqttPublisherManager publisher = publishers.get(brokerId);
        if (publisher == null) {
            return ConnectionState.DISCONNECTED;
        }
        
        return publisher.getConnectionState();
    }
    
    /**
     * Checks if a specific broker is connected
     * 
     * @param brokerId The broker ID
     * @return true if connected, false otherwise
     */
    public boolean isConnected(Long brokerId) {
        if (brokerId == null) {
            return false;
        }
        
        MqttPublisherManager publisher = publishers.get(brokerId);
        return publisher != null && publisher.isConnected();
    }
    
    /**
     * Gets the configuration for a specific broker
     * 
     * @param brokerId The broker ID
     * @return The broker configuration, or null if not found
     */
    public MqttBrokerConfig getBrokerConfig(Long brokerId) {
        return brokerConfigs.get(brokerId);
    }
    
    /**
     * Gets all active broker IDs
     * 
     * @return Set of active broker IDs
     */
    public Set<Long> getActiveBrokerIds() {
        return publishers.keySet();
    }
    
    /**
     * Gets the number of active brokers
     * 
     * @return The count of active brokers
     */
    public int getActiveBrokerCount() {
        return publishers.size();
    }
    
    /**
     * Gets the publisher manager for a specific broker (for advanced use)
     * 
     * @param brokerId The broker ID
     * @return The publisher manager, or null if not found
     */
    public MqttPublisherManager getPublisher(Long brokerId) {
        return publishers.get(brokerId);
    }
    
    /**
     * Disconnects all brokers
     */
    public synchronized void disconnectAll() {
        logger.info("Disconnecting all brokers");
        
        for (Map.Entry<Long, MqttPublisherManager> entry : publishers.entrySet()) {
            logger.info("Disconnecting broker {}", entry.getKey());
            entry.getValue().disconnect();
        }
        
        logger.info("All brokers disconnected");
    }
    
    /**
     * Shuts down all brokers and releases resources
     */
    public synchronized void shutdown() {
        logger.info("Shutting down MultiBrokerManager");
        
        for (Map.Entry<Long, MqttPublisherManager> entry : publishers.entrySet()) {
            logger.info("Shutting down broker {}", entry.getKey());
            entry.getValue().shutdown();
        }
        
        publishers.clear();
        brokerConfigs.clear();
        
        logger.info("MultiBrokerManager shut down complete");
    }
}
