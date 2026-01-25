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
 * - Initialization of MQTT publisher and tag subscription managers
 * - Registration of web resources and configuration pages
 */
public class MqttGatewayHook extends AbstractGatewayModuleHook {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttGatewayHook.class);
    
    private GatewayContext gatewayContext;
    private ModuleStatistics statistics;
    private MqttPublisherManager publisherManager;
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
        
        // Initialize MQTT publisher manager
        this.publisherManager = new MqttPublisherManager(statistics);
        
        // Initialize tag subscription manager
        this.tagSubscriptionManager = new TagSubscriptionManager(context, publisherManager, statistics);
        
        logger.info("Initialized MQTT Publisher Manager, Tag Subscription Manager, and Configuration Manager");
        
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
        
        // Try loading configuration from database first (preferred)
        MqttBrokerConfig config = configManager.loadBrokerConfigFromDatabase();
        
        // Fallback to JSON if database is empty (backward compatibility)
        if (config == null) {
            logger.info("No database configuration found, falling back to JSON file");
            config = configManager.loadConfig();
        }
        
        if (config != null) {
            try {
                config.validate();
                logger.info("Loaded MQTT configuration: {}", config.getBrokerUrl());
                publisherManager.connect(config);
            } catch (IllegalArgumentException e) {
                logger.warn("Loaded configuration is invalid: {}. Module will start but MQTT will not connect.", 
                           e.getMessage());
            }
        } else {
            logger.info("No MQTT configuration found. Module started but not connected to broker.");
            logger.info("Configure via Gateway web UI or by creating mqtt-uns-config.json in data directory");
        }
        
        // Try loading tag configuration from database first
        TagPublishConfig tagConfig = configManager.loadTagConfigFromDatabase();
        
        // Fallback to JSON if database is empty
        if (tagConfig == null) {
            tagConfig = configManager.loadTagConfig();
        }
        
        if (tagConfig != null && tagConfig.isEnabled()) {
            try {
                tagConfig.validate();
                logger.info("Starting tag subscriptions for {} providers and {} folders", 
                           tagConfig.getTagProviders().size(),
                           tagConfig.getTagFolders().size());
                tagSubscriptionManager.start(tagConfig);
            } catch (IllegalArgumentException e) {
                logger.warn("Tag publishing configuration is invalid: {}. Tag subscriptions will not start.", 
                           e.getMessage());
            }
        } else {
            logger.info("Tag publishing is not configured or disabled. No tags will be monitored.");
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
        
        // Shutdown MQTT connection
        if (publisherManager != null) {
            publisherManager.shutdown();
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
     * Gets the MQTT publisher manager
     * 
     * @return The publisher manager, or null if not initialized
     */
    public MqttPublisherManager getPublisherManager() {
        return publisherManager;
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
        if (statistics == null || publisherManager == null || tagSubscriptionManager == null) {
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
        
        ConnectionState connState = publisherManager.getConnectionState();
        double publishSuccessRate = statistics.getPublishSuccessRate();
        long messagesFailed = statistics.getMessagesFailedToPublish();
        
        if (connState == ConnectionState.CONNECTED && publishSuccessRate >= 95.0) {
            healthLevel = ModuleHealthStatus.HealthLevel.HEALTHY;
            statusMessage = "Module operating normally";
            healthy = true;
        } else if (connState == ConnectionState.RECONNECTING || 
                   (connState == ConnectionState.CONNECTED && publishSuccessRate >= 80.0)) {
            healthLevel = ModuleHealthStatus.HealthLevel.DEGRADED;
            statusMessage = String.format("Degraded: %s, %.1f%% success rate", 
                connState.getDisplayName(), publishSuccessRate);
            healthy = false;
        } else {
            healthLevel = ModuleHealthStatus.HealthLevel.UNHEALTHY;
            if (connState == ConnectionState.DISCONNECTED) {
                statusMessage = "MQTT broker not connected";
            } else if (connState == ConnectionState.ERROR) {
                statusMessage = String.format("Connection error after %d attempts", 
                    publisherManager.getReconnectAttempts());
            } else if (messagesFailed > 0 && publishSuccessRate < 80.0) {
                statusMessage = String.format("High failure rate: %.1f%% failures", 
                    100.0 - publishSuccessRate);
            } else {
                statusMessage = "Module unhealthy: " + connState.getDisplayName();
            }
            healthy = false;
        }
        
        return new ModuleHealthStatus.Builder()
            .healthy(healthy)
            .healthLevel(healthLevel)
            .mqttConnectionState(connState)
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
                    .addPage("settings", page -> page
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
     * reconnect to MQTT or restart tag subscriptions with new settings.
     */
    private void registerConfigurationListeners() {
        // Listen for broker config changes using RecordListenerAdapter
        brokerConfigListener = new RecordListenerAdapter<MqttBrokerConfigRecord>() {
            @Override
            public void recordAdded(MqttBrokerConfigRecord record) {
                logger.info("Broker configuration added: {}", record.getBrokerUrl());
                applyBrokerConfig(record);
            }
            
            @Override
            public void recordUpdated(MqttBrokerConfigRecord record) {
                logger.info("Broker configuration updated: {}", record.getBrokerUrl());
                applyBrokerConfig(record);
            }
            
            @Override
            public void recordDeleted(KeyValue key) {
                logger.info("Broker configuration deleted");
                publisherManager.disconnect();
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
     * Applies broker configuration changes by reconnecting to MQTT broker
     */
    private void applyBrokerConfig(MqttBrokerConfigRecord record) {
        try {
            if (!record.isEnabled()) {
                logger.info("Broker configuration is disabled, disconnecting");
                publisherManager.disconnect();
                return;
            }
            
            MqttBrokerConfig config = RecordMapper.toModel(record);
            config.validate();
            
            // Disconnect from old broker and connect to new one
            publisherManager.disconnect();
            publisherManager.connect(config);
            
            logger.info("Applied new broker configuration: {}", config.getBrokerUrl());
            
        } catch (IllegalArgumentException e) {
            logger.error("Cannot apply invalid broker configuration: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error applying broker configuration", e);
        }
    }
    
    /**
     * Applies tag configuration changes by restarting tag subscriptions
     */
    private void applyTagConfig(MqttTagConfigRecord record) {
        try {
            if (!record.isEnabled()) {
                logger.info("Tag configuration is disabled, stopping tag subscriptions");
                tagSubscriptionManager.shutdown();
                return;
            }
            
            TagPublishConfig config = RecordMapper.toModel(record);
            config.validate();
            
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
