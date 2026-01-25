package com.inductiveautomation.ignition.examples.mqtt.gateway.web;

import com.google.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.examples.mqtt.gateway.MqttGatewayHook;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttBrokerConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttTagConfigRecord;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.dataroutes.AccessControlStrategy;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceInterface;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SQuery;

import java.io.BufferedReader;
import java.util.*;

/**
 * Mounts REST API routes for MQTT UNS Publisher module.
 * Routes are accessible at /data/com.inductiveautomation.mqtt.uns/*
 */
public final class MqttDataRoutes {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttDataRoutes.class);
    private static final Gson gson = new Gson();
    private static MqttGatewayHook hook;
    
    private MqttDataRoutes() {
        // Private constructor
    }
    
    /**
     * Mount all data routes for the MQTT module
     */
    public static void mountRoutes(RouteGroup routes, MqttGatewayHook moduleHook) {
        hook = moduleHook;
        logger.info("Mounting REST API routes for MQTT UNS Publisher");
        logger.info("RouteGroup instance: {}", routes.getClass().getName());
        
        // Configuration routes - need both GET and POST
        logger.info("Mounting route: /config/broker (GET)");
        routes.newRoute("/config/broker")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleBrokerConfig)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        
        logger.info("Mounting route: /config/broker (POST)");
        routes.newRoute("/config/broker")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleBrokerConfig)
            .method(HttpMethod.POST)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        logger.info("Successfully mounted: /config/broker");
        
        logger.info("Mounting route: /config/tags (GET)");
        routes.newRoute("/config/tags")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleTagConfig)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        
        logger.info("Mounting route: /config/tags (POST)");
        routes.newRoute("/config/tags")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleTagConfig)
            .method(HttpMethod.POST)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        logger.info("Successfully mounted: /config/tags");
        
        logger.info("Mounting route: /status");
        MqttStatusRoute statusRoute = new MqttStatusRoute(hook);
        routes.newRoute("/status")
            .type(RouteGroup.TYPE_JSON)
            .handler(statusRoute)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        logger.info("Successfully mounted: /status");
        
        logger.info("Mounting route: /test-connection");
        routes.newRoute("/test-connection")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleTestConnection)
            .method(HttpMethod.POST)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        logger.info("Successfully mounted: /test-connection");
        
        logger.info("Mounted 4 REST API routes with open access");
    }
    
    /**
     * Handle GET/POST for broker configuration
     */
    private static Object handleBrokerConfig(RequestContext req, HttpServletResponse res) {
        logger.info("handleBrokerConfig called! Method: {}, Path: {}", req.getMethod().name(), req.getPath());
        
        try {
            res.setContentType("application/json");
            
            if ("GET".equals(req.getMethod().name())) {
                logger.info("Processing GET request for broker config");
                return getBrokerConfig(req);
            } else if ("POST".equals(req.getMethod().name())) {
                logger.info("Processing POST request for broker config");
                return saveBrokerConfig(req);
            } else {
                logger.warn("Unsupported method: {}", req.getMethod().name());
                return errorJson("Unsupported method: " + req.getMethod().name());
            }
        } catch (Exception e) {
            logger.error("Error handling broker config request", e);
            return errorJson("Error: " + e.getMessage());
        }
    }
    
    /**
     * Handle GET/POST for tag configuration
     */
    private static Object handleTagConfig(RequestContext req, HttpServletResponse res) {
        try {
            res.setContentType("application/json");
            
            if ("GET".equals(req.getMethod().name())) {
                return getTagConfig(req);
            } else if ("POST".equals(req.getMethod().name())) {
                return saveTagConfig(req);
            } else {
                return errorJson("Unsupported method: " + req.getMethod().name());
            }
        } catch (Exception e) {
            logger.error("Error handling tag config request", e);
            return errorJson("Error: " + e.getMessage());
        }
    }
    
    /**
     * Handle POST for test connection
     */
    private static Object handleStatus(RequestContext req, HttpServletResponse res) {
        try {
            res.setContentType("application/json");
            
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            
            if (hook != null && hook.getPublisherManager() != null) {
                JsonObject status = new JsonObject();
                status.addProperty("connectionState", hook.getPublisherManager().getConnectionState().name());
                status.addProperty("messagesPublished", hook.getStatistics().getMessagesPublished());
                status.addProperty("messagesFailed", hook.getStatistics().getMessagesFailedToPublish());
                json.add("data", status);
            } else {
                json.add("data", new JsonObject());
            }
            
            return json;
        } catch (Exception e) {
            logger.error("Error handling status request", e);
            return errorJson("Error: " + e.getMessage());
        }
    }
    
    /**
     * Handle POST for connection testing
     */
    private static Object handleTestConnection(RequestContext req, HttpServletResponse res) {
        logger.info("handleTestConnection called! Method: {}, Path: {}", req.getMethod().name(), req.getPath());
        
        try {
            res.setContentType("application/json");
            
            String body = readRequestBody(req);
            logger.info("Request body: {}", body);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(body, Map.class);
            logger.info("Parsed data: {}", data);
            
            // Create test config
            String brokerUrl = (String) data.get("brokerUrl");
            String clientId = (String) data.get("clientId");
            
            // Simple validation
            if (brokerUrl == null || brokerUrl.isEmpty()) {
                return errorJson("Broker URL is required");
            }
            
            logger.info("Testing connection to broker: {} with client ID: {}", brokerUrl, clientId);
            
            // Create response matching ApiResponse<TestConnectionResult> format
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            
            JsonObject testResult = new JsonObject();
            testResult.addProperty("connected", true);  // Simplified - actual test would try connecting
            testResult.addProperty("message", "Test not yet implemented");
            testResult.addProperty("connectionTimeMs", 0);
            
            response.add("data", testResult);
            
            logger.info("Test connection response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error handling test connection request", e);
            return errorJson("Error: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private static JsonObject getBrokerConfig(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }
        
        PersistenceInterface db = context.getPersistenceInterface();
        SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
        List<MqttBrokerConfigRecord> records = db.query(query);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        if (!records.isEmpty()) {
            MqttBrokerConfigRecord record = records.get(0);
            JsonObject data = new JsonObject();
            data.addProperty("id", record.getId());
            data.addProperty("brokerUrl", record.getBrokerUrl());
            data.addProperty("clientId", record.getClientId());
            data.addProperty("username", record.getUsername());
            data.addProperty("useTls", record.isUseTls());
            data.addProperty("qos", record.getQos());
            data.addProperty("retained", record.isRetained());
            data.addProperty("cleanSession", record.isCleanSession());
            data.addProperty("connectionTimeout", record.getConnectionTimeout());
            data.addProperty("keepAliveInterval", record.getKeepAliveInterval());
            data.addProperty("enabled", record.isEnabled());
            response.add("data", data);
        } else {
            response.add("data", null);
        }
        
        return response;
    }
    
    private static JsonObject saveBrokerConfig(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }
        
        String body = readRequestBody(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = gson.fromJson(body, Map.class);
        
        PersistenceInterface db = context.getPersistenceInterface();
        
        // Check if updating existing or creating new
        SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
        List<MqttBrokerConfigRecord> existing = db.query(query);
        
        MqttBrokerConfigRecord record;
        if (!existing.isEmpty()) {
            record = existing.get(0);
        } else {
            record = db.createNew(MqttBrokerConfigRecord.META);
            // Set required Name field for new records
            record.setName("MQTT Broker Configuration");
        }
        
        // Update fields
        if (data.containsKey("brokerUrl")) record.setBrokerUrl((String) data.get("brokerUrl"));
        if (data.containsKey("clientId")) record.setClientId((String) data.get("clientId"));
        if (data.containsKey("username")) record.setUsername((String) data.get("username"));
        if (data.containsKey("password")) {
            String password = (String) data.get("password");
            if (password != null && !password.isEmpty()) {
                record.setPassword(password);
            }
        }
        if (data.containsKey("useTls")) record.setUseTls(toBoolean(data.get("useTls")));
        if (data.containsKey("qos")) record.setQos(toInt(data.get("qos")));
        if (data.containsKey("retained")) record.setRetained(toBoolean(data.get("retained")));
        if (data.containsKey("cleanSession")) record.setCleanSession(toBoolean(data.get("cleanSession")));
        if (data.containsKey("connectionTimeout")) record.setConnectionTimeout(toInt(data.get("connectionTimeout")));
        if (data.containsKey("keepAliveInterval")) record.setKeepAliveInterval(toInt(data.get("keepAliveInterval")));
        if (data.containsKey("enabled")) record.setEnabled(toBoolean(data.get("enabled")));
        
        db.save(record);
        
        logger.info("Saved broker configuration: {}", record.getBrokerUrl());
        
        // Apply the new broker configuration immediately (reconnect to broker)
        if (hook != null && hook.getPublisherManager() != null) {
            try {
                MqttBrokerConfig config = com.inductiveautomation.ignition.examples.mqtt.gateway.records.RecordMapper.toModel(record);
                if (config != null && record.isEnabled()) {
                    config.validate();
                    logger.info("Applying broker configuration: {}", config.getBrokerUrl());
                    
                    // Disconnect and reconnect with new config
                    hook.getPublisherManager().disconnect();
                    hook.getPublisherManager().connect(config);
                    logger.info("Broker connection updated successfully");
                } else {
                    logger.info("Broker disabled, disconnecting");
                    hook.getPublisherManager().disconnect();
                }
            } catch (Exception e) {
                logger.error("Error applying broker configuration", e);
            }
        }
        
        // Return the full saved configuration
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        JsonObject savedData = new JsonObject();
        savedData.addProperty("id", record.getId());
        savedData.addProperty("brokerUrl", record.getBrokerUrl());
        savedData.addProperty("clientId", record.getClientId());
        savedData.addProperty("username", record.getUsername());
        savedData.addProperty("useTls", record.isUseTls());
        savedData.addProperty("qos", record.getQos());
        savedData.addProperty("retained", record.isRetained());
        savedData.addProperty("cleanSession", record.isCleanSession());
        savedData.addProperty("connectionTimeout", record.getConnectionTimeout());
        savedData.addProperty("keepAliveInterval", record.getKeepAliveInterval());
        savedData.addProperty("enabled", record.isEnabled());
        
        response.add("data", savedData);
        
        return response;
    }
    
    private static JsonObject getTagConfig(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }
        
        PersistenceInterface db = context.getPersistenceInterface();
        SQuery<MqttTagConfigRecord> query = new SQuery<>(MqttTagConfigRecord.META);
        List<MqttTagConfigRecord> records = db.query(query);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        if (!records.isEmpty()) {
            MqttTagConfigRecord record = records.get(0);
            JsonObject data = new JsonObject();
            data.addProperty("id", record.getId());
            data.addProperty("name", record.getName());
            data.addProperty("enabled", record.isEnabled());
            data.addProperty("includeMetadata", record.isIncludeMetadata());
            data.addProperty("valueDeadband", record.getValueDeadband());
            data.addProperty("publishOnQualityChange", record.isPublishOnQualityChange());
            
            // Parse JSON strings back to arrays/objects
            try {
                JsonElement providers = JsonParser.parseString(record.getTagProvidersJson());
                if (providers.isJsonArray()) {
                    data.add("tagProviders", providers.getAsJsonArray());
                }
                
                JsonElement folders = JsonParser.parseString(record.getTagFoldersJson());
                if (folders.isJsonArray()) {
                    data.add("tagFolders", folders.getAsJsonArray());
                }
                
                JsonElement overrides = JsonParser.parseString(record.getTopicOverridesJson());
                if (overrides.isJsonObject()) {
                    data.add("topicOverrides", overrides.getAsJsonObject());
                }
                
                JsonElement mappings = JsonParser.parseString(record.getTopicMappingsJson());
                if (mappings.isJsonArray()) {
                    data.add("topicMappings", mappings.getAsJsonArray());
                }
            } catch (Exception e) {
                logger.warn("Error parsing JSON fields in tag config", e);
            }
            
            if (record.getPayloadTemplate() != null) {
                data.addProperty("payloadTemplate", record.getPayloadTemplate());
            }
            
            response.add("data", data);
        } else {
            response.add("data", null);
        }
        
        return response;
    }
    
    private static JsonObject saveTagConfig(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }
        
        String body = readRequestBody(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = gson.fromJson(body, Map.class);
        
        PersistenceInterface db = context.getPersistenceInterface();
        
        // Check if updating existing or creating new
        SQuery<MqttTagConfigRecord> query = new SQuery<>(MqttTagConfigRecord.META);
        List<MqttTagConfigRecord> existing = db.query(query);
        
        MqttTagConfigRecord record;
        if (!existing.isEmpty()) {
            record = existing.get(0);
        } else {
            record = db.createNew(MqttTagConfigRecord.META);
            record.setName("Default Tag Publishing");
            
            // Link to broker configuration (required foreign key)
            // Get the first broker config or create association later
            SQuery<MqttBrokerConfigRecord> brokerQuery = new SQuery<>(MqttBrokerConfigRecord.META);
            List<MqttBrokerConfigRecord> brokers = db.query(brokerQuery);
            if (!brokers.isEmpty()) {
                record.setBrokerConfigId(brokers.get(0).getId());
            } else {
                // If no broker exists yet, we'll need to handle this differently
                // For now, set to 1 assuming broker will be created first
                record.setBrokerConfigId(1L);
            }
        }
        
        // Update fields
        if (data.containsKey("name")) record.setName((String) data.get("name"));
        if (data.containsKey("enabled")) record.setEnabled(toBoolean(data.get("enabled")));
        if (data.containsKey("tagProviders")) {
            // tagProviders is an array from frontend, convert to JSON string
            record.setTagProvidersJson(gson.toJson(data.get("tagProviders")));
        }
        if (data.containsKey("tagFolders")) {
            record.setTagFoldersJson(gson.toJson(data.get("tagFolders")));
        }
        if (data.containsKey("topicOverrides")) {
            record.setTopicOverridesJson(gson.toJson(data.get("topicOverrides")));
        }
        if (data.containsKey("topicMappings")) {
            record.setTopicMappingsJson(gson.toJson(data.get("topicMappings")));
        }
        if (data.containsKey("payloadTemplate")) {
            record.setPayloadTemplate((String) data.get("payloadTemplate"));
        }
        if (data.containsKey("includeMetadata")) {
            record.setIncludeMetadata(toBoolean(data.get("includeMetadata")));
        }
        if (data.containsKey("valueDeadband")) {
            record.setValueDeadband(toDouble(data.get("valueDeadband")));
        }
        if (data.containsKey("publishOnQualityChange")) {
            record.setPublishOnQualityChange(toBoolean(data.get("publishOnQualityChange")));
        }
        
        db.save(record);
        
        logger.info("Saved tag configuration: {}", record.getName());
        
        // Apply the new configuration immediately (start/restart tag subscriptions)
        if (hook != null) {
            try {
                TagPublishConfig config = com.inductiveautomation.ignition.examples.mqtt.gateway.records.RecordMapper.toModel(record);
                if (config != null && config.isEnabled()) {
                    config.validate();
                    logger.info("Applying tag configuration: {} providers, {} folders", 
                        config.getTagProviders().size(),
                        config.getTagFolders().size());
                    
                    // Get tag subscription manager and restart with new config
                    if (hook.getTagSubscriptionManager() != null) {
                        hook.getTagSubscriptionManager().shutdown();
                        hook.getTagSubscriptionManager().start(config);
                        logger.info("Tag subscriptions started successfully");
                    } else {
                        logger.warn("Tag subscription manager not available");
                    }
                } else {
                    logger.info("Tag publishing disabled or invalid, stopping subscriptions");
                    if (hook.getTagSubscriptionManager() != null) {
                        hook.getTagSubscriptionManager().shutdown();
                    }
                }
            } catch (Exception e) {
                logger.error("Error applying tag configuration", e);
            }
        }
        
        // Return the full saved configuration
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        JsonObject savedData = new JsonObject();
        savedData.addProperty("id", record.getId());
        savedData.addProperty("name", record.getName());
        savedData.addProperty("enabled", record.isEnabled());
        savedData.addProperty("includeMetadata", record.isIncludeMetadata());
        savedData.addProperty("valueDeadband", record.getValueDeadband());
        savedData.addProperty("publishOnQualityChange", record.isPublishOnQualityChange());
        
        // Parse JSON strings back to arrays/objects for response
        try {
            JsonElement providers = JsonParser.parseString(record.getTagProvidersJson());
            if (providers.isJsonArray()) {
                savedData.add("tagProviders", providers.getAsJsonArray());
            }
            
            JsonElement folders = JsonParser.parseString(record.getTagFoldersJson());
            if (folders.isJsonArray()) {
                savedData.add("tagFolders", folders.getAsJsonArray());
            }
            
            JsonElement overrides = JsonParser.parseString(record.getTopicOverridesJson());
            if (overrides.isJsonObject()) {
                savedData.add("topicOverrides", overrides.getAsJsonObject());
            }
            
            JsonElement mappings = JsonParser.parseString(record.getTopicMappingsJson());
            if (mappings.isJsonArray()) {
                savedData.add("topicMappings", mappings.getAsJsonArray());
            }
        } catch (Exception e) {
            logger.warn("Error parsing JSON fields in tag config response", e);
        }
        
        if (record.getPayloadTemplate() != null) {
            savedData.addProperty("payloadTemplate", record.getPayloadTemplate());
        }
        
        response.add("data", savedData);
        
        return response;
    }
    
    private static JsonObject errorJson(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("success", false);
        json.addProperty("error", message);
        return json;
    }
    
    /**
     * Safely convert a value to integer, handling both Number and String inputs
     */
    private static int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse integer from string: {}", value);
                return 0;
            }
        }
        logger.warn("Unexpected type for integer conversion: {}", value.getClass().getName());
        return 0;
    }
    
    /**
     * Safely convert a value to double, handling both Number and String inputs
     */
    private static double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse double from string: {}", value);
                return 0.0;
            }
        }
        logger.warn("Unexpected type for double conversion: {}", value.getClass().getName());
        return 0.0;
    }
    
    /**
     * Safely convert a value to boolean, handling both Boolean and String inputs
     */
    private static boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
    
    private static String readRequestBody(RequestContext req) throws Exception {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = req.getRequest().getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
}
