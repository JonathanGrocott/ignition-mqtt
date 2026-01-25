package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.examples.mqtt.common.model.ConnectionState;

/**
 * Represents the overall health status of the MQTT UNS Publisher module.
 * Provides diagnostic information for troubleshooting and monitoring.
 */
public class ModuleHealthStatus {
    
    private final boolean healthy;
    private final ConnectionState mqttConnectionState;
    private final int monitoredTagCount;
    private final long messagesPublished;
    private final long messagesFailed;
    private final long uptimeMs;
    private final String statusMessage;
    private final HealthLevel healthLevel;
    
    public enum HealthLevel {
        HEALTHY("Healthy"),
        DEGRADED("Degraded"),
        UNHEALTHY("Unhealthy");
        
        private final String displayName;
        
        HealthLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public ModuleHealthStatus(
            boolean healthy,
            ConnectionState mqttConnectionState,
            int monitoredTagCount,
            long messagesPublished,
            long messagesFailed,
            long uptimeMs,
            String statusMessage,
            HealthLevel healthLevel) {
        this.healthy = healthy;
        this.mqttConnectionState = mqttConnectionState;
        this.monitoredTagCount = monitoredTagCount;
        this.messagesPublished = messagesPublished;
        this.messagesFailed = messagesFailed;
        this.uptimeMs = uptimeMs;
        this.statusMessage = statusMessage;
        this.healthLevel = healthLevel;
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    public ConnectionState getMqttConnectionState() {
        return mqttConnectionState;
    }
    
    public int getMonitoredTagCount() {
        return monitoredTagCount;
    }
    
    public long getMessagesPublished() {
        return messagesPublished;
    }
    
    public long getMessagesFailed() {
        return messagesFailed;
    }
    
    public long getUptimeMs() {
        return uptimeMs;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public HealthLevel getHealthLevel() {
        return healthLevel;
    }
    
    public double getPublishSuccessRate() {
        long total = messagesPublished + messagesFailed;
        if (total == 0) {
            return 100.0;
        }
        return (messagesPublished * 100.0) / total;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ModuleHealthStatus{level=%s, mqtt=%s, tags=%d, published=%d, failed=%d, uptime=%dms, message='%s'}",
            healthLevel.getDisplayName(),
            mqttConnectionState.getDisplayName(),
            monitoredTagCount,
            messagesPublished,
            messagesFailed,
            uptimeMs,
            statusMessage
        );
    }
    
    /**
     * Returns a human-readable health report
     */
    public String getHealthReport() {
        long uptimeSec = uptimeMs / 1000;
        long uptimeMin = uptimeSec / 60;
        long uptimeHrs = uptimeMin / 60;
        
        StringBuilder sb = new StringBuilder();
        sb.append("MQTT UNS Publisher Module Health Check\n");
        sb.append("=======================================\n");
        sb.append(String.format("Status: %s\n", healthLevel.getDisplayName()));
        sb.append(String.format("Message: %s\n\n", statusMessage));
        
        sb.append(String.format("Uptime: %d hours, %d minutes, %d seconds\n", 
            uptimeHrs, uptimeMin % 60, uptimeSec % 60));
        sb.append(String.format("MQTT Connection: %s\n", mqttConnectionState.getDisplayName()));
        sb.append(String.format("Monitored Tags: %d\n", monitoredTagCount));
        sb.append(String.format("Messages Published: %d\n", messagesPublished));
        sb.append(String.format("Messages Failed: %d\n", messagesFailed));
        sb.append(String.format("Publish Success Rate: %.2f%%\n", getPublishSuccessRate()));
        
        return sb.toString();
    }
    
    /**
     * Builder for creating ModuleHealthStatus instances
     */
    public static class Builder {
        private boolean healthy = true;
        private ConnectionState mqttConnectionState = ConnectionState.DISCONNECTED;
        private int monitoredTagCount = 0;
        private long messagesPublished = 0;
        private long messagesFailed = 0;
        private long uptimeMs = 0;
        private String statusMessage = "OK";
        private HealthLevel healthLevel = HealthLevel.HEALTHY;
        
        public Builder healthy(boolean healthy) {
            this.healthy = healthy;
            return this;
        }
        
        public Builder mqttConnectionState(ConnectionState state) {
            this.mqttConnectionState = state;
            return this;
        }
        
        public Builder monitoredTagCount(int count) {
            this.monitoredTagCount = count;
            return this;
        }
        
        public Builder messagesPublished(long count) {
            this.messagesPublished = count;
            return this;
        }
        
        public Builder messagesFailed(long count) {
            this.messagesFailed = count;
            return this;
        }
        
        public Builder uptimeMs(long uptime) {
            this.uptimeMs = uptime;
            return this;
        }
        
        public Builder statusMessage(String message) {
            this.statusMessage = message;
            return this;
        }
        
        public Builder healthLevel(HealthLevel level) {
            this.healthLevel = level;
            return this;
        }
        
        public ModuleHealthStatus build() {
            return new ModuleHealthStatus(
                healthy,
                mqttConnectionState,
                monitoredTagCount,
                messagesPublished,
                messagesFailed,
                uptimeMs,
                statusMessage,
                healthLevel
            );
        }
    }
}
