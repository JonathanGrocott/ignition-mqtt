package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.web;

import com.google.gson.Gson;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugPublishConfig;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.SparkplugGatewayHook;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records.SparkplugBrokerConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records.SparkplugPublishConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records.SparkplugRecordMapper;
import com.inductiveautomation.ignition.gateway.dataroutes.AccessControlStrategy;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceInterface;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleorm.dataset.SQuery;

import java.io.BufferedReader;
import java.util.List;

public final class SparkplugDataRoutes {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugDataRoutes.class);
    private static final Gson gson = new Gson();
    private static SparkplugGatewayHook hook;

    private SparkplugDataRoutes() {
    }

    public static void mountRoutes(RouteGroup routes, SparkplugGatewayHook moduleHook) {
        hook = moduleHook;
        logger.info("Mounting REST API routes for MQTT SparkplugB Publisher");

        routes.newRoute("/config/broker")
            .type(RouteGroup.TYPE_JSON)
            .handler(SparkplugDataRoutes::handleBrokerGetOrDelete)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();

        routes.newRoute("/config/broker")
            .type(RouteGroup.TYPE_JSON)
            .handler(SparkplugDataRoutes::handleBrokerPost)
            .method(HttpMethod.POST)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();

        routes.newRoute("/config/broker")
            .type(RouteGroup.TYPE_JSON)
            .handler(SparkplugDataRoutes::handleBrokerGetOrDelete)
            .method(HttpMethod.DELETE)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();

        routes.newRoute("/config/publish")
            .type(RouteGroup.TYPE_JSON)
            .handler(SparkplugDataRoutes::handlePublishConfig)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();

        routes.newRoute("/config/publish")
            .type(RouteGroup.TYPE_JSON)
            .handler(SparkplugDataRoutes::handlePublishConfig)
            .method(HttpMethod.POST)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();

        routes.newRoute("/ui/sparkplug-config.js")
            .handler(SparkplugDataRoutes::handleUiBundle)
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();

        routes.newRoute("/ping")
            .handler((req, res) -> {
                res.setContentType("text/plain");
                return "ok";
            })
            .method(HttpMethod.GET)
            .accessControl(AccessControlStrategy.OPEN_ROUTE)
            .mount();
    }

    private static Object handleBrokerPost(RequestContext req, HttpServletResponse res) {
        try {
            res.setContentType("application/json");
            return saveBrokerConfig(req);
        } catch (Exception e) {
            logger.error("Error handling broker POST request", e);
            return errorJson("Error: " + e.getMessage());
        }
    }

    private static Object handleBrokerGetOrDelete(RequestContext req, HttpServletResponse res) {
        try {
            res.setContentType("application/json");
            String idParam = req.getParameter("id");
            if ("GET".equals(req.getMethod().name())) {
                if (idParam != null && !idParam.isEmpty()) {
                    return getBrokerById(req);
                }
                return getAllBrokerConfigs(req);
            }
            if ("DELETE".equals(req.getMethod().name())) {
                return deleteBroker(req);
            }
            return errorJson("Unsupported method: " + req.getMethod().name());
        } catch (Exception e) {
            logger.error("Error handling broker request", e);
            return errorJson("Error: " + e.getMessage());
        }
    }

    private static Object handlePublishConfig(RequestContext req, HttpServletResponse res) {
        try {
            res.setContentType("application/json");
            if ("GET".equals(req.getMethod().name())) {
                return getPublishConfigs(req);
            }
            if ("POST".equals(req.getMethod().name())) {
                return savePublishConfig(req);
            }
            return errorJson("Unsupported method: " + req.getMethod().name());
        } catch (Exception e) {
            logger.error("Error handling publish config", e);
            return errorJson("Error: " + e.getMessage());
        }
    }

    private static Object handleUiBundle(RequestContext req, HttpServletResponse res) {
        res.setContentType("application/javascript");
        try {
            ClassLoader loader = SparkplugDataRoutes.class.getClassLoader();
            try (java.io.InputStream input = loader.getResourceAsStream("mounted/sparkplug-config.js")) {
                if (input == null) {
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return "// sparkplug-config.js not found";
                }
                return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.error("Failed to load Sparkplug UI bundle", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return "// sparkplug-config.js failed to load";
        }
    }

    private static JsonObject getAllBrokerConfigs(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }

        PersistenceInterface db = context.getPersistenceInterface();
        SQuery<SparkplugBrokerConfigRecord> query = new SQuery<>(SparkplugBrokerConfigRecord.META);
        List<SparkplugBrokerConfigRecord> records = db.query(query);

        JsonArray brokers = new JsonArray();
        for (SparkplugBrokerConfigRecord record : records) {
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

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", brokers);
        return response;
    }

    private static JsonObject getBrokerById(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }

        String idParam = req.getParameter("id");
        if (idParam == null || idParam.isEmpty()) {
            return errorJson("Broker ID is required");
        }

        long id = Long.parseLong(idParam);
        PersistenceInterface db = context.getPersistenceInterface();
        SparkplugBrokerConfigRecord record = db.find(SparkplugBrokerConfigRecord.META, id);
        if (record == null) {
            return errorJson("Broker not found with ID: " + id);
        }

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

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", data);
        return response;
    }

    private static JsonObject deleteBroker(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }

        String idParam = req.getParameter("id");
        if (idParam == null || idParam.isEmpty()) {
            return errorJson("Broker ID is required");
        }

        long id = Long.parseLong(idParam);
        PersistenceInterface db = context.getPersistenceInterface();
        SparkplugBrokerConfigRecord record = db.find(SparkplugBrokerConfigRecord.META, id);
        if (record == null) {
            return errorJson("Broker not found with ID: " + id);
        }

        record.deleteRecord();
        db.save(record);

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        return response;
    }

    private static JsonObject saveBrokerConfig(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }

        String body = readRequestBody(req);
        if (body == null || body.isEmpty()) {
            return errorJson("Request body is empty");
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = gson.fromJson(body, java.util.Map.class);

        PersistenceInterface db = context.getPersistenceInterface();
        SparkplugBrokerConfigRecord record;
        if (data.containsKey("id") && data.get("id") != null) {
            long id = ((Number) data.get("id")).longValue();
            record = db.find(SparkplugBrokerConfigRecord.META, id);
            if (record == null) {
                return errorJson("Broker not found with ID: " + id);
            }
        } else {
            record = db.createNew(SparkplugBrokerConfigRecord.META);
            record.setName("New MQTT Broker");
        }

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

        try {
            hook.reloadConfigurations();
        } catch (Exception e) {
            logger.warn("Failed to reload Sparkplug configuration after broker save: {}", e.getMessage());
        }

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

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", savedData);
        return response;
    }

    private static JsonObject getPublishConfigs(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }

        PersistenceInterface db = context.getPersistenceInterface();
        SQuery<SparkplugPublishConfigRecord> query = new SQuery<>(SparkplugPublishConfigRecord.META);
        List<SparkplugPublishConfigRecord> records = db.query(query);

        JsonArray configs = new JsonArray();
        for (SparkplugPublishConfigRecord record : records) {
            SparkplugPublishConfig config = SparkplugRecordMapper.toModel(record);
            configs.add(JsonParser.parseString(gson.toJson(config)));
        }

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", configs);
        return response;
    }

    private static JsonObject savePublishConfig(RequestContext req) throws Exception {
        GatewayContext context = hook.getGatewayContext();
        if (context == null) {
            return errorJson("Module not initialized");
        }

        String body = readRequestBody(req);
        if (body == null || body.isEmpty()) {
            return errorJson("Request body is empty");
        }

        SparkplugPublishConfig publishConfig = gson.fromJson(body, SparkplugPublishConfig.class);
        publishConfig.validate();

        PersistenceInterface db = context.getPersistenceInterface();
        SparkplugPublishConfigRecord record;
        if (publishConfig.getId() != null) {
            record = db.find(SparkplugPublishConfigRecord.META, publishConfig.getId());
            if (record == null) {
                return errorJson("Publish config not found with ID: " + publishConfig.getId());
            }
        } else {
            record = db.createNew(SparkplugPublishConfigRecord.META);
            record.setName("New Sparkplug Publish");
        }

        SparkplugRecordMapper.updateRecord(record, publishConfig);
        db.save(record);

        try {
            hook.reloadConfigurations();
        } catch (Exception e) {
            logger.warn("Failed to reload Sparkplug configuration after publish save: {}", e.getMessage());
        }

        SparkplugPublishConfig saved = SparkplugRecordMapper.toModel(record);
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", JsonParser.parseString(gson.toJson(saved)));
        return response;
    }

    private static String readRequestBody(RequestContext req) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getRequest().getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return value instanceof Number && ((Number) value).intValue() != 0;
    }

    private static int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return 0;
    }

    private static JsonObject errorJson(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", message);
        return response;
    }
}
