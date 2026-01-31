package com.inductiveautomation.ignition.examples.mqtt.gateway.web;

import com.google.gson.Gson;
import com.inductiveautomation.ignition.examples.mqtt.gateway.config.ConfigurationManager;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttBrokerConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttTagConfigRecord;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteHandler;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceInterface;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SQuery;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * REST API route handler for MQTT module configuration.
 * 
 * Endpoints:
 * - GET  /data/mqtt-uns-publisher/config       - Get all configurations
 * - GET  /data/mqtt-uns-publisher/config/broker  - Get broker config
 * - GET  /data/mqtt-uns-publisher/config/tags    - Get tag config
 * - POST /data/mqtt-uns-publisher/config/broker  - Save broker config
 * - POST /data/mqtt-uns-publisher/config/tags    - Save tag config
 */
public class MqttConfigRoute implements RouteHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttConfigRoute.class);
    private final com.inductiveautomation.ignition.examples.mqtt.gateway.MqttGatewayHook hook;
    private final Gson gson = new Gson();
    
    /**
     * Constructor that accepts the module hook instance.
     * Context and config manager are fetched lazily from the hook when handling requests,
     * since mountRouteHandlers() is called before setup() in the module lifecycle.
     */
    public MqttConfigRoute(com.inductiveautomation.ignition.examples.mqtt.gateway.MqttGatewayHook hook) {
        this.hook = hook;
    }
    
    @Override
    public Object handle(RequestContext requestContext, HttpServletResponse response) throws Exception {
        HttpMethod httpMethod = requestContext.getMethod();
        String method = httpMethod.name(); // Convert enum to String
        String path = requestContext.getPath();
        
        logger.debug("Config route: {} {}", method, path);
        
        // Set response content type to JSON
        response.setContentType("application/json");
        
        Map<String, Object> result;
        
        // Route based on path and method
        if (path.endsWith("/broker")) {
            if ("GET".equals(method)) {
                result = handleGetBrokerConfig();
            } else if ("POST".equals(method)) {
                result = handleSaveBrokerConfig(requestContext);
            } else {
                result = errorResponse("Unsupported method: " + method);
            }
        } else if (path.endsWith("/tags")) {
            if ("GET".equals(method)) {
                result = handleGetTagConfig();
            } else if ("POST".equals(method)) {
                result = handleSaveTagConfig(requestContext);
            } else {
                result = errorResponse("Unsupported method: " + method);
            }
        } else if (path.endsWith("/config")) {
            // Return both broker and tag config
            if ("GET".equals(method)) {
                result = handleGetAllConfig();
            } else {
                result = errorResponse("Unsupported method: " + method);
            }
        } else {
            result = errorResponse("Unsupported operation: " + method + " " + path);
        }
        
        // Convert Map to JSON string
        return gson.toJson(result);
    }
    
    /**
     * GET /config - Returns both broker and tag configurations
     */
    private Map<String, Object> handleGetAllConfig() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            GatewayContext context = hook.getGatewayContext();
            if (context == null) {
                return errorResponse("Module not initialized");
            }
            
            PersistenceInterface db = context.getPersistenceInterface();
            
            // Get broker config
            SQuery<MqttBrokerConfigRecord> brokerQuery = new SQuery<>(MqttBrokerConfigRecord.META);
            List<MqttBrokerConfigRecord> brokerRecords = db.query(brokerQuery);
            
            if (!brokerRecords.isEmpty()) {
                response.put("broker", recordToMap(brokerRecords.get(0)));
            } else {
                response.put("broker", null);
            }
            
            // Get tag config
            SQuery<MqttTagConfigRecord> tagQuery = new SQuery<>(MqttTagConfigRecord.META);
            List<MqttTagConfigRecord> tagRecords = db.query(tagQuery);
            
            if (!tagRecords.isEmpty()) {
                response.put("tags", recordToMap(tagRecords.get(0)));
            } else {
                response.put("tags", null);
            }
            
            response.put("success", true);
            return response;
            
        } catch (Exception e) {
            logger.error("Error loading configuration", e);
            return errorResponse("Failed to load configuration: " + e.getMessage());
        }
    }
    
    /**
     * GET /config/broker - Returns broker configuration
     */
    private Map<String, Object> handleGetBrokerConfig() {
        try {
            GatewayContext context = hook.getGatewayContext();
            if (context == null) {
                return errorResponse("Module not initialized");
            }
            
            PersistenceInterface db = context.getPersistenceInterface();
            
            SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
            List<MqttBrokerConfigRecord> records = db.query(query);
            
            if (records.isEmpty()) {
                return successResponse(null);
            }
            
            return successResponse(recordToMap(records.get(0)));
            
        } catch (Exception e) {
            logger.error("Error loading broker configuration", e);
            return errorResponse("Failed to load broker configuration: " + e.getMessage());
        }
    }
    
    /**
     * GET /config/tags - Returns tag configuration
     */
    private Map<String, Object> handleGetTagConfig() {
        try {
            GatewayContext context = hook.getGatewayContext();
            if (context == null) {
                return errorResponse("Module not initialized");
            }
            
            PersistenceInterface db = context.getPersistenceInterface();
            
            SQuery<MqttTagConfigRecord> query = new SQuery<>(MqttTagConfigRecord.META);
            List<MqttTagConfigRecord> records = db.query(query);
            
            if (records.isEmpty()) {
                return successResponse(null);
            }
            
            return successResponse(recordToMap(records.get(0)));
            
        } catch (Exception e) {
            logger.error("Error loading tag configuration", e);
            return errorResponse("Failed to load tag configuration: " + e.getMessage());
        }
    }
    
    /**
     * POST /config/broker - Saves broker configuration
     */
    private Map<String, Object> handleSaveBrokerConfig(RequestContext requestContext) {
        try {
            GatewayContext context = hook.getGatewayContext();
            if (context == null) {
                return errorResponse("Module not initialized");
            }
            
            String body = readRequestBody(requestContext);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(body, Map.class);
            
            PersistenceInterface db = context.getPersistenceInterface();
            
            // Check if updating existing or creating new
            Long id = data.get("id") != null ? ((Number) data.get("id")).longValue() : null;
            MqttBrokerConfigRecord record;
            
            if (id != null) {
                // Update existing
                record = db.find(MqttBrokerConfigRecord.META, id);
                if (record == null) {
                    return errorResponse("Broker configuration not found: " + id);
                }
            } else {
                // Check if any record exists (we only support one for now)
                SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
                List<MqttBrokerConfigRecord> existing = db.query(query);
                
                if (!existing.isEmpty()) {
                    // Update the existing one
                    record = existing.get(0);
                } else {
                    // Create new
                    record = db.createNew(MqttBrokerConfigRecord.META);
                }
            }
            
            // Update fields from JSON
            updateBrokerRecord(record, data);
            
            // Save to database (this will trigger RecordListener)
            db.save(record);
            
            logger.info("Saved broker configuration: {}", record.getBrokerUrl());
            
            Map<String, Object> response = successResponse(recordToMap(record));
            response.put("id", record.getId());
            return response;
            
        } catch (Exception e) {
            logger.error("Error saving broker configuration", e);
            return errorResponse("Failed to save broker configuration: " + e.getMessage());
        }
    }
    
    /**
     * POST /config/tags - Saves tag configuration
     */
    private Map<String, Object> handleSaveTagConfig(RequestContext requestContext) {
        try {
            GatewayContext context = hook.getGatewayContext();
            if (context == null) {
                return errorResponse("Module not initialized");
            }
            
            String body = readRequestBody(requestContext);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(body, Map.class);
            
            PersistenceInterface db = context.getPersistenceInterface();
            
            // Check if updating existing or creating new
            Long id = data.get("id") != null ? ((Number) data.get("id")).longValue() : null;
            MqttTagConfigRecord record;
            
            if (id != null) {
                // Update existing
                record = db.find(MqttTagConfigRecord.META, id);
                if (record == null) {
                    return errorResponse("Tag configuration not found: " + id);
                }
            } else {
                // Check if any record exists (we only support one for now)
                SQuery<MqttTagConfigRecord> query = new SQuery<>(MqttTagConfigRecord.META);
                List<MqttTagConfigRecord> existing = db.query(query);
                
                if (!existing.isEmpty()) {
                    // Update the existing one
                    record = existing.get(0);
                } else {
                    // Create new
                    record = db.createNew(MqttTagConfigRecord.META);
                    record.setName("Default Tag Publishing");
                }
            }
            
            // Update fields from JSON
            updateTagRecord(record, data);
            
            // Save to database (this will trigger RecordListener)
            db.save(record);
            
            logger.info("Saved tag configuration: {}", record.getName());
            
            Map<String, Object> response = successResponse(recordToMap(record));
            response.put("id", record.getId());
            return response;
            
        } catch (Exception e) {
            logger.error("Error saving tag configuration", e);
            return errorResponse("Failed to save tag configuration: " + e.getMessage());
        }
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private void updateBrokerRecord(MqttBrokerConfigRecord record, Map<String, Object> data) {
        if (data.containsKey("brokerUrl")) {
            record.setBrokerUrl((String) data.get("brokerUrl"));
        }
        if (data.containsKey("clientId")) {
            record.setClientId((String) data.get("clientId"));
        }
        if (data.containsKey("username")) {
            record.setUsername((String) data.get("username"));
        }
        if (data.containsKey("password")) {
            String password = (String) data.get("password");
            if (password != null && !password.isEmpty()) {
                record.setPassword(password);
            }
        }
        if (data.containsKey("useTls")) {
            record.setUseTls((Boolean) data.get("useTls"));
        }
        if (data.containsKey("qos")) {
            record.setQos(((Number) data.get("qos")).intValue());
        }
        if (data.containsKey("retained")) {
            record.setRetained((Boolean) data.get("retained"));
        }
        if (data.containsKey("cleanSession")) {
            record.setCleanSession((Boolean) data.get("cleanSession"));
        }
        if (data.containsKey("connectionTimeout")) {
            record.setConnectionTimeout(((Number) data.get("connectionTimeout")).intValue());
        }
        if (data.containsKey("keepAliveInterval")) {
            record.setKeepAliveInterval(((Number) data.get("keepAliveInterval")).intValue());
        }
        if (data.containsKey("enabled")) {
            record.setEnabled((Boolean) data.get("enabled"));
        }
    }
    
    private void updateTagRecord(MqttTagConfigRecord record, Map<String, Object> data) {
        if (data.containsKey("name")) {
            record.setName((String) data.get("name"));
        }
        if (data.containsKey("enabled")) {
            record.setEnabled((Boolean) data.get("enabled"));
        }
        if (data.containsKey("tagProviders")) {
            record.setTagProvidersJson(gson.toJson(data.get("tagProviders")));
        }
        if (data.containsKey("tagFolders")) {
            record.setTagFoldersJson(gson.toJson(data.get("tagFolders")));
        }
        if (data.containsKey("topicOverrides")) {
            record.setTopicOverridesJson(gson.toJson(data.get("topicOverrides")));
        }
        if (data.containsKey("payloadTemplate")) {
            record.setPayloadTemplate((String) data.get("payloadTemplate"));
        }
        if (data.containsKey("payloadFields")) {
            record.setPayloadFieldsJson(gson.toJson(data.get("payloadFields")));
        }
        if (data.containsKey("includeMetadata")) {
            record.setIncludeMetadata((Boolean) data.get("includeMetadata"));
        }
        if (data.containsKey("valueDeadband")) {
            record.setValueDeadband(((Number) data.get("valueDeadband")).doubleValue());
        }
        if (data.containsKey("publishOnQualityChange")) {
            record.setPublishOnQualityChange((Boolean) data.get("publishOnQualityChange"));
        }
    }
    
    private Map<String, Object> recordToMap(MqttBrokerConfigRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("brokerUrl", record.getBrokerUrl());
        map.put("clientId", record.getClientId());
        map.put("username", record.getUsername());
        // Don't send password to client for security
        map.put("useTls", record.isUseTls());
        map.put("qos", record.getQos());
        map.put("retained", record.isRetained());
        map.put("cleanSession", record.isCleanSession());
        map.put("connectionTimeout", record.getConnectionTimeout());
        map.put("keepAliveInterval", record.getKeepAliveInterval());
        map.put("enabled", record.isEnabled());
        return map;
    }
    
    private Map<String, Object> recordToMap(MqttTagConfigRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("name", record.getName());
        map.put("enabled", record.isEnabled());
        
        // Parse JSON strings back to collections - handle null/empty safely
        String providersJson = record.getTagProvidersJson();
        if (providersJson != null && !providersJson.isEmpty() && !providersJson.equals("null")) {
            map.put("tagProviders", gson.fromJson(providersJson, List.class));
        } else {
            map.put("tagProviders", new ArrayList<>());
        }
        
        String foldersJson = record.getTagFoldersJson();
        if (foldersJson != null && !foldersJson.isEmpty() && !foldersJson.equals("null")) {
            map.put("tagFolders", gson.fromJson(foldersJson, List.class));
        } else {
            map.put("tagFolders", new ArrayList<>());
        }
        
        String overridesJson = record.getTopicOverridesJson();
        if (overridesJson != null && !overridesJson.isEmpty() && !overridesJson.equals("null")) {
            map.put("topicOverrides", gson.fromJson(overridesJson, Map.class));
        } else {
            map.put("topicOverrides", new HashMap<>());
        }

        String payloadFieldsJson = record.getPayloadFieldsJson();
        if (payloadFieldsJson != null && !payloadFieldsJson.isEmpty() && !payloadFieldsJson.equals("null")) {
            map.put("payloadFields", gson.fromJson(payloadFieldsJson, Map.class));
        }
        
        map.put("payloadTemplate", record.getPayloadTemplate());
        map.put("includeMetadata", record.isIncludeMetadata());
        map.put("valueDeadband", record.getValueDeadband());
        map.put("publishOnQualityChange", record.isPublishOnQualityChange());
        return map;
    }
    
    private Map<String, Object> successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }
    
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
    
    /**
     * Helper method to read request body from RequestContext
     */
    private String readRequestBody(RequestContext requestContext) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = requestContext.getRequest().getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
}
