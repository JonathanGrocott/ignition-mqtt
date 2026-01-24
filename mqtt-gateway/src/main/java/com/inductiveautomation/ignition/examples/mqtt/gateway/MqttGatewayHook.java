package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
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
    private MqttPublisherManager publisherManager;
    private ConfigurationManager configManager;
    
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
        
        // Initialize configuration manager
        this.configManager = new ConfigurationManager(context);
        
        // Initialize MQTT publisher manager
        this.publisherManager = new MqttPublisherManager();
        
        logger.info("Initialized MQTT Publisher Manager and Configuration Manager");
        
        // TODO: Register web routes (Phase 5)
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
        
        // TODO: Start tag subscriptions (Phase 3)
        
        logger.info("{} module started successfully", MODULE_NAME);
    }
    
    /**
     * Called when the module should shut down.
     * This is called during Gateway shutdown or when the module is being removed.
     */
    @Override
    public void shutdown() {
        logger.info("Shutting down {} module", MODULE_NAME);
        
        // TODO: Stop tag subscriptions (Phase 3)
        
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
}
