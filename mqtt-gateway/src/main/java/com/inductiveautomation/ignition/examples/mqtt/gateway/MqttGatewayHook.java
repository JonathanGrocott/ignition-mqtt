package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.examples.mqtt.common.model.ConnectionState;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.examples.mqtt.gateway.config.ConfigurationManager;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttBrokerConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttTagConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.RecordMapper;
import com.inductiveautomation.ignition.examples.mqtt.gateway.web.MqttDataRoutes;
import com.inductiveautomation.ignition.gateway.dataroutes.AccessControlStrategy;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.localdb.persistence.IRecordListener;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordListenerAdapter;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.KeyValue;
import com.inductiveautomation.ignition.gateway.web.systemjs.SystemJsModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SQuery;

import java.sql.SQLException;
import java.util.Optional;

import static com.inductiveautomation.ignition.examples.mqtt.common.MqttModuleConstants.MODULE_ID;
import static com.inductiveautomation.ignition.examples.mqtt.common.MqttModuleConstants.MODULE_NAME;

/**
 * Gateway hook for the MQTT UNS Publisher module.
 * 
 * This is the entry point for the module on the Gateway scope. It handles:
 * - Module lifecycle (setup, startup, shutdown)
 * - Initialization of multi-broker MQTT publisher and tag subscription managers
 * - Registration of web resources and configuration pages
 * - Management of multiple MQTT broker connections
 */
