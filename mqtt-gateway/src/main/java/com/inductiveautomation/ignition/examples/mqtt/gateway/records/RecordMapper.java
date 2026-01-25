package com.inductiveautomation.ignition.examples.mqtt.gateway.records;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting between PersistentRecords and model POJOs.
 * This maintains backward compatibility while transitioning to database storage.
 */
public class RecordMapper {
    
    private static final Gson gson = new Gson();
    
    /**
     * Converts MqttBrokerConfigRecord to MqttBrokerConfig POJO
     */
    public static MqttBrokerConfig toModel(MqttBrokerConfigRecord record) {
        if (record == null) {
            return null;
        }
        
        return new MqttBrokerConfig(
            record.getBrokerUrl(),
            record.getClientId(),
            record.getUsername(),
            record.getPassword(),
            record.isUseTls(),
            record.getQos(),
            record.isRetained(),
            record.getKeepAliveInterval(),
            record.getConnectionTimeout(),
            record.isCleanSession()
        );
    }
    
    /**
     * Converts MqttBrokerConfig POJO to MqttBrokerConfigRecord
     */
    public static void fromModel(MqttBrokerConfig model, MqttBrokerConfigRecord record) {
        if (model == null || record == null) {
            return;
        }
        
        record.setBrokerUrl(model.getBrokerUrl());
        record.setClientId(model.getClientId());
        record.setUsername(model.getUsername());
        record.setPassword(model.getPassword());
        record.setUseTls(model.isUseTls());
        record.setQos(model.getQos());
        record.setRetained(model.isRetained());
        record.setKeepAliveInterval(model.getKeepAlive());
        record.setConnectionTimeout(model.getConnectionTimeout());
        record.setCleanSession(model.isCleanSession());
    }
    
    /**
     * Converts MqttTagConfigRecord to TagPublishConfig POJO
     */
    public static TagPublishConfig toModel(MqttTagConfigRecord record) {
        if (record == null) {
            return null;
        }
        
        TagPublishConfig config = new TagPublishConfig();
        config.setEnabled(record.isEnabled());
        
        // Parse JSON strings to collections
        Type listType = new TypeToken<List<String>>(){}.getType();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Type topicMappingsType = new TypeToken<List<TopicMapping>>(){}.getType();
        
        List<String> providers = gson.fromJson(record.getTagProvidersJson(), listType);
        List<String> folders = gson.fromJson(record.getTagFoldersJson(), listType);
        Map<String, String> overrides = gson.fromJson(record.getTopicOverridesJson(), mapType);
        List<TopicMapping> mappings = gson.fromJson(record.getTopicMappingsJson(), topicMappingsType);
        
        config.setTagProviders(providers);
        config.setTagFolders(folders);
        config.setTopicOverrides(overrides);
        config.setTopicMappings(mappings);
        config.setPayloadTemplate(record.getPayloadTemplate());
        config.setIncludeMetadata(record.isIncludeMetadata());
        config.setValueDeadband(record.getValueDeadband());
        config.setPublishOnQualityChange(record.isPublishOnQualityChange());
        
        return config;
    }
    
    /**
     * Converts TagPublishConfig POJO to MqttTagConfigRecord
     */
    public static void fromModel(TagPublishConfig model, MqttTagConfigRecord record) {
        if (model == null || record == null) {
            return;
        }
        
        record.setEnabled(model.isEnabled());
        
        // Convert collections to JSON strings
        record.setTagProvidersJson(gson.toJson(model.getTagProviders()));
        record.setTagFoldersJson(gson.toJson(model.getTagFolders()));
        record.setTopicOverridesJson(gson.toJson(model.getTopicOverrides()));
        record.setTopicMappingsJson(gson.toJson(model.getTopicMappings()));
        
        record.setPayloadTemplate(model.getPayloadTemplate());
        record.setIncludeMetadata(model.isIncludeMetadata());
        record.setValueDeadband(model.getValueDeadband());
        record.setPublishOnQualityChange(model.isPublishOnQualityChange());
    }
}
