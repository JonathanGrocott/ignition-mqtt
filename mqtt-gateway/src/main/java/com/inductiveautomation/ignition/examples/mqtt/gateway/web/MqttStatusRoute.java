package com.inductiveautomation.ignition.examples.mqtt.gateway.web;

import com.google.gson.Gson;
import com.inductiveautomation.ignition.examples.mqtt.gateway.ModuleHealthStatus;
import com.inductiveautomation.ignition.examples.mqtt.gateway.ModuleStatistics;
import com.inductiveautomation.ignition.examples.mqtt.gateway.MqttGatewayHook;
import com.inductiveautomation.ignition.examples.mqtt.gateway.MqttPublisherManager;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteHandler;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API route handler for module status and health monitoring.
 * 
 * Endpoints:
 * - GET /data/mqtt-uns-publisher/status - Get module health and statistics
 */
public class MqttStatusRoute implements RouteHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttStatusRoute.class);
    private final MqttGatewayHook gatewayHook;
    private final Gson gson = new Gson();
    
    public MqttStatusRoute(MqttGatewayHook gatewayHook) {
        this.gatewayHook = gatewayHook;
    }
    
    @Override
    public Object handle(RequestContext requestContext, HttpServletResponse response) throws Exception {
        HttpMethod httpMethod = requestContext.getMethod();
        String method = httpMethod.name(); // Convert enum to String
        
        // Set response content type to JSON
        response.setContentType("application/json");
        
        if (!"GET".equals(method)) {
            return gson.toJson(errorResponse("Only GET method is supported"));
        }
        
        Map<String, Object> result = handleGetStatus();
        return gson.toJson(result);
    }
    
    /**
     * GET /status - Returns module health, connection state, and statistics
     */
    private Map<String, Object> handleGetStatus() {
        try {
            ModuleHealthStatus health = gatewayHook.getHealthStatus();
            ModuleStatistics statistics = gatewayHook.getStatistics();
            MqttPublisherManager publisherManager = gatewayHook.getPublisherManager();
            
            Map<String, Object> status = new HashMap<>();
            
            // Health information
            status.put("healthy", health.isHealthy());
            status.put("healthLevel", health.getHealthLevel().name());
            status.put("statusMessage", health.getStatusMessage());
            
            // Connection information
            if (health.getMqttConnectionState() != null) {
                status.put("connectionState", health.getMqttConnectionState().name());
                status.put("connectionStateDisplay", health.getMqttConnectionState().getDisplayName());
            }
            
            if (publisherManager != null) {
                status.put("brokerUrl", publisherManager.getBrokerUrl());
                status.put("reconnectAttempts", publisherManager.getReconnectAttempts());
            }
            
            // Statistics
            Map<String, Object> stats = new HashMap<>();
            if (statistics != null) {
                stats.put("messagesPublished", statistics.getMessagesPublished());
                stats.put("messagesFailed", statistics.getMessagesFailedToPublish());
                stats.put("publishSuccessRate", statistics.getPublishSuccessRate());
                stats.put("tagReadsSuccessful", statistics.getTagReadsSuccessful());
                stats.put("tagReadsFailed", statistics.getTagReadsFailed());
                stats.put("tagReadSuccessRate", statistics.getTagReadSuccessRate());
                stats.put("uptimeMs", statistics.getUptimeMs());
                stats.put("uptimeDisplay", statistics.getUptimeDisplay());
            }
            status.put("statistics", stats);
            
            // Tag monitoring
            status.put("monitoredTagCount", health.getMonitoredTagCount());
            
            return successResponse(status);
            
        } catch (Exception e) {
            logger.error("Error getting module status", e);
            return errorResponse("Failed to get module status: " + e.getMessage());
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
}
