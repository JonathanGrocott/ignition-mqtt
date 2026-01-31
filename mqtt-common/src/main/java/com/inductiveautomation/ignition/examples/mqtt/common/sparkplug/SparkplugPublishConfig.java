package com.inductiveautomation.ignition.examples.mqtt.common.sparkplug;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SparkplugPublishConfig {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("enabled")
    private boolean enabled;

    @SerializedName("brokerId")
    private Long brokerId;

    @SerializedName("groupId")
    private String groupId;

    @SerializedName("edgeNodeId")
    private String edgeNodeId;

    @SerializedName("deviceMappings")
    private List<SparkplugDeviceMapping> deviceMappings;

    public SparkplugPublishConfig() {
        this.name = "Default SparkplugB Config";
        this.enabled = true;
        this.groupId = "group";
        this.edgeNodeId = "edge";
        this.deviceMappings = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(Long brokerId) {
        this.brokerId = brokerId;
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

    public List<SparkplugDeviceMapping> getDeviceMappings() {
        return deviceMappings;
    }

    public void setDeviceMappings(List<SparkplugDeviceMapping> deviceMappings) {
        this.deviceMappings = deviceMappings != null ? deviceMappings : new ArrayList<>();
    }

    public void validate() {
        if (brokerId == null) {
            throw new IllegalArgumentException("Broker ID is required");
        }
        boolean hasGroup = groupId != null && !groupId.trim().isEmpty();
        boolean hasEdge = edgeNodeId != null && !edgeNodeId.trim().isEmpty();
        if (deviceMappings != null) {
            for (SparkplugDeviceMapping mapping : deviceMappings) {
                if (mapping != null) {
                    if (!hasGroup || !hasEdge) {
                        if (mapping.getGroupId() == null || mapping.getGroupId().trim().isEmpty()) {
                            throw new IllegalArgumentException("Group ID is required for device mappings");
                        }
                        if (mapping.getEdgeNodeId() == null || mapping.getEdgeNodeId().trim().isEmpty()) {
                            throw new IllegalArgumentException("Edge Node ID is required for device mappings");
                        }
                    }
                    mapping.validate();
                }
            }
        }
    }
}
