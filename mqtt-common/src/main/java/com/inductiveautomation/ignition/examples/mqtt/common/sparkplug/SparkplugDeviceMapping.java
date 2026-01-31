package com.inductiveautomation.ignition.examples.mqtt.common.sparkplug;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.UUID;

public class SparkplugDeviceMapping {

    @SerializedName("id")
    private String id;

    @SerializedName("sourcePattern")
    private String sourcePattern;

    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("groupId")
    private String groupId;

    @SerializedName("edgeNodeId")
    private String edgeNodeId;

    @SerializedName("enabled")
    private boolean enabled;

    public SparkplugDeviceMapping() {
        this.id = UUID.randomUUID().toString();
        this.sourcePattern = "";
        this.deviceId = "";
        this.groupId = "";
        this.edgeNodeId = "";
        this.enabled = true;
    }

    public SparkplugDeviceMapping(String id, String sourcePattern, String deviceId, boolean enabled) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.sourcePattern = sourcePattern;
        this.deviceId = deviceId;
        this.groupId = "";
        this.edgeNodeId = "";
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourcePattern() {
        return sourcePattern;
    }

    public void setSourcePattern(String sourcePattern) {
        this.sourcePattern = sourcePattern;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getEdgeNodeId() {
        return edgeNodeId;
    }

    public void setEdgeNodeId(String edgeNodeId) {
        this.edgeNodeId = edgeNodeId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void validate() {
        if (sourcePattern == null || sourcePattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Source pattern cannot be empty");
        }
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be empty");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SparkplugDeviceMapping that = (SparkplugDeviceMapping) o;
        return enabled == that.enabled &&
            Objects.equals(id, that.id) &&
            Objects.equals(sourcePattern, that.sourcePattern) &&
            Objects.equals(deviceId, that.deviceId) &&
            Objects.equals(groupId, that.groupId) &&
            Objects.equals(edgeNodeId, that.edgeNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sourcePattern, deviceId, groupId, edgeNodeId, enabled);
    }

    @Override
    public String toString() {
        return "SparkplugDeviceMapping{" +
            "id='" + id + '\'' +
            ", sourcePattern='" + sourcePattern + '\'' +
            ", deviceId='" + deviceId + '\'' +
            ", groupId='" + groupId + '\'' +
            ", edgeNodeId='" + edgeNodeId + '\'' +
            ", enabled=" + enabled +
            '}';
    }
}
