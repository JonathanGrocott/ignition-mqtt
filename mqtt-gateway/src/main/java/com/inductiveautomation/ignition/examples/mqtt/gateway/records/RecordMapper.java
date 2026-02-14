package com.inductiveautomation.ignition.examples.mqtt.gateway.records;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting between PersistentRecords and model POJOs.
 * This maintains backward compatibility while transitioning to database storage.
 */
public class RecordMapper {
    
    // Custom deserializer to handle brokerId as either integer or floating point
    private static class TopicMappingDeserializer implements JsonDeserializer<TopicMapping> {
        @Override
        public TopicMapping deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            
            TopicMapping mapping = new TopicMapping();
            
            if (obj.has("id")) {
                mapping.setId(obj.get("id").getAsString());
            }
            if (obj.has("sourcePattern")) {
                mapping.setSourcePattern(obj.get("sourcePattern").getAsString());
            }
            if (obj.has("topicPrefix")) {
                mapping.setTopicPrefix(obj.get("topicPrefix").getAsString());
            }
            if (obj.has("enabled")) {
                mapping.setEnabled(obj.get("enabled").getAsBoolean());
            }
            if (obj.has("preserveTopicCase")) {
                mapping.setPreserveTopicCase(obj.get("preserveTopicCase").getAsBoolean());
            }
            if (obj.has("publishMode") && obj.get("publishMode").isJsonPrimitive()) {
                try {
                    String mode = obj.get("publishMode").getAsString();
                    mapping.setPublishMode(com.inductiveautomation.ignition.examples.mqtt.common.model.TopicPublishMode.valueOf(mode));
                } catch (Exception ignored) {
                    mapping.setPublishMode(com.inductiveautomation.ignition.examples.mqtt.common.model.TopicPublishMode.PER_TAG_TOPIC);
                }
            }
            if (obj.has("batchWindowMs")) {
                mapping.setBatchWindowMs(obj.get("batchWindowMs").getAsInt());
            }
            if (obj.has("maxBatchSize")) {
                mapping.setMaxBatchSize(obj.get("maxBatchSize").getAsInt());
            }
            if (obj.has("useDefaultPayloadFields")) {
                mapping.setUseDefaultPayloadFields(obj.get("useDefaultPayloadFields").getAsBoolean());
            }
            if (obj.has("payloadFields") && obj.get("payloadFields").isJsonObject()) {
                PayloadFieldConfig payloadFields = context.deserialize(obj.get("payloadFields"), PayloadFieldConfig.class);
                mapping.setPayloadFields(payloadFields);
            }
            if (obj.has("brokerId") && !obj.get("brokerId").isJsonNull()) {
                // Handle brokerId as either integer or floating point
                JsonElement brokerIdElement = obj.get("brokerId");
                if (brokerIdElement.isJsonPrimitive()) {
                    JsonPrimitive primitive = brokerIdElement.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        // Convert any number type to Long
                        mapping.setBrokerId(primitive.getAsLong());
                    }
                }
            }
            
            return mapping;
        }
    }
    
    // Configure Gson with custom deserializer
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(TopicMapping.class, new TopicMappingDeserializer())
        .create();
    
    /**
     * Converts MqttBrokerConfigRecord to MqttBrokerConfig POJO
     */
    public static MqttBrokerConfig toModel(MqttBrokerConfigRecord record) {
        if (record == null) {
            return null;
        }
        
        MqttBrokerConfig config = new MqttBrokerConfig();
        config.setId(record.getId());
        config.setName(record.getName());
        config.setBrokerUrl(record.getBrokerUrl());
        config.setClientId(record.getClientId());
        config.setUsername(record.getUsername());
        config.setPassword(record.getPassword());
        config.setUseTls(record.isUseTls());
        config.setQos(record.getQos());
        config.setRetained(record.isRetained());
        config.setKeepAlive(record.getKeepAliveInterval());
        config.setConnectionTimeout(record.getConnectionTimeout());
        config.setSlowReconnectIntervalSeconds(record.getSlowReconnectIntervalSeconds());
        config.setCleanSession(record.isCleanSession());
        return config;
    }
    
    /**
     * Converts MqttBrokerConfig POJO to MqttBrokerConfigRecord
     */
    public static void fromModel(MqttBrokerConfig model, MqttBrokerConfigRecord record) {
        if (model == null || record == null) {
            return;
        }
        
        record.setName(model.getName());
        record.setBrokerUrl(model.getBrokerUrl());
        record.setClientId(model.getClientId());
        record.setUsername(model.getUsername());
        record.setPassword(model.getPassword());
        record.setUseTls(model.isUseTls());
        record.setQos(model.getQos());
        record.setRetained(model.isRetained());
        record.setKeepAliveInterval(model.getKeepAlive());
        record.setConnectionTimeout(model.getConnectionTimeout());
        record.setSlowReconnectIntervalSeconds(model.getSlowReconnectIntervalSeconds());
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
        Type payloadFieldsType = new TypeToken<PayloadFieldConfig>(){}.getType();
        
        List<String> providers = gson.fromJson(record.getTagProvidersJson(), listType);
        List<String> folders = gson.fromJson(record.getTagFoldersJson(), listType);
        Map<String, String> overrides = gson.fromJson(record.getTopicOverridesJson(), mapType);
        
        String mappingsJson = record.getTopicMappingsJson();
        List<TopicMapping> mappings = gson.fromJson(mappingsJson, topicMappingsType);

        PayloadFieldConfig payloadFields = null;
        String payloadFieldsJson = record.getPayloadFieldsJson();
        if (payloadFieldsJson != null && !payloadFieldsJson.isEmpty() && !payloadFieldsJson.equals("null")) {
            payloadFields = gson.fromJson(payloadFieldsJson, payloadFieldsType);
        }
        
        config.setTagProviders(providers);
        config.setTagFolders(folders);
        config.setTopicOverrides(overrides);
        config.setTopicMappings(mappings);
        config.setPayloadTemplate(record.getPayloadTemplate());
        config.setPayloadFields(payloadFields);
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
        record.setPayloadFieldsJson(gson.toJson(model.getPayloadFields()));
        record.setIncludeMetadata(model.isIncludeMetadata());
        record.setValueDeadband(model.getValueDeadband());
        record.setPublishOnQualityChange(model.isPublishOnQualityChange());
    }
}
