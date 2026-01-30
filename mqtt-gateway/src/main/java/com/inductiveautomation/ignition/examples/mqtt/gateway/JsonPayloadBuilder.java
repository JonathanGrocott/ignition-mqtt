package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig;

import java.util.Date;

/**
 * Builds JSON payloads for publishing tag data to MQTT.
 * 
 * Default payload structure:
 * {
 *   "timestamp": 1706140800000,
 *   "value": 72.5,
 *   "quality": "Good",
 *   "qualityCode": 192,
 *   "tagPath": "[default]Site/Area/Temperature"
 * }
 * 
 * With metadata (when enabled):
 * {
 *   "timestamp": 1706140800000,
 *   "value": 72.5,
 *   "quality": "Good",
 *   "qualityCode": 192,
 *   "tagPath": "[default]Site/Area/Temperature",
 *   "metadata": {
 *     "dataType": "Float8"
 *   }
 * }
 */
public class JsonPayloadBuilder {
    
    private final Gson gson;
    
    public JsonPayloadBuilder() {
        this.gson = new GsonBuilder()
            .serializeNulls()
            .create();
    }
    
    /**
     * Builds a JSON payload for a tag value change.
     * 
     * @param tagPath The tag path
     * @param qualifiedValue The tag value with quality and timestamp
     * @param includeMetadata Whether to include metadata in the payload
     * @return JSON string payload
     */
    public String buildPayload(TagPath tagPath, QualifiedValue qualifiedValue, boolean includeMetadata) {
        JsonObject payload = new JsonObject();
        
        // Timestamp (milliseconds since epoch)
        Date timestamp = qualifiedValue.getTimestamp();
        if (timestamp != null) {
            payload.addProperty("timestamp", timestamp.getTime());
        } else {
            payload.addProperty("timestamp", System.currentTimeMillis());
        }
        
        // Value (can be any JSON-serializable type)
        Object value = qualifiedValue.getValue();
        if (value == null) {
            payload.add("value", null);
        } else if (value instanceof Number) {
            payload.addProperty("value", (Number) value);
        } else if (value instanceof Boolean) {
            payload.addProperty("value", (Boolean) value);
        } else if (value instanceof String) {
            payload.addProperty("value", (String) value);
        } else {
            // For complex types, convert to string
            payload.addProperty("value", value.toString());
        }
        
        // Quality
        QualityCode quality = qualifiedValue.getQuality();
        if (quality != null) {
            payload.addProperty("quality", quality.getName());
            payload.addProperty("qualityCode", quality.getCode());
        } else {
            payload.addProperty("quality", "Unknown");
            payload.addProperty("qualityCode", 0);
        }
        
        // Tag path (full path with provider)
        payload.addProperty("tagPath", tagPath.toStringFull());
        
        // Metadata (if requested)
        if (includeMetadata) {
            JsonObject metadata = buildMetadata(qualifiedValue);
            if (metadata.size() > 0) {
                payload.add("metadata", metadata);
            }
        }
        
        return gson.toJson(payload);
    }

