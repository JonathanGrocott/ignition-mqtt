package com.inductiveautomation.ignition.examples.mqtt.gateway.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttModuleConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Simple configuration persistence using JSON files.
 * This is a temporary solution for Phase 2-3. Will be replaced with 
 * Config Resource API in Phase 5 when we add the web UI.
 * 
 * Manages both MQTT broker configuration and tag publishing configuration.
 */
public class ConfigurationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String CONFIG_FILENAME = "mqtt-uns-config.json";
    
    private final Gson gson;
    private final File configFile;
    
    public ConfigurationManager(GatewayContext context) {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        // Store config in Gateway data directory
        File dataDir = context.getSystemManager().getDataDir();
        this.configFile = new File(dataDir, CONFIG_FILENAME);
        
        logger.info("Configuration file: {}", configFile.getAbsolutePath());
    }
    
    /**
     * Loads the complete module configuration from file
     * 
     * @return The loaded configuration, or a default configuration if file doesn't exist
     */
    public MqttModuleConfig loadModuleConfig() {
        if (!configFile.exists()) {
            logger.info("Configuration file does not exist, returning default configuration");
            return new MqttModuleConfig();
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            MqttModuleConfig config = gson.fromJson(reader, MqttModuleConfig.class);
            logger.info("Loaded module configuration from file");
            return config;
        } catch (IOException e) {
            logger.error("Failed to load configuration from file", e);
            return new MqttModuleConfig();
        }
    }
    
    /**
     * Loads the MQTT broker configuration from file (legacy method for backwards compatibility)
     * 
     * @return The loaded broker configuration, or a default configuration if file doesn't exist
     */
    public MqttBrokerConfig loadConfig() {
        MqttModuleConfig moduleConfig = loadModuleConfig();
        if (moduleConfig.hasBrokerConfig()) {
            return moduleConfig.getBroker();
        }
        
        logger.info("No broker configuration found, returning default");
        return new MqttBrokerConfig();
    }
    
    /**
     * Loads the tag publishing configuration from file
     * 
     * @return The loaded tag configuration, or null if not configured
     */
    public TagPublishConfig loadTagConfig() {
        MqttModuleConfig moduleConfig = loadModuleConfig();
        if (moduleConfig.hasTagConfig()) {
            return moduleConfig.getTags();
        }
        
        logger.info("No tag publishing configuration found");
        return null;
    }
    
    /**
     * Saves the complete module configuration to file
     * 
     * @param config The configuration to save
     * @return true if saved successfully, false otherwise
     */
    public boolean saveModuleConfig(MqttModuleConfig config) {
        try {
            config.validate();
        } catch (IllegalArgumentException e) {
            logger.error("Cannot save invalid configuration: {}", e.getMessage());
            return false;
        }
        
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
            logger.info("Saved module configuration to file");
            return true;
        } catch (IOException e) {
            logger.error("Failed to save configuration to file", e);
            return false;
        }
    }
    
    /**
     * Saves the MQTT broker configuration to file (legacy method - updates broker config only)
     * 
     * @param config The broker configuration to save
     * @return true if saved successfully, false otherwise
     */
    public boolean saveConfig(MqttBrokerConfig config) {
        // Load existing module config and update broker settings
        MqttModuleConfig moduleConfig = loadModuleConfig();
        moduleConfig.setBroker(config);
        return saveModuleConfig(moduleConfig);
    }
    
    /**
     * Checks if a configuration file exists
     * 
     * @return true if configuration exists, false otherwise
     */
    public boolean configExists() {
        return configFile.exists();
    }
    
    /**
     * Deletes the configuration file
     * 
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteConfig() {
        if (configFile.exists()) {
            boolean deleted = configFile.delete();
            if (deleted) {
                logger.info("Deleted configuration file");
            } else {
                logger.warn("Failed to delete configuration file");
            }
            return deleted;
        }
        return true;
    }
}
