package com.inductiveautomation.ignition.examples.mqtt.gateway.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttModuleConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttBrokerConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttTagConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.RecordMapper;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceInterface;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SQuery;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Configuration persistence using PersistentRecords (database storage).
 * Maintains backward compatibility with JSON file configuration for migration.
 * 
 * New behavior:
 * - Configuration stored in internal database (MqttBrokerConfigRecord, MqttTagConfigRecord)
 * - Automatic replication to redundant Gateways
 * - On first startup: auto-migrates JSON config to database if exists
 * - JSON files are deprecated but still loaded as fallback
 * 
 * Migration path:
 * 1. Module loads, checks database for config
 * 2. If database empty but JSON exists, migrate JSON → database
 * 3. Mark JSON file with ".migrated" extension
 * 4. Future loads use database only
 */
public class ConfigurationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String CONFIG_FILENAME = "mqtt-uns-config.json";
    private static final String MIGRATED_SUFFIX = ".migrated";
    private static final String DEFAULT_BROKER_NAME = "Default MQTT Broker";
    private static final String DEFAULT_TAG_CONFIG_NAME = "Default Tag Publishing";
    
    private final Gson gson;
    private final File configFile;
    private final GatewayContext context;
    private boolean migrationAttempted = false;
    
    public ConfigurationManager(GatewayContext context) {
        this.context = context;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        // Store config in Gateway data directory
        File dataDir = context.getSystemManager().getDataDir();
        this.configFile = new File(dataDir, CONFIG_FILENAME);
        
        logger.info("Configuration file (legacy): {}", configFile.getAbsolutePath());
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
    
    // ============================================================================
    // Database-backed Configuration Methods (NEW - Phase 5)
    // ============================================================================
    
    /**
     * Loads broker configuration from database.
     * If no database config exists, attempts to migrate from JSON file.
     * 
     * @return The loaded broker configuration, or null if none exists
     */
    public MqttBrokerConfig loadBrokerConfigFromDatabase() {
        try {
            PersistenceInterface db = context.getPersistenceInterface();
            
            // Query for broker config records
            SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
            List<MqttBrokerConfigRecord> records = db.query(query);
            
            if (!records.isEmpty()) {
                // Return first record (for now, we only support one broker config)
                MqttBrokerConfigRecord record = records.get(0);
                logger.debug("Loaded broker config from database: {}", record.getBrokerUrl());
                return RecordMapper.toModel(record);
            }
            
            // No database config found, try migration
            if (!migrationAttempted) {
                logger.info("No broker configuration in database, attempting migration from JSON...");
                return migrateJsonToDatabase();
            }
            
            logger.debug("No broker configuration found in database");
            return null;
            
        } catch (Exception e) {
            logger.error("Error loading broker configuration from database", e);
            return null;
        }
    }
    
    /**
     * Loads all broker configurations from database.
     * 
     * @return List of all broker configurations (may be empty)
     */
    public List<MqttBrokerConfig> loadAllBrokerConfigs() {
        java.util.List<MqttBrokerConfig> configs = new java.util.ArrayList<>();
        
        try {
            PersistenceInterface db = context.getPersistenceInterface();
            
            // Query for all broker config records
            SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
            List<MqttBrokerConfigRecord> records = db.query(query);
            
            for (MqttBrokerConfigRecord record : records) {
                try {
                    MqttBrokerConfig config = RecordMapper.toModel(record);
                    configs.add(config);
                    logger.debug("Loaded broker config from database: {} (ID: {})", 
                        config.getName(), config.getId());
                } catch (Exception e) {
                    logger.error("Error converting broker record to model: {}", e.getMessage());
                }
            }
            
            logger.debug("Loaded {} broker configuration(s) from database", configs.size());
            
        } catch (Exception e) {
            logger.error("Error loading broker configurations from database", e);
        }
        
        return configs;
    }
    
    /**
     * Loads tag configuration from database.
     * 
     * @return The loaded tag configuration, or null if none exists
     */
    public TagPublishConfig loadTagConfigFromDatabase() {
        try {
            PersistenceInterface db = context.getPersistenceInterface();
            
            // Query for tag config records
            SQuery<MqttTagConfigRecord> query = new SQuery<>(MqttTagConfigRecord.META);
            List<MqttTagConfigRecord> records = db.query(query);
            
            if (!records.isEmpty()) {
                // Return first enabled record
                for (MqttTagConfigRecord record : records) {
                    if (record.isEnabled()) {
                        logger.debug("Loaded tag config from database: {}", record.getName());
                        return RecordMapper.toModel(record);
                    }
                }
            }
            
            logger.debug("No enabled tag configuration found in database");
            return null;
            
        } catch (Exception e) {
            logger.error("Error loading tag configuration from database", e);
            return null;
        }
    }
    
    /**
     * Saves broker configuration to database.
     * Creates new record if none exists, updates existing record otherwise.
     * 
     * @param config The broker configuration to save
     * @return true if saved successfully, false otherwise
     */
    public boolean saveBrokerConfigToDatabase(MqttBrokerConfig config) {
        try {
            config.validate();
            
            PersistenceInterface db = context.getPersistenceInterface();
            
            // Check if record already exists
            SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
            List<MqttBrokerConfigRecord> records = db.query(query);
            
            MqttBrokerConfigRecord record;
            if (!records.isEmpty()) {
                // Update existing record
                record = records.get(0);
                logger.info("Updating existing broker configuration in database");
            } else {
                // Create new record
                record = db.createNew(MqttBrokerConfigRecord.META);
                logger.info("Creating new broker configuration in database");
            }
            
            // Populate record from model
            RecordMapper.fromModel(config, record);
            
            // Save to database
            db.save(record);
            
            logger.info("Saved broker configuration to database: {}", config.getBrokerUrl());
            return true;
            
        } catch (IllegalArgumentException e) {
            logger.error("Cannot save invalid broker configuration: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error saving broker configuration to database", e);
            return false;
        }
    }
    
    /**
     * Saves tag configuration to database.
     * Creates new record if none exists, updates existing record otherwise.
     * 
     * @param config The tag configuration to save
     * @return true if saved successfully, false otherwise
     */
    public boolean saveTagConfigToDatabase(TagPublishConfig config) {
        try {
            config.validate();
            
            PersistenceInterface db = context.getPersistenceInterface();
            
            // Check if record already exists
            SQuery<MqttTagConfigRecord> query = new SQuery<>(MqttTagConfigRecord.META);
            List<MqttTagConfigRecord> records = db.query(query);
            
            MqttTagConfigRecord record;
            if (!records.isEmpty()) {
                // Update existing record
                record = records.get(0);
                logger.info("Updating existing tag configuration in database");
            } else {
                // Create new record
                record = db.createNew(MqttTagConfigRecord.META);
                record.setName(DEFAULT_TAG_CONFIG_NAME);
                logger.info("Creating new tag configuration in database");
            }
            
            // Populate record from model
            RecordMapper.fromModel(config, record);
            
            // Save to database
            db.save(record);
            
            logger.info("Saved tag configuration to database");
            return true;
            
        } catch (IllegalArgumentException e) {
            logger.error("Cannot save invalid tag configuration: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error saving tag configuration to database", e);
            return false;
        }
    }
    
    /**
     * Migrates existing JSON configuration to database.
     * Marks the JSON file as migrated after successful migration.
     * 
     * @return The migrated broker configuration, or null if no JSON file exists
     */
    public MqttBrokerConfig migrateJsonToDatabase() {
        migrationAttempted = true;
        
        if (!configFile.exists()) {
            logger.info("No JSON configuration file to migrate");
            return null;
        }
        
        try {
            logger.info("Migrating JSON configuration to database...");
            
            // Load JSON configuration
            MqttModuleConfig moduleConfig = loadModuleConfig();
            
            if (moduleConfig == null) {
                logger.warn("Failed to load JSON configuration for migration");
                return null;
            }
            
            PersistenceInterface db = context.getPersistenceInterface();
            MqttBrokerConfig brokerConfig = null;
            
            // Migrate broker configuration
            if (moduleConfig.hasBrokerConfig()) {
                brokerConfig = moduleConfig.getBroker();
                
                MqttBrokerConfigRecord brokerRecord = db.createNew(MqttBrokerConfigRecord.META);
                RecordMapper.fromModel(brokerConfig, brokerRecord);
                db.save(brokerRecord);
                
                logger.info("Migrated broker configuration: {}", brokerConfig.getBrokerUrl());
            }
            
            // Migrate tag configuration
            if (moduleConfig.hasTagConfig()) {
                TagPublishConfig tagConfig = moduleConfig.getTags();
                
                MqttTagConfigRecord tagRecord = db.createNew(MqttTagConfigRecord.META);
                tagRecord.setName(DEFAULT_TAG_CONFIG_NAME);
                RecordMapper.fromModel(tagConfig, tagRecord);
                db.save(tagRecord);
                
                logger.info("Migrated tag configuration");
            }
            
            // Mark JSON file as migrated
            File migratedFile = new File(configFile.getAbsolutePath() + MIGRATED_SUFFIX);
            if (configFile.renameTo(migratedFile)) {
                logger.info("Marked JSON configuration file as migrated: {}", migratedFile.getName());
            } else {
                logger.warn("Failed to rename JSON file - manual cleanup may be required");
            }
            
            logger.info("Migration completed successfully");
            return brokerConfig;
            
        } catch (Exception e) {
            logger.error("Error during JSON to database migration", e);
            return null;
        }
    }
    
    /**
     * Checks if any configuration exists in the database
     * 
     * @return true if database has configuration, false otherwise
     */
    public boolean hasDatabaseConfig() {
        try {
            PersistenceInterface db = context.getPersistenceInterface();
            
            SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
            List<MqttBrokerConfigRecord> records = db.query(query);
            
            return !records.isEmpty();
            
        } catch (Exception e) {
            logger.error("Error checking for database configuration", e);
            return false;
        }
    }
    
    /**
     * Ensures a default configuration exists in the database.
     * Creates a default disabled broker config if none exists.
     */
    public void ensureDefaultDatabaseConfig() {
        try {
            if (!hasDatabaseConfig()) {
                logger.info("No database configuration found, creating default...");
                
                PersistenceInterface db = context.getPersistenceInterface();
                
                // Create default broker config (disabled)
                MqttBrokerConfigRecord brokerRecord = db.createNew(MqttBrokerConfigRecord.META);
                brokerRecord.setBrokerUrl("tcp://localhost:1883");
                brokerRecord.setClientId("ignition-mqtt-publisher");
                brokerRecord.setEnabled(false);
                brokerRecord.setQos(1);
                brokerRecord.setRetained(false);
                brokerRecord.setCleanSession(true);
                brokerRecord.setConnectionTimeout(30);
                brokerRecord.setKeepAliveInterval(60);
                brokerRecord.setSlowReconnectIntervalSeconds(600);
                brokerRecord.setUseTls(false);
                
                db.save(brokerRecord);
                
                logger.info("Created default broker configuration (disabled)");
            }
        } catch (Exception e) {
            logger.error("Error ensuring default database configuration", e);
        }
    }
}