    /**
     * Builds a JSON payload based on selected fields and tag properties.
     *
     * @param tagPath The tag path
     * @param qualifiedValue The tag value with quality and timestamp
     * @param payloadFields Field selection configuration
     * @param properties Selected tag properties to include
     * @return JSON string payload
     */
    public String buildPayload(
        TagPath tagPath,
        QualifiedValue qualifiedValue,
        PayloadFieldConfig payloadFields,
        java.util.Map<String, Object> properties
    ) {
        JsonObject payload = new JsonObject();

        // Timestamp (milliseconds since epoch) - always included
        Date timestamp = qualifiedValue.getTimestamp();
        if (timestamp != null) {
            payload.addProperty("timestamp", timestamp.getTime());
        } else {
            payload.addProperty("timestamp", System.currentTimeMillis());
        }

        // Value - always included
        Object value = qualifiedValue.getValue();
        if (value == null) {
            payload.add("value", null);
        } else if (value instanceof Number) {
            payload.addProperty("value", (Number) value);
        } else if (value instanceof Boolean) {
            payload.addProperty("value", (Boolean) value);
        } else if (value instanceof String) {
            payload.addProperty("value", (String) value);
        } else {
            payload.addProperty("value", value.toString());
        }

        PayloadFieldConfig fields = payloadFields != null ? payloadFields : new PayloadFieldConfig();

        // Quality
        QualityCode quality = qualifiedValue.getQuality();
        if (fields.isIncludeQuality()) {
            if (quality != null) {
                payload.addProperty("quality", quality.getName());
            } else {
                payload.addProperty("quality", "Unknown");
            }
        }
        if (fields.isIncludeQualityCode()) {
            if (quality != null) {
                payload.addProperty("qualityCode", quality.getCode());
            } else {
                payload.addProperty("qualityCode", 0);
            }
        }

        // Tag path
        if (fields.isIncludeTagPath()) {
            payload.addProperty("tagPath", tagPath.toStringFull());
        }

        if (properties != null && !properties.isEmpty()) {
            JsonObject propertiesJson = new JsonObject();
            for (java.util.Map.Entry<String, Object> entry : properties.entrySet()) {
                addJsonValue(propertiesJson, entry.getKey(), entry.getValue());
            }
            if (propertiesJson.size() > 0) {
                payload.add("properties", propertiesJson);
            }
        }

        return gson.toJson(payload);
    }
    
    /**
     * Builds metadata object from the qualified value.
     * Currently includes basic type information.
     * Future: Can be extended to include units, engineering limits, etc.
     * 
     * @param qualifiedValue The qualified value
     * @return Metadata JSON object
     */
    private JsonObject buildMetadata(QualifiedValue qualifiedValue) {
        JsonObject metadata = new JsonObject();
        
        Object value = qualifiedValue.getValue();
        if (value != null) {
            String dataType = getDataTypeName(value);
            metadata.addProperty("dataType", dataType);
        }
        
        // TODO: Add more metadata in future phases:
        // - Engineering units
        // - Engineering limits (low/high)
        // - Custom properties
        // - Tag type (memory, OPC, etc.)
        
        return metadata;
    }
    
    /**
     * Gets a friendly name for the data type
     * 
     * @param value The value
     * @return Data type name
     */
    private String getDataTypeName(Object value) {
        if (value instanceof Integer) {
            return "Int4";
        } else if (value instanceof Long) {
            return "Int8";
        } else if (value instanceof Float) {
            return "Float4";
        } else if (value instanceof Double) {
            return "Float8";
        } else if (value instanceof Boolean) {
            return "Boolean";
        } else if (value instanceof String) {
            return "String";
        } else if (value instanceof Date) {
            return "DateTime";
        } else {
            return value.getClass().getSimpleName();
        }
    }
    
    /**
     * Builds a simplified payload (just value and timestamp)
     * Useful for high-frequency tags where bandwidth is a concern
     * 
     * @param qualifiedValue The tag value
     * @return JSON string payload
     */
    public String buildSimplePayload(QualifiedValue qualifiedValue) {
        JsonObject payload = new JsonObject();
        
        // Timestamp
        Date timestamp = qualifiedValue.getTimestamp();
        payload.addProperty("t", timestamp != null ? timestamp.getTime() : System.currentTimeMillis());
        
        // Value
        Object value = qualifiedValue.getValue();
        if (value == null) {
            payload.add("v", null);
        } else if (value instanceof Number) {
            payload.addProperty("v", (Number) value);
        } else if (value instanceof Boolean) {
            payload.addProperty("v", (Boolean) value);
        } else {
            payload.addProperty("v", value.toString());
        }
        
        return gson.toJson(payload);
    }

    private void addJsonValue(JsonObject json, String key, Object value) {
        if (value == null) {
            json.add(key, null);
            return;
        }
        if (value instanceof Number) {
            json.addProperty(key, (Number) value);
            return;
        }
        if (value instanceof Boolean) {
            json.addProperty(key, (Boolean) value);
            return;
        }
        if (value instanceof String) {
            json.addProperty(key, (String) value);
            return;
        }
        json.add(key, gson.toJsonTree(value));
    }
}