public class MqttGatewayHook extends AbstractGatewayModuleHook {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttGatewayHook.class);
    
    private GatewayContext gatewayContext;
    private ModuleStatistics statistics;
    private MultiBrokerManager multiBrokerManager;
    private ConfigurationManager configManager;
    private TagSubscriptionManager tagSubscriptionManager;
    private IRecordListener<MqttBrokerConfigRecord> brokerConfigListener;
    private IRecordListener<MqttTagConfigRecord> tagConfigListener;
    
    /**
     * Called during Gateway startup to set up the module.
     * This is called before startup() and before the Gateway is fully initialized.
     * 
     * @param context The Gateway context providing access to Gateway services
     */
    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
        logger.info("Setting up {} module (ID: {})", MODULE_NAME, MODULE_ID);
        
        // Register PersistentRecords for database storage
        try {
            logger.info("Registering PersistentRecord schemas...");
            context.getSchemaUpdater().updatePersistentRecords(
                MqttBrokerConfigRecord.META,
                MqttTagConfigRecord.META
            );
            logger.info("Successfully registered PersistentRecord schemas");
        } catch (SQLException e) {
            logger.error("Error registering PersistentRecord schemas", e);
        }
        
        // Initialize statistics tracker
        this.statistics = new ModuleStatistics();
        
        // Initialize configuration manager
        this.configManager = new ConfigurationManager(context);
        
        // Ensure default configuration exists in database (creates if needed)
        configManager.ensureDefaultDatabaseConfig();
        
        // Initialize multi-broker manager
        this.multiBrokerManager = new MultiBrokerManager(statistics);
        
        // Initialize tag subscription manager with multi-broker support
        this.tagSubscriptionManager = new TagSubscriptionManager(context, multiBrokerManager, statistics);
        
        logger.info("Initialized Multi-Broker Manager, Tag Subscription Manager, and Configuration Manager");
        
        // Register listeners for configuration changes
        registerConfigurationListeners();
        
        logger.info("Module setup complete - web UI and REST API routes will be mounted during startup");
    }
    
    /**
     * Called when the module should start up.
     * This is called after the Gateway has fully initialized.
     * 
     * @param activationState The current license state
     */
    @Override
    public void startup(LicenseState activationState) {
        logger.info("Starting up {} module", MODULE_NAME);
        
        // Register web UI page in Gateway navigation (must be done during startup, not setup)
        registerWebUI();
        
        // Load all broker configurations from database
        java.util.List<MqttBrokerConfig> brokerConfigs = configManager.loadAllBrokerConfigs();
        logger.info("Loaded {} broker configuration(s) from database", brokerConfigs.size());
        
        // Load tag configuration to determine which brokers need connections
        TagPublishConfig tagConfig = configManager.loadTagConfigFromDatabase();
        
        if (tagConfig == null) {
            logger.info("No tag configuration found. Module started but no brokers will connect.");
            logger.info("Configure via Gateway web UI");
            return;
        }
        
        // Determine which brokers have enabled topic mappings
        java.util.Set<Long> brokersInUse = new java.util.HashSet<>();
        if (tagConfig.getTopicMappings() != null) {
            for (com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping : tagConfig.getTopicMappings()) {
                if (mapping.isEnabled() && mapping.getBrokerId() != null) {
                    brokersInUse.add(mapping.getBrokerId());
                }
            }
        }
        
        logger.info("Found {} broker(s) with enabled topic mappings", brokersInUse.size());
        
        // Connect only brokers that have enabled mappings
        for (MqttBrokerConfig brokerConfig : brokerConfigs) {
            if (brokerConfig.getId() == null) {
                logger.warn("Skipping broker with null ID: {}", brokerConfig.getName());
                continue;
            }
            
            if (brokersInUse.contains(brokerConfig.getId())) {
                try {
                    brokerConfig.validate();
                    logger.info("Connecting to broker: {} (ID: {}, URL: {})", 
                        brokerConfig.getName(), brokerConfig.getId(), brokerConfig.getBrokerUrl());
                    multiBrokerManager.connectBroker(brokerConfig.getId(), brokerConfig);
                } catch (IllegalArgumentException e) {
                    logger.warn("Broker configuration is invalid: {}. Broker will not connect: {}", 
                        e.getMessage(), brokerConfig.getName());
                }
            } else {
                logger.info("Broker {} (ID: {}) has no enabled mappings, not connecting", 
                    brokerConfig.getName(), brokerConfig.getId());
            }
        }
        
        // Start tag subscriptions
        if (tagConfig.isEnabled()) {
            try {
                tagConfig.validate();
                logger.info("Starting tag subscriptions with {} topic mapping(s)", 
                    tagConfig.getTopicMappings() != null ? tagConfig.getTopicMappings().size() : 0);
                tagSubscriptionManager.start(tagConfig);
            } catch (IllegalArgumentException e) {
                logger.warn("Tag publishing configuration is invalid: {}. Tag subscriptions will not start.", 
                    e.getMessage());
            }
        } else {
            logger.info("Tag publishing is disabled. No tags will be monitored.");
        }
        
        logger.info("{} module started successfully", MODULE_NAME);
    }
    
    /**
     * Called when the module should shut down.
     * This is called during Gateway shutdown or when the module is being removed.
     */
    @Override
    public void shutdown() {
        logger.info("Shutting down {} module", MODULE_NAME);
        
        // Log final statistics
        if (statistics != null) {
            logger.info("Final statistics:\n{}", statistics.getDetailedReport());
        }
        
        // Unregister configuration listeners
        if (brokerConfigListener != null) {
            MqttBrokerConfigRecord.META.removeRecordListener(brokerConfigListener);
        }
        if (tagConfigListener != null) {
            MqttTagConfigRecord.META.removeRecordListener(tagConfigListener);
        }
        
        // Stop tag subscriptions
        if (tagSubscriptionManager != null) {
            tagSubscriptionManager.shutdown();
        }
        
        // Shutdown all MQTT broker connections
        if (multiBrokerManager != null) {
            multiBrokerManager.shutdown();
        }
        
        logger.info("{} module shut down successfully", MODULE_NAME);
    }
    
    /**
     * Indicates whether this module is a "free" module, meaning it does not 
     * participate in the licensing system.
     * 
     * @return true if this is a free module
     */
    @Override
    public boolean isFreeModule() {
        return true;
    }
    
    /**
     * Mounts REST API routes for the web UI.
     * These routes handle configuration CRUD and status monitoring.
     * Routes are open to authenticated Gateway users.
     * 
     * NOTE: This method is called BEFORE setup() in the module lifecycle,
     * so we pass 'this' hook instance and route handlers will fetch
     * context/managers lazily when requests are handled.
     */
    @Override
    public void mountRouteHandlers(RouteGroup routes) {
        MqttDataRoutes.mountRoutes(routes, this);
    }
    
    /**
     * Returns the folder containing static resources to be mounted.
     * These files will be served at /res/{module-id}/
     * 
     * @return The folder name within src/main/resources/
     */
    @Override
    public Optional<String> getMountedResourceFolder() {
        return Optional.of("mounted");
    }
    
    /**
     * Returns an alias for the resource mount path.
     * Instead of /res/com.inductiveautomation.ignition.examples.mqtt-gateway/
     * Use the shorter /res/mqtt-uns-publisher/
     * 
     * @return The mount path alias
     */
    @Override
    public Optional<String> getMountPathAlias() {
        return Optional.of("mqtt-uns-publisher");
    }
    
    /**
     * Gets the module statistics
     * 
     * @return The statistics object, or null if not initialized
     */
    public ModuleStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Gets the multi-broker manager
     * 
     * @return The multi-broker manager, or null if not initialized
     */
    public MultiBrokerManager getMultiBrokerManager() {
        return multiBrokerManager;
    }
    
    /**
     * Gets the MQTT publisher manager (deprecated - use getMultiBrokerManager)
     * 
     * @return The publisher manager, or null
     * @deprecated Use getMultiBrokerManager() instead for multi-broker support
     */
    @Deprecated
    public MqttPublisherManager getPublisherManager() {
        // For backward compatibility with MqttStatusRoute
        // Return the first active publisher if any exist
        if (multiBrokerManager != null && multiBrokerManager.getActiveBrokerCount() > 0) {
            java.util.Set<Long> brokerIds = multiBrokerManager.getActiveBrokerIds();
            if (!brokerIds.isEmpty()) {
                return multiBrokerManager.getPublisher(brokerIds.iterator().next());
            }
        }
        return null;
    }
    
    /**
     * Gets the tag subscription manager
     * 
     * @return The tag subscription manager, or null if not initialized
     */
    public TagSubscriptionManager getTagSubscriptionManager() {
        return tagSubscriptionManager;
    }
    
    /**
     * Gets the configuration manager
     * 
     * @return The configuration manager, or null if not initialized
     */
    public ConfigurationManager getConfigurationManager() {
        return configManager;
    }
    
    /**
     * Gets the gateway context
     * 
     * @return The gateway context, or null if not initialized
     */
    public GatewayContext getGatewayContext() {
        return gatewayContext;
    }
    
    /**
     * Performs a health check on the module
     * 
     * @return The current health status
     */
    public ModuleHealthStatus getHealthStatus() {
        if (statistics == null || multiBrokerManager == null || tagSubscriptionManager == null) {
            return new ModuleHealthStatus.Builder()
                .healthy(false)
                .healthLevel(ModuleHealthStatus.HealthLevel.UNHEALTHY)
                .statusMessage("Module not initialized")
                .build();
        }
        
        // Determine health level based on various factors
        ModuleHealthStatus.HealthLevel healthLevel;
        String statusMessage;
        boolean healthy;
        
        // Get total available brokers from database
        int totalBrokers = 0;
        try {
            totalBrokers = configManager.loadAllBrokerConfigs().size();
        } catch (Exception e) {
            logger.warn("Error loading broker count", e);
        }
        
        // For multi-broker, check overall status
        int activeBrokers = multiBrokerManager.getActiveBrokerCount();
        int connectedBrokers = 0;
        ConnectionState overallState = ConnectionState.DISCONNECTED;
        
        for (Long brokerId : multiBrokerManager.getActiveBrokerIds()) {
            if (multiBrokerManager.isConnected(brokerId)) {
                connectedBrokers++;
                overallState = ConnectionState.CONNECTED;
            } else {
                ConnectionState brokerState = multiBrokerManager.getConnectionState(brokerId);
                if (brokerState == ConnectionState.RECONNECTING && overallState != ConnectionState.CONNECTED) {
                    overallState = ConnectionState.RECONNECTING;
                } else if (brokerState == ConnectionState.ERROR && overallState == ConnectionState.DISCONNECTED) {
                    overallState = ConnectionState.ERROR;
                }
            }
        }
        
        double publishSuccessRate = statistics.getPublishSuccessRate();
        long messagesFailed = statistics.getMessagesFailedToPublish();
        
        // Determine health based on broker status
        if (totalBrokers == 0) {
            healthLevel = ModuleHealthStatus.HealthLevel.HEALTHY;
            statusMessage = "No brokers configured - add brokers in Broker Settings";
            healthy = true;
        } else if (activeBrokers == 0) {
            // Brokers exist but none are in use (no topic mappings)
            healthLevel = ModuleHealthStatus.HealthLevel.HEALTHY;
            statusMessage = String.format("%d broker(s) available - add topic mappings to activate", totalBrokers);
            healthy = true;
        } else if (connectedBrokers == activeBrokers && publishSuccessRate >= 95.0) {
            healthLevel = ModuleHealthStatus.HealthLevel.HEALTHY;
            statusMessage = String.format("All %d broker(s) connected, operating normally", connectedBrokers);
            healthy = true;
        } else if (connectedBrokers > 0 || 
                   (overallState == ConnectionState.RECONNECTING) || 
                   (overallState == ConnectionState.CONNECTED && publishSuccessRate >= 80.0)) {
            healthLevel = ModuleHealthStatus.HealthLevel.DEGRADED;
            statusMessage = String.format("Degraded: %d/%d brokers connected, %.1f%% success rate", 
                connectedBrokers, activeBrokers, publishSuccessRate);
            healthy = false;
        } else {
            healthLevel = ModuleHealthStatus.HealthLevel.UNHEALTHY;
            if (connectedBrokers == 0 && activeBrokers > 0) {
                statusMessage = String.format("No brokers connected (%d in use, %d available)", activeBrokers, totalBrokers);
            } else if (messagesFailed > 0 && publishSuccessRate < 80.0) {
                statusMessage = String.format("High failure rate: %.1f%% failures", 
                    100.0 - publishSuccessRate);
            } else {
                statusMessage = String.format("Module unhealthy: %d/%d brokers connected", 
                    connectedBrokers, activeBrokers);
            }
            healthy = false;
        }
        
        return new ModuleHealthStatus.Builder()
            .healthy(healthy)
            .healthLevel(healthLevel)
            .mqttConnectionState(overallState)
            .monitoredTagCount(tagSubscriptionManager.getMonitoredTagCount())
            .messagesPublished(statistics.getMessagesPublished())
            .messagesFailed(statistics.getMessagesFailedToPublish())
            .uptimeMs(statistics.getUptimeMs())
            .statusMessage(statusMessage)
            .build();
    }
    
    /**
     * Registers the Web UI page in the Gateway navigation.
     * Adds a page to the Connections section that loads the React application.
     */
    private void registerWebUI() {
        try {
            // Files in src/main/resources/mounted/ are automatically served at /res/mqtt-uns-publisher/
            // (using the mount path alias defined in getMountPathAlias())
            
            // Create SystemJs module pointing to the JavaScript file
            // First param is a unique identifier (not the AMD module name)
            SystemJsModule jsModule = new SystemJsModule(
                "com.inductiveautomation.mqtt.uns.gateway",     // Unique module identifier
                "/res/mqtt-uns-publisher/mqtt-config.js"       // Web path (uses mount alias)
            );
            
            logger.info("Registered SystemJS module with ID: com.inductiveautomation.mqtt.uns.gateway");
            logger.info("Module script path: /res/mqtt-uns-publisher/mqtt-config.js");
            
            // Add page to Gateway navigation (Connections section)
            // Use addPage() pattern like MQTT Transmission module
            gatewayContext.getWebResourceManager()
                .getNavigationModel()
                .getConnections()
                .addCategory("mqtt-uns-publisher", cat -> cat
                    .label("MQTT UNS Publisher")
                    .position(100)
                    .addPage("configuration", page -> page
                        .position(10)
                        .mount("/mqtt-uns-publisher", "Configuration", jsModule)
                    )
                );
            
            logger.info("Registered MQTT UNS Publisher web UI in Gateway navigation");
            
        } catch (Exception e) {
            logger.error("Error registering web UI", e);
        }
    }
    
    /**
     * Registers listeners to respond to configuration changes in the database.
     * When configuration is updated via web UI, these listeners will automatically
     * reconnect to MQTT brokers or restart tag subscriptions with new settings.
     */
    private void registerConfigurationListeners() {
        // Listen for broker config changes using RecordListenerAdapter
        brokerConfigListener = new RecordListenerAdapter<MqttBrokerConfigRecord>() {
            @Override
            public void recordAdded(MqttBrokerConfigRecord record) {
                logger.info("Broker configuration added: {} (ID: {})", record.getName(), record.getId());
                applyBrokerConfig(record);
            }
            
            @Override
            public void recordUpdated(MqttBrokerConfigRecord record) {
                // Check if record is deleted before accessing it
                if (record.isDeleted()) {
                    logger.info("Broker configuration marked for deletion, skipping update");
                    return;
                }
                logger.info("Broker configuration updated: {} (ID: {})", record.getName(), record.getId());
                applyBrokerConfig(record);
            }
            
            @Override
            public void recordDeleted(KeyValue key) {
                logger.info("Broker configuration deleted: {}", key);
                // Note: We don't try to extract broker ID from the key because the record
                // is already deleted. The broker will be removed on the next tag config 
                // change or module restart when we reload broker configs from database.
                // For immediate removal, the DELETE endpoint handles calling removeBroker().
            }
        };
        
        // Listen for tag config changes
        tagConfigListener = new RecordListenerAdapter<MqttTagConfigRecord>() {
            @Override
            public void recordAdded(MqttTagConfigRecord record) {
                logger.info("Tag configuration added: {}", record.getName());
                applyTagConfig(record);
            }
            
            @Override
            public void recordUpdated(MqttTagConfigRecord record) {
                logger.info("Tag configuration updated: {}", record.getName());
                applyTagConfig(record);
            }
            
            @Override
            public void recordDeleted(KeyValue key) {
                logger.info("Tag configuration deleted");
                tagSubscriptionManager.shutdown();
            }
        };
        
        // Register listeners with the record metadata
        MqttBrokerConfigRecord.META.addRecordListener(brokerConfigListener);
        MqttTagConfigRecord.META.addRecordListener(tagConfigListener);
        
        logger.info("Registered configuration change listeners");
    }
    
    /**
     * Applies broker configuration changes by connecting/disconnecting/updating broker
     */
    private void applyBrokerConfig(MqttBrokerConfigRecord record) {
        try {
            Long brokerId = record.getId();
            if (brokerId == null) {
                logger.warn("Cannot apply broker config: ID is null");
                return;
            }
            
            if (!record.isEnabled()) {
                logger.info("Broker {} is disabled, disconnecting", record.getName());
                multiBrokerManager.disconnectBroker(brokerId);
                return;
            }
            
            MqttBrokerConfig config = RecordMapper.toModel(record);
            config.validate();
            
            // Note: We don't automatically connect brokers when they're added/updated
            // They only connect when there are enabled topic mappings that use them
            // This is handled by applyTagConfig which checks all mappings
            
            logger.info("Broker configuration updated: {} (ID: {})", config.getName(), brokerId);
            
            // If this broker is currently connected, reconnect with new settings
            if (multiBrokerManager.isConnected(brokerId)) {
                logger.info("Reconnecting broker {} with new configuration", config.getName());
                multiBrokerManager.connectBroker(brokerId, config);
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Cannot apply invalid broker configuration: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error applying broker configuration", e);
        }
    }
    
    /**
     * Applies tag configuration changes by restarting tag subscriptions
     * and connecting/disconnecting brokers based on enabled mappings
     */
    private void applyTagConfig(MqttTagConfigRecord record) {
        try {
            if (!record.isEnabled()) {
                logger.info("Tag configuration is disabled, stopping tag subscriptions and disconnecting all brokers");
                tagSubscriptionManager.shutdown();
                multiBrokerManager.disconnectAll();
                return;
            }
            
            TagPublishConfig config = RecordMapper.toModel(record);
            config.validate();
            
            // Determine which brokers are needed based on enabled topic mappings
            java.util.Set<Long> brokersNeeded = new java.util.HashSet<>();
            if (config.getTopicMappings() != null) {
                for (com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping : config.getTopicMappings()) {
                    if (mapping.isEnabled() && mapping.getBrokerId() != null) {
                        brokersNeeded.add(mapping.getBrokerId());
                    }
                }
            }
            
            logger.info("Tag config requires {} broker(s)", brokersNeeded.size());
            
            // Disconnect brokers that are no longer needed
            java.util.Set<Long> currentBrokers = multiBrokerManager.getActiveBrokerIds();
            for (Long brokerId : currentBrokers) {
                if (!brokersNeeded.contains(brokerId)) {
                    logger.info("Disconnecting broker {} (no longer needed)", brokerId);
                    multiBrokerManager.disconnectBroker(brokerId);
                }
            }
            
            // Connect brokers that are now needed
            for (Long brokerId : brokersNeeded) {
                if (!multiBrokerManager.isConnected(brokerId)) {
                    // Load broker config from database
                    try {
                        MqttBrokerConfigRecord brokerRecord = gatewayContext
                            .getPersistenceInterface()
                            .find(MqttBrokerConfigRecord.META, brokerId);
                        
                        if (brokerRecord != null && brokerRecord.isEnabled()) {
                            MqttBrokerConfig brokerConfig = RecordMapper.toModel(brokerRecord);
                            brokerConfig.validate();
                            logger.info("Connecting broker {} for tag publishing", brokerConfig.getName());
                            multiBrokerManager.connectBroker(brokerId, brokerConfig);
                        } else {
                            logger.warn("Broker {} not found or disabled, cannot connect", brokerId);
                        }
                    } catch (Exception e) {
                        logger.error("Error loading/connecting broker {}: {}", brokerId, e.getMessage());
                    }
                }
            }
            
            // Stop old subscriptions and start new ones
            tagSubscriptionManager.shutdown();
            tagSubscriptionManager.start(config);
            
            logger.info("Applied new tag configuration: {}", record.getName());
            
        } catch (IllegalArgumentException e) {
            logger.error("Cannot apply invalid tag configuration: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error applying tag configuration", e);
        }
    }
}
