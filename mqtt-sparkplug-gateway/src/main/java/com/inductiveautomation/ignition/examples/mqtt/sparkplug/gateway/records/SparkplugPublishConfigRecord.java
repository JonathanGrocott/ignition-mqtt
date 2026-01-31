package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import simpleorm.dataset.SFieldFlags;

public class SparkplugPublishConfigRecord extends PersistentRecord {

    public static final RecordMeta<SparkplugPublishConfigRecord> META =
        new RecordMeta<>(SparkplugPublishConfigRecord.class, "SparkplugPublishConfig");

    public static final IdentityField Id = new IdentityField(META);

    public static final LongField BrokerConfigId =
        new LongField(META, "BrokerConfigId", SFieldFlags.SMANDATORY);

    public static final StringField Name = new StringField(META, "Name", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);

    public static final BooleanField Enabled = new BooleanField(META, "Enabled")
        .setDefault(true);

    public static final StringField GroupId = new StringField(META, "GroupId", SFieldFlags.SMANDATORY)
        .setDefault("group");

    public static final StringField EdgeNodeId = new StringField(META, "EdgeNodeId", SFieldFlags.SMANDATORY)
        .setDefault("edge");

    public static final StringField DeviceMappings = new StringField(META, "DeviceMappings", 8000)
        .setDefault("[]");

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }

    public Long getId() {
        return getLong(Id);
    }

    public Long getBrokerConfigId() {
        return getLong(BrokerConfigId);
    }

    public void setBrokerConfigId(Long brokerConfigId) {
        setLong(BrokerConfigId, brokerConfigId);
    }

    public String getName() {
        return getString(Name);
    }

    public void setName(String name) {
        setString(Name, name);
    }

    public boolean isEnabled() {
        return getBoolean(Enabled);
    }

    public void setEnabled(boolean enabled) {
        setBoolean(Enabled, enabled);
    }

    public String getGroupId() {
        return getString(GroupId);
    }

    public void setGroupId(String groupId) {
        setString(GroupId, groupId);
    }

    public String getEdgeNodeId() {
        return getString(EdgeNodeId);
    }

    public void setEdgeNodeId(String edgeNodeId) {
        setString(EdgeNodeId, edgeNodeId);
    }

    public String getDeviceMappingsJson() {
        return getString(DeviceMappings);
    }

    public void setDeviceMappingsJson(String json) {
        setString(DeviceMappings, json);
    }
}
