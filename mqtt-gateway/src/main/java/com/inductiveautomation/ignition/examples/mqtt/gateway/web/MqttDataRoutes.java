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
        logger.debug("Mounting REST API routes for MQTT UNS Publisher");
        logger.debug("RouteGroup instance: {}", routes.getClass().getName());
        
        // Configuration routes
        // GET with optional ?id param - gets all brokers or specific broker
        logger.debug("Mounting route: /config/broker (GET)");
        routes.newRoute("/config/broker")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleBrokerGetOrDelete)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        
        // POST to create/update broker
        logger.debug("Mounting route: /config/broker (POST)");
        routes.newRoute("/config/broker")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleBrokerPost)
            .method(HttpMethod.POST)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        
        // DELETE with ?id param to delete broker
        logger.debug("Mounting route: /config/broker (DELETE)");
        routes.newRoute("/config/broker")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleBrokerGetOrDelete)
            .method(HttpMethod.DELETE)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        logger.debug("Successfully mounted: /config/broker (GET/POST/DELETE)");
        
        logger.debug("Mounting route: /config/tags (GET)");
        routes.newRoute("/config/tags")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleTagConfig)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        
        logger.debug("Mounting route: /config/tags (POST)");
        routes.newRoute("/config/tags")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleTagConfig)
            .method(HttpMethod.POST)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        logger.debug("Successfully mounted: /config/tags");
        
        logger.debug("Mounting route: /status");
        MqttStatusRoute statusRoute = new MqttStatusRoute(hook);
        routes.newRoute("/status")
            .type(RouteGroup.TYPE_JSON)
            .handler(statusRoute)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        logger.debug("Successfully mounted: /status");
        
        logger.debug("Mounting route: /test-connection");
        routes.newRoute("/test-connection")
            .type(RouteGroup.TYPE_JSON)
            .handler(MqttDataRoutes::handleTestConnection)
            .method(HttpMethod.POST)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
        logger.debug("Successfully mounted: /test-connection");
        
        logger.debug("Mounted 4 REST API routes with open access");
    }
    
    /**
     * Handle POST for broker configuration
     */
    private static Object handleBrokerPost(RequestContext req, HttpServletResponse res) {
        logger.debug("handleBrokerPost called! Method: {}, Path: {}", req.getMethod().name(), req.getPath());
        
        try {
            res.setContentType("application/json");
            return saveBrokerConfig(req);
        } catch (Exception e) {
            logger.error("Error handling broker POST request", e);
            return errorJson("Error: " + e.getMessage());
        }
    }
    
    /**
     * Handle GET/DELETE for broker - with optional id query parameter
     * GET with id = get specific broker
     * GET without id = get all brokers  
     * DELETE with id = delete broker
     */
    private static Object handleBrokerGetOrDelete(RequestContext req, HttpServletResponse res) {
        logger.debug("=== handleBrokerGetOrDelete called ===");
        logger.debug("Method: {}", req.getMethod().name());
        logger.debug("Path: {}", req.getPath());
        logger.debug("Query String: {}", req.getRequest().getQueryString());
        
        try {
            res.setContentType("application/json");
            
            String idParam = req.getParameter("id");
            logger.debug("ID parameter: {}", idParam);
            
            if ("GET".equals(req.getMethod().name())) {
                if (idParam != null && !idParam.isEmpty()) {
                    logger.debug("Processing GET request for specific broker with ID: {}", idParam);
                    return getBrokerById(req);
                } else {
                    logger.debug("Processing GET request for all brokers");
                    return getAllBrokerConfigs(req);
                }
            } else if ("DELETE".equals(req.getMethod().name())) {
                logger.debug("Processing DELETE request for broker");
                return deleteBroker(req);
            } else {
                logger.warn("Unsupported method: {}", req.getMethod().name());
                return errorJson("Unsupported method: " + req.getMethod().name());
            }
        } catch (Exception e) {
            logger.error("Error handling broker by ID request", e);
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
        logger.debug("handleTestConnection called! Method: {}, Path: {}", req.getMethod().name(), req.getPath());
        
        try {
            res.setContentType("application/json");
            
            String body = readRequestBody(req);
            logger.debug("Request body: {}", body);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(body, Map.class);
            logger.debug("Parsed data: {}", data);
            
            // Create test config
            String brokerUrl = (String) data.get("brokerUrl");
            String clientId = (String) data.get("clientId");
            
            // Simple validation
            if (brokerUrl == null || brokerUrl.isEmpty()) {
                return errorJson("Broker URL is required");
            }
            
            logger.debug("Testing connection to broker: {} with client ID: {}", brokerUrl, clientId);
            
            // Create response matching ApiResponse<TestConnectionResult> format
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            
            JsonObject testResult = new JsonObject();
            testResult.addProperty("connected", true);  // Simplified - actual test would try connecting
            testResult.addProperty("message", "Test not yet implemented");
            testResult.addProperty("connectionTimeMs", 0);
            
            response.add("data", testResult);
            
            logger.debug("Test connection response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error handling test connection request", e);
            return errorJson("Error: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    /**
     * Get all broker configurations
     */
    private static JsonObject getAllBrokerConfigs(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }
        
        PersistenceInterface db = context.getPersistenceInterface();
        SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
        List<MqttBrokerConfigRecord> records = db.query(query);
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        JsonArray brokers = new JsonArray();
        for (MqttBrokerConfigRecord record : records) {
            JsonObject brokerJson = new JsonObject();
            brokerJson.addProperty("id", record.getId());
            brokerJson.addProperty("name", record.getName());
            brokerJson.addProperty("brokerUrl", record.getBrokerUrl());
            brokerJson.addProperty("clientId", record.getClientId());
            brokerJson.addProperty("username", record.getUsername());
            brokerJson.addProperty("useTls", record.isUseTls());
            brokerJson.addProperty("qos", record.getQos());
            brokerJson.addProperty("retained", record.isRetained());
            brokerJson.addProperty("cleanSession", record.isCleanSession());
            brokerJson.addProperty("connectionTimeout", record.getConnectionTimeout());
            brokerJson.addProperty("keepAliveInterval", record.getKeepAliveInterval());
            brokerJson.addProperty("enabled", record.isEnabled());
            brokers.add(brokerJson);
        }
        
        response.add("data", brokers);
        return response;
    }
    
    /**
     * Get a specific broker by ID
     */
    private static JsonObject getBrokerById(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }
        
        // Get ID from query parameter
        String idParam = req.getParameter("id");
        logger.debug("getBrokerById - ID parameter: {}", idParam);
        
        if (idParam == null || idParam.isEmpty()) {
            return errorJson("Broker ID is required");
        }
        
        long id;
        try {
            id = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            return errorJson("Invalid broker ID: " + idParam);
        }
        
        PersistenceInterface db = context.getPersistenceInterface();
        MqttBrokerConfigRecord record = db.find(MqttBrokerConfigRecord.META, id);
        
        if (record == null) {
            return errorJson("Broker not found with ID: " + id);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        JsonObject data = new JsonObject();
        data.addProperty("id", record.getId());
        data.addProperty("name", record.getName());
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
        return response;
    }
    
    /**
     * Delete a broker by ID
     */
    private static JsonObject deleteBroker(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }
        
        // Get ID from query parameter
        String idParam = req.getParameter("id");
        logger.debug("deleteBroker - ID parameter: {}", idParam);
        
        if (idParam == null || idParam.isEmpty()) {
            return errorJson("Broker ID is required");
        }
        
        long id;
        try {
            id = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            return errorJson("Invalid broker ID: " + idParam);
        }
        
        PersistenceInterface db = context.getPersistenceInterface();
        
        // Check if any topic mappings reference this broker
        SQuery<MqttTagConfigRecord> tagQuery = new SQuery<>(MqttTagConfigRecord.META);
        List<MqttTagConfigRecord> tagRecords = db.query(tagQuery);
        
        for (MqttTagConfigRecord tagRecord : tagRecords) {
            try {
                String mappingsJson = tagRecord.getTopicMappingsJson();
                if (mappingsJson != null && !mappingsJson.isEmpty()) {
                    JsonElement mappingsElement = JsonParser.parseString(mappingsJson);
                    if (mappingsElement.isJsonArray()) {
                        JsonArray mappings = mappingsElement.getAsJsonArray();
                        for (JsonElement mappingElement : mappings) {
                            if (mappingElement.isJsonObject()) {
                                JsonObject mapping = mappingElement.getAsJsonObject();
                                if (mapping.has("brokerId")) {
                                    JsonElement brokerIdElement = mapping.get("brokerId");
                                    if (brokerIdElement != null && !brokerIdElement.isJsonNull()) {
                                        long brokerId = brokerIdElement.getAsLong();
                                        if (brokerId == id) {
                                            return errorJson("Cannot delete broker: It is being used by topic mappings. Please remove all topic mappings that reference this broker before deleting it.");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error checking topic mappings for broker references", e);
            }
        }
        
        // Delete the broker
        MqttBrokerConfigRecord record = db.find(MqttBrokerConfigRecord.META, id);
        if (record == null) {
            return errorJson("Broker not found with ID: " + id);
        }
        
        String brokerName = record.getName();
        
        try {
            // Remove broker from MultiBrokerManager first (disconnect if connected)
            logger.debug("Removing broker from MultiBrokerManager: {} (ID: {})", brokerName, id);
            hook.getMultiBrokerManager().removeBroker(id);
            
            // Delete the record
            logger.debug("Deleting broker record from database: {} (ID: {})", brokerName, id);
            record.deleteRecord();
            
            // Force commit by calling save with the deleted record
            db.save(record);
            
            logger.debug("Successfully deleted broker: {} (ID: {})", brokerName, id);
        } catch (Exception e) {
            logger.error("Error deleting broker: {} (ID: {})", brokerName, id, e);
            return errorJson("Failed to delete broker: " + e.getMessage());
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Broker deleted successfully");
        
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
        
        MqttBrokerConfigRecord record;
        boolean isNewRecord = false;
        
        // Check if this is an update (has ID) or create (no ID)
        if (data.containsKey("id") && data.get("id") != null) {
            // UPDATE existing
            long id = ((Number) data.get("id")).longValue();
            record = db.find(MqttBrokerConfigRecord.META, id);
            if (record == null) {
                return errorJson("Broker not found with ID: " + id);
            }
            logger.debug("Updating existing broker with ID: {}", id);
        } else {
            // CREATE new
            record = db.createNew(MqttBrokerConfigRecord.META);
            record.setName("New MQTT Broker");
            isNewRecord = true;
            logger.debug("Creating new broker configuration");
        }
        
        // Update fields
        if (data.containsKey("name")) record.setName((String) data.get("name"));
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
        
        logger.debug("Saved broker configuration: {} (ID: {})", record.getName(), record.getId());
        
        // Note: In multi-broker architecture, we don't apply configuration here
        // The MultiBrokerManager will be responsible for managing connections
        // based on which brokers have enabled topic mappings
        
        // Return the full saved configuration
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        JsonObject savedData = new JsonObject();
        savedData.addProperty("id", record.getId());
        savedData.addProperty("name", record.getName());
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

                String payloadFieldsJson = record.getPayloadFieldsJson();
                if (payloadFieldsJson != null && !payloadFieldsJson.isEmpty()) {
                    JsonElement payloadFields = JsonParser.parseString(payloadFieldsJson);
                    if (payloadFields.isJsonObject()) {
                        data.add("payloadFields", payloadFields.getAsJsonObject());
                    }
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
        if (data.containsKey("payloadFields")) {
            record.setPayloadFieldsJson(gson.toJson(data.get("payloadFields")));
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
        
        logger.debug("Saved tag configuration: {}", record.getName());
        
        // Apply the new configuration immediately (connect brokers and start/restart tag subscriptions)
        if (hook != null) {
            try {
                TagPublishConfig config = com.inductiveautomation.ignition.examples.mqtt.gateway.records.RecordMapper.toModel(record);
                if (config != null && config.isEnabled()) {
                    config.validate();
                    logger.debug("Applying tag configuration: {} providers, {} folders, {} mappings", 
                        config.getTagProviders().size(),
                        config.getTagFolders().size(),
                        config.getTopicMappings() != null ? config.getTopicMappings().size() : 0);
                    
                    // Determine which brokers are needed based on enabled topic mappings
                    java.util.Set<Long> brokersNeeded = new java.util.HashSet<>();
                    if (config.getTopicMappings() != null) {
                        for (com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping mapping : config.getTopicMappings()) {
                            if (mapping.isEnabled() && mapping.getBrokerId() != null) {
                                brokersNeeded.add(mapping.getBrokerId());
                                logger.debug("Topic mapping requires broker ID: {}", mapping.getBrokerId());
                            }
                        }
                    }
                    
                    logger.debug("Tag config requires {} broker(s): {}", brokersNeeded.size(), brokersNeeded);
                    
                    // Disconnect brokers that are no longer needed
                    java.util.Set<Long> currentBrokers = hook.getMultiBrokerManager().getActiveBrokerIds();
                    logger.debug("Currently active brokers: {}", currentBrokers);
                    for (Long brokerId : currentBrokers) {
                        if (!brokersNeeded.contains(brokerId)) {
                            logger.debug("Disconnecting broker {} (no longer needed)", brokerId);
                            hook.getMultiBrokerManager().disconnectBroker(brokerId);
                        }
                    }
                    
                    // Connect brokers that are now needed
                    for (Long brokerId : brokersNeeded) {
                        if (!hook.getMultiBrokerManager().isConnected(brokerId)) {
                            // Load broker config from database
                            try {
                                logger.debug("Loading broker config for ID {} from database...", brokerId);
                                MqttBrokerConfigRecord brokerRecord = db.find(MqttBrokerConfigRecord.META, brokerId);
                                
                                if (brokerRecord != null && brokerRecord.isEnabled()) {
                                    MqttBrokerConfig brokerConfig = com.inductiveautomation.ignition.examples.mqtt.gateway.records.RecordMapper.toModel(brokerRecord);
                                    brokerConfig.validate();
                                    logger.debug("Connecting broker {} (ID: {}) for tag publishing", brokerConfig.getName(), brokerId);
                                    hook.getMultiBrokerManager().connectBroker(brokerId, brokerConfig);
                                } else {
                                    logger.warn("Broker {} not found or disabled, cannot connect", brokerId);
                                }
                            } catch (Exception e) {
                                logger.error("Error loading/connecting broker {}: {}", brokerId, e.getMessage(), e);
                            }
                        } else {
                            logger.debug("Broker {} already connected", brokerId);
                        }
                    }
                    
                    // Get tag subscription manager and restart with new config
                    if (hook.getTagSubscriptionManager() != null) {
                        hook.getTagSubscriptionManager().shutdown();
                        hook.getTagSubscriptionManager().start(config);
                        logger.debug("Tag subscriptions started successfully");
                    } else {
                        logger.warn("Tag subscription manager not available");
                    }
                } else {
                    logger.info("Tag publishing disabled or invalid, stopping subscriptions and disconnecting brokers");
                    if (hook.getTagSubscriptionManager() != null) {
                        hook.getTagSubscriptionManager().shutdown();
                    }
                    if (hook.getMultiBrokerManager() != null) {
                        hook.getMultiBrokerManager().disconnectAll();
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

            String payloadFieldsJson = record.getPayloadFieldsJson();
            if (payloadFieldsJson != null && !payloadFieldsJson.isEmpty()) {
                JsonElement payloadFields = JsonParser.parseString(payloadFieldsJson);
                if (payloadFields.isJsonObject()) {
                    savedData.add("payloadFields", payloadFields.getAsJsonObject());
                }
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
