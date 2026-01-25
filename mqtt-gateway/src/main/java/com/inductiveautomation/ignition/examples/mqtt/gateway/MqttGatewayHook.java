package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.examples.mqtt.common.model.ConnectionState;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.examples.mqtt.gateway.config.ConfigurationManager;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        
        // Initialize statistics tracker
        this.statistics = new ModuleStatistics();
        
        // Initialize configuration manager
        this.configManager = new ConfigurationManager(context);
        
        // Initialize MQTT publisher manager
        this.publisherManager = new MqttPublisherManager(statistics);
        
        // Initialize tag subscription manager
        this.tagSubscriptionManager = new TagSubscriptionManager(context, publisherManager, statistics);
        
        logger.info("Initialized MQTT Publisher Manager, Tag Subscription Manager, and Configuration Manager");
        
        // TODO: Register web routes (Phase 5 - requires Perspective or WebDev module example study)
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
        
        // Load configuration and start MQTT connection
        MqttBrokerConfig config = configManager.loadConfig();
        
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
            logger.info("You can configure the module by editing: {}", 
                       configManager.configExists() ? "existing config" : "creating mqtt-uns-config.json in data directory");
        }
        
        // Load tag publishing configuration and start subscriptions
        TagPublishConfig tagConfig = configManager.loadTagConfig();
        
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
}
