package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugDeviceMapping;
import com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugPublishConfig;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class SparkplugRecordMapper {

    private static final Gson gson = new GsonBuilder().create();
    private static final Type DEVICE_MAPPING_LIST = new TypeToken<List<SparkplugDeviceMapping>>() {}.getType();

    public static MqttBrokerConfig toModel(SparkplugBrokerConfigRecord record) {
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
        config.setCleanSession(record.isCleanSession());
        config.setConnectionTimeout(record.getConnectionTimeout());
        config.setKeepAlive(record.getKeepAliveInterval());
        config.setSlowReconnectIntervalSeconds(record.getSlowReconnectIntervalSeconds());
        return config;
    }

    public static void updateRecord(SparkplugBrokerConfigRecord record, MqttBrokerConfig config) {
        record.setName(config.getName());
        record.setBrokerUrl(config.getBrokerUrl());
        record.setClientId(config.getClientId());
        record.setUsername(config.getUsername());
        record.setPassword(config.getPassword());
        record.setUseTls(config.isUseTls());
        record.setQos(config.getQos());
        record.setRetained(config.isRetained());
        record.setCleanSession(config.isCleanSession());
        record.setConnectionTimeout(config.getConnectionTimeout());
        record.setKeepAliveInterval(config.getKeepAlive());
        record.setSlowReconnectIntervalSeconds(config.getSlowReconnectIntervalSeconds());
    }

    public static SparkplugPublishConfig toModel(SparkplugPublishConfigRecord record) {
        SparkplugPublishConfig config = new SparkplugPublishConfig();
        config.setId(record.getId());
        config.setBrokerId(record.getBrokerConfigId());
        config.setName(record.getName());
        config.setEnabled(record.isEnabled());
        config.setGroupId(record.getGroupId());
        config.setEdgeNodeId(record.getEdgeNodeId());
        config.setDeviceMappings(parseMappings(record.getDeviceMappingsJson()));
        return config;
    }

    public static void updateRecord(SparkplugPublishConfigRecord record, SparkplugPublishConfig config) {
        record.setBrokerConfigId(config.getBrokerId());
        record.setName(config.getName());
        record.setEnabled(config.isEnabled());
        record.setGroupId(config.getGroupId());
        record.setEdgeNodeId(config.getEdgeNodeId());
        record.setDeviceMappingsJson(gson.toJson(config.getDeviceMappings()));
    }

    private static List<SparkplugDeviceMapping> parseMappings(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<SparkplugDeviceMapping> mappings = gson.fromJson(json, DEVICE_MAPPING_LIST);
            return mappings != null ? mappings : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
