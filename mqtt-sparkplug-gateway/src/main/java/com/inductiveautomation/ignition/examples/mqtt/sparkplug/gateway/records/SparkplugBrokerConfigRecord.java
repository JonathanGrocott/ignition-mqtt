package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import simpleorm.dataset.SFieldFlags;

public class SparkplugBrokerConfigRecord extends PersistentRecord {

    public static final RecordMeta<SparkplugBrokerConfigRecord> META =
        new RecordMeta<>(SparkplugBrokerConfigRecord.class, "SparkplugBrokerConfig");

    public static final IdentityField Id = new IdentityField(META);

    public static final StringField Name = new StringField(META, "Name", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);

    public static final StringField BrokerUrl = new StringField(META, "BrokerUrl", SFieldFlags.SMANDATORY)
        .setDefault("tcp://localhost:1883");

    public static final StringField ClientId = new StringField(META, "ClientId", SFieldFlags.SMANDATORY)
        .setDefault("ignition-sparkplug-publisher");

    public static final StringField Username = new StringField(META, "Username");

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
