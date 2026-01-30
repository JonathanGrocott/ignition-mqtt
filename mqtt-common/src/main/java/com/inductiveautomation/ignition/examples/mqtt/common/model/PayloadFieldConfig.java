package com.inductiveautomation.ignition.examples.mqtt.common.model;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines which fields should be included in the MQTT payload.
 */
public class PayloadFieldConfig {

    @SerializedName("includeQuality")
    private boolean includeQuality = true;

    @SerializedName("includeQualityCode")
    private boolean includeQualityCode = true;

    @SerializedName("includeTagPath")
    private boolean includeTagPath = true;

    @SerializedName("properties")
    private Map<String, Boolean> properties = new HashMap<>();

    public PayloadFieldConfig() {
    }

    public boolean isIncludeQuality() {
        return includeQuality;
    }

    public void setIncludeQuality(boolean includeQuality) {
        this.includeQuality = includeQuality;
    }

    public boolean isIncludeQualityCode() {
        return includeQualityCode;
    }

    public void setIncludeQualityCode(boolean includeQualityCode) {
        this.includeQualityCode = includeQualityCode;
    }

    public boolean isIncludeTagPath() {
        return includeTagPath;
    }

    public void setIncludeTagPath(boolean includeTagPath) {
        this.includeTagPath = includeTagPath;
    }

    public Map<String, Boolean> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Boolean> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }

    public boolean isPropertyEnabled(String key) {
        if (properties == null) {
            return false;
        }
        Boolean enabled = properties.get(key);
        return enabled != null && enabled;
    }

    public PayloadFieldConfig copy() {
        PayloadFieldConfig copy = new PayloadFieldConfig();
        copy.includeQuality = this.includeQuality;
        copy.includeQualityCode = this.includeQualityCode;
        copy.includeTagPath = this.includeTagPath;
        copy.properties = this.properties != null ? new HashMap<>(this.properties) : new HashMap<>();
        return copy;
    }
}
