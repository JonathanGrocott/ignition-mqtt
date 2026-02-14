package com.inductiveautomation.ignition.examples.mqtt.gateway.records;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import simpleorm.dataset.SFieldFlags;

/**
 * Stores MQTT broker connection configuration in the internal database.
 * This replaces the JSON file-based MqttBrokerConfig approach with proper database persistence.
 * 
 * Features:
 * - Automatic table creation in internal database
 * - Type-safe field definitions
 * - Automatic replication to redundant Gateways
 * - Change notification support
 */
public class MqttBrokerConfigRecord extends PersistentRecord {
    
    // Meta object - required for all PersistentRecords
    // This defines the table name and record class
    public static final RecordMeta<MqttBrokerConfigRecord> META = 
        new RecordMeta<>(MqttBrokerConfigRecord.class, "MqttBrokerConfig");
    
    // Primary key - auto-incrementing ID
    public static final IdentityField Id = new IdentityField(META);
    
    // Configuration name (e.g., "Main MQTT Broker", "Backup Broker")
    // SMANDATORY = required field, SDESCRIPTIVE = used in UI displays
    public static final StringField Name = new StringField(META, "Name", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);
    
    // Broker connection settings
    public static final StringField BrokerUrl = new StringField(META, "BrokerUrl", SFieldFlags.SMANDATORY)
        .setDefault("tcp://localhost:1883");
    
    public static final StringField ClientId = new StringField(META, "ClientId", SFieldFlags.SMANDATORY)
        .setDefault("ignition-mqtt-publisher");
    
    public static final StringField Username = new StringField(META, "Username");
    
    // Password should be encrypted when stored - Ignition handles this automatically
    public static final EncodedStringField Password = new EncodedStringField(META, "Password");
    
    public static final BooleanField UseTls = new BooleanField(META, "UseTls")
        .setDefault(false);
    
    public static final IntField Qos = new IntField(META, "Qos")
        .setDefault(1);
    
    public static final BooleanField Retained = new BooleanField(META, "Retained")
        .setDefault(false);
    
    public static final BooleanField CleanSession = new BooleanField(META, "CleanSession")
        .setDefault(true);
    
    public static final IntField ConnectionTimeout = new IntField(META, "ConnectionTimeout")
        .setDefault(30);
    
    public static final IntField KeepAliveInterval = new IntField(META, "KeepAliveInterval")
        .setDefault(60);

    public static final IntField SlowReconnectIntervalSeconds = new IntField(META, "SlowReconnectIntervalSeconds")
        .setDefault(600);
    
    public static final BooleanField Enabled = new BooleanField(META, "Enabled")
        .setDefault(true);
    
    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
    
    // Convenience getters/setters for type-safe access
    
    public Long getId() {
        return getLong(Id);
    }
    
    public String getName() {
        return getString(Name);
    }
    
    public void setName(String name) {
        setString(Name, name);
    }
    
    public String getBrokerUrl() {
        return getString(BrokerUrl);
    }
    
    public void setBrokerUrl(String url) {
        setString(BrokerUrl, url);
    }
    
    public String getClientId() {
        return getString(ClientId);
    }
    
    public void setClientId(String clientId) {
        setString(ClientId, clientId);
    }
    
    public String getUsername() {
        return getString(Username);
    }
    
    public void setUsername(String username) {
        setString(Username, username);
    }
    
    public String getPassword() {
        return getString(Password);
    }
    
    public void setPassword(String password) {
        setString(Password, password);
    }
    
    public boolean isUseTls() {
        return getBoolean(UseTls);
    }
    
    public void setUseTls(boolean useTls) {
        setBoolean(UseTls, useTls);
    }
    
    public int getQos() {
        return getInt(Qos);
    }
    
    public void setQos(int qos) {
        setInt(Qos, qos);
    }
    
    public boolean isRetained() {
        return getBoolean(Retained);
    }
    
    public void setRetained(boolean retained) {
        setBoolean(Retained, retained);
    }
    
    public boolean isCleanSession() {
        return getBoolean(CleanSession);
    }
    
    public void setCleanSession(boolean cleanSession) {
        setBoolean(CleanSession, cleanSession);
    }
    
    public int getConnectionTimeout() {
        return getInt(ConnectionTimeout);
    }
    
    public void setConnectionTimeout(int timeout) {
        setInt(ConnectionTimeout, timeout);
    }
    
    public int getKeepAliveInterval() {
        return getInt(KeepAliveInterval);
    }
    
    public void setKeepAliveInterval(int interval) {
        setInt(KeepAliveInterval, interval);
    }

    public int getSlowReconnectIntervalSeconds() {
        return getInt(SlowReconnectIntervalSeconds);
    }

    public void setSlowReconnectIntervalSeconds(int intervalSeconds) {
        setInt(SlowReconnectIntervalSeconds, intervalSeconds);
    }
    
    public boolean isEnabled() {
        return getBoolean(Enabled);
    }
    
    public void setEnabled(boolean enabled) {
        setBoolean(Enabled, enabled);
    }
}
