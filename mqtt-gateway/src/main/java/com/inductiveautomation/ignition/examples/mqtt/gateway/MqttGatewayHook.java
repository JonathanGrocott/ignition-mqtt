package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
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
        
        // TODO: Register configuration resources
        // TODO: Register web routes
        // TODO: Initialize managers (will be created in Phase 2 and 3)
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
        
        // TODO: Start MQTT connection (Phase 2)
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
        // TODO: Disconnect MQTT client (Phase 2)
        // TODO: Clean up resources
        
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
