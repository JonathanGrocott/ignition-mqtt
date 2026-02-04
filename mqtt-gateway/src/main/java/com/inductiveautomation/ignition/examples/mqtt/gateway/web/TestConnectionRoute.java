package com.inductiveautomation.ignition.examples.mqtt.gateway.web;

import com.google.gson.Gson;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteHandler;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API route handler for testing MQTT broker connections.
 * 
 * Endpoints:
 * - POST /data/mqtt-uns-publisher/test-connection - Test connection to MQTT broker
 */
public class TestConnectionRoute implements RouteHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TestConnectionRoute.class);
    private final Gson gson = new Gson();
    
    @Override
    public Object handle(RequestContext requestContext, HttpServletResponse response) throws Exception {
        HttpMethod httpMethod = requestContext.getMethod();
        String method = httpMethod.name(); // Convert enum to String
        
        // Set response content type to JSON
        response.setContentType("application/json");
        
        if (!"POST".equals(method)) {
            return gson.toJson(errorResponse("Only POST method is supported"));
        }
        
        Map<String, Object> result = handleTestConnection(requestContext);
        return gson.toJson(result);
    }
    
    /**
     * POST /test-connection - Tests connection to MQTT broker with provided settings
     */
    private Map<String, Object> handleTestConnection(RequestContext requestContext) {
        MqttClient testClient = null;
        
        try {
            String body = readRequestBody(requestContext);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(body, Map.class);
            
            // Extract connection parameters
            String brokerUrl = (String) data.get("brokerUrl");
            String clientId = (String) data.get("clientId");
            String username = (String) data.get("username");
            String password = (String) data.get("password");
            Boolean useTls = (Boolean) data.getOrDefault("useTls", false);
            Integer connectionTimeout = data.get("connectionTimeout") != null ? 
                ((Number) data.get("connectionTimeout")).intValue() : 30;
            Integer keepAliveInterval = data.get("keepAliveInterval") != null ?
                ((Number) data.get("keepAliveInterval")).intValue() : 60;
            Boolean cleanSession = (Boolean) data.getOrDefault("cleanSession", true);
            
            // Validate required fields
            if (brokerUrl == null || brokerUrl.trim().isEmpty()) {
                return errorResponse("Broker URL is required");
            }
            
            if (clientId == null || clientId.trim().isEmpty()) {
                clientId = "ignition-test-" + System.currentTimeMillis();
            }
            
            logger.debug("Testing connection to MQTT broker: {}", brokerUrl);
            
            // Create test client
            testClient = new MqttClient(
                brokerUrl,
                clientId,
                new MemoryPersistence()
            );
            
            // Configure connection options
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(cleanSession);
            options.setConnectionTimeout(connectionTimeout);
            options.setKeepAliveInterval(keepAliveInterval);
            options.setAutomaticReconnect(false); // Don't auto-reconnect for test
            
            if (username != null && !username.trim().isEmpty()) {
                options.setUserName(username);
                
                if (password != null && !password.trim().isEmpty()) {
                    options.setPassword(password.toCharArray());
                }
            }
            
            // Attempt connection
            long startTime = System.currentTimeMillis();
            testClient.connect(options);
            long connectionTime = System.currentTimeMillis() - startTime;
            
            // Connection successful
            boolean connected = testClient.isConnected();
            
            // Disconnect immediately
            if (connected) {
                testClient.disconnect();
                testClient.close();
            }
            
            logger.debug("Test connection successful to {} (took {}ms)", brokerUrl, connectionTime);
            
            Map<String, Object> result = new HashMap<>();
            result.put("connected", connected);
            result.put("connectionTimeMs", connectionTime);
            result.put("brokerUrl", brokerUrl);
            result.put("message", "Connection test successful");
            
            return successResponse(result);
            
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            logger.warn("Test connection failed: {} - {}", e.getReasonCode(), e.getMessage());
            
            Map<String, Object> result = new HashMap<>();
            result.put("connected", false);
            result.put("errorCode", e.getReasonCode());
            result.put("error", e.getMessage());
            result.put("message", getHumanReadableError(e));
            
            return errorResponse(result);
            
        } catch (Exception e) {
            logger.error("Unexpected error during connection test", e);
            return errorResponse("Connection test failed: " + e.getMessage());
            
        } finally {
            // Clean up test client
            if (testClient != null) {
                try {
                    if (testClient.isConnected()) {
                        testClient.disconnect();
                    }
                    testClient.close();
                } catch (Exception e) {
                    logger.debug("Error closing test client", e);
                }
            }
        }
    }
    
    /**
     * Converts MQTT exception codes to human-readable messages
     */
    private String getHumanReadableError(org.eclipse.paho.client.mqttv3.MqttException e) {
        int code = e.getReasonCode();
        
        switch (code) {
            case 0: // CONNECTION_LOST
                return "Connection lost to broker";
            case 1: // SUBSCRIBE_FAILED
                return "Subscription failed";
            case 2: // CLIENT_EXCEPTION
                return "Client error: " + e.getMessage();
            case 3: // DISCONNECTED_BUFFER_FULL
                return "Disconnected - buffer full";
            case 4: // NOT_AUTHORIZED
                return "Authentication failed - check username and password";
            case 5: // UNEXPECTED_ERROR
                return "Unexpected error: " + e.getMessage();
            case 32100: // CLIENT_CONNECTED
                return "Client already connected";
            case 32101: // CLIENT_ALREADY_DISCONNECTED
                return "Client already disconnected";
            case 32102: // CLIENT_DISCONNECTING
                return "Client is disconnecting";
            case 32103: // SERVER_CONNECT_ERROR
                return "Cannot connect to broker - check broker URL and network";
            case 32104: // CLIENT_NOT_CONNECTED
                return "Client not connected";
            case 32105: // SOCKET_FACTORY_MISMATCH
                return "TLS configuration error";
            case 32200: // SSL_CONFIG_ERROR
                return "TLS/SSL configuration error - check TLS settings";
            case 32201: // CLIENT_DISCONNECT_PROHIBITED
                return "Disconnect prohibited";
            case 32202: // CONNECT_IN_PROGRESS
                return "Connection already in progress";
            default:
                return "Connection failed: " + e.getMessage();
        }
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
    
    private Map<String, Object> errorResponse(Map<String, Object> errorData) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.putAll(errorData);
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
