package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.config;

import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugPublishConfig;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records.SparkplugBrokerConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records.SparkplugPublishConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records.SparkplugRecordMapper;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceInterface;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SQuery;

import java.util.ArrayList;
import java.util.List;

public class SparkplugConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugConfigurationManager.class);
    private static final String DEFAULT_BROKER_NAME = "Default Sparkplug Broker";
    private static final String DEFAULT_PUBLISH_CONFIG_NAME = "Default Sparkplug Publish";

    private final GatewayContext context;

    public SparkplugConfigurationManager(GatewayContext context) {
        this.context = context;
    }

    public void ensureDefaultDatabaseConfig() {
        try {
            PersistenceInterface db = context.getPersistenceInterface();

            SQuery<SparkplugBrokerConfigRecord> brokerQuery = new SQuery<>(SparkplugBrokerConfigRecord.META);
            List<SparkplugBrokerConfigRecord> brokers = db.query(brokerQuery);

            Long brokerId;
            if (brokers.isEmpty()) {
                SparkplugBrokerConfigRecord brokerRecord = new SparkplugBrokerConfigRecord();
                brokerRecord.setName(DEFAULT_BROKER_NAME);
                db.save(brokerRecord);
                brokerId = brokerRecord.getId();
                logger.info("Created default Sparkplug broker configuration");
            } else {
                brokerId = brokers.get(0).getId();
            }

            SQuery<SparkplugPublishConfigRecord> publishQuery = new SQuery<>(SparkplugPublishConfigRecord.META);
            List<SparkplugPublishConfigRecord> publishConfigs = db.query(publishQuery);
            if (publishConfigs.isEmpty()) {
                SparkplugPublishConfigRecord publishRecord = new SparkplugPublishConfigRecord();
                publishRecord.setName(DEFAULT_PUBLISH_CONFIG_NAME);
                publishRecord.setBrokerConfigId(brokerId);
                db.save(publishRecord);
                logger.info("Created default Sparkplug publish configuration");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Sparkplug configuration", e);
        }
    }

    public List<MqttBrokerConfig> loadAllBrokerConfigs() {
        List<MqttBrokerConfig> configs = new ArrayList<>();
        try {
            PersistenceInterface db = context.getPersistenceInterface();
            SQuery<SparkplugBrokerConfigRecord> query = new SQuery<>(SparkplugBrokerConfigRecord.META);
            List<SparkplugBrokerConfigRecord> records = db.query(query);
            for (SparkplugBrokerConfigRecord record : records) {
                configs.add(SparkplugRecordMapper.toModel(record));
            }
        } catch (Exception e) {
            logger.error("Error loading Sparkplug broker configurations", e);
        }
        return configs;
    }

    public List<SparkplugPublishConfig> loadAllPublishConfigs() {
        List<SparkplugPublishConfig> configs = new ArrayList<>();
        try {
            PersistenceInterface db = context.getPersistenceInterface();
            SQuery<SparkplugPublishConfigRecord> query = new SQuery<>(SparkplugPublishConfigRecord.META);
            List<SparkplugPublishConfigRecord> records = db.query(query);
            for (SparkplugPublishConfigRecord record : records) {
                configs.add(SparkplugRecordMapper.toModel(record));
            }
        } catch (Exception e) {
            logger.error("Error loading Sparkplug publish configurations", e);
        }
        return configs;
    }

    public SparkplugPublishConfig savePublishConfig(SparkplugPublishConfig config) {
        config.validate();
        try {
            PersistenceInterface db = context.getPersistenceInterface();
            SparkplugPublishConfigRecord record = new SparkplugPublishConfigRecord();
            if (config.getId() != null) {
                record = db.find(SparkplugPublishConfigRecord.META, config.getId());
                if (record == null) {
                    record = new SparkplugPublishConfigRecord();
                }
            }
            SparkplugRecordMapper.updateRecord(record, config);
            db.save(record);
            return SparkplugRecordMapper.toModel(record);
        } catch (Exception e) {
            logger.error("Failed to save Sparkplug publish config", e);
            return null;
        }
    }
}
