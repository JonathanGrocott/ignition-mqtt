package com.inductiveautomation.ignition.examples.mqtt.gateway;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks runtime statistics for the MQTT UNS Publisher module.
 * Thread-safe using atomic operations.
 */
public class ModuleStatistics {
    
    private final AtomicLong messagesPublished = new AtomicLong(0);
    private final AtomicLong messagesFailedToPublish = new AtomicLong(0);
    private final AtomicLong tagReadsSuccessful = new AtomicLong(0);
    private final AtomicLong tagReadsFailed = new AtomicLong(0);
    private final AtomicLong connectionAttempts = new AtomicLong(0);
    private final AtomicLong connectionFailures = new AtomicLong(0);
    private final AtomicLong batchMessagesPublished = new AtomicLong(0);
    private final AtomicLong batchMetricsPublished = new AtomicLong(0);
    private final AtomicLong batchFlushes = new AtomicLong(0);
    private final AtomicLong batchMaxSize = new AtomicLong(0);
    private final long startTimeMs;
    private volatile long lastPublishTimeMs;
    private volatile long lastSuccessfulConnectionMs;
    
    public ModuleStatistics() {
        this.startTimeMs = System.currentTimeMillis();
        this.lastPublishTimeMs = 0;
        this.lastSuccessfulConnectionMs = 0;
    }
    
    // Increment methods
    
    public void incrementMessagesPublished() {
        messagesPublished.incrementAndGet();
        lastPublishTimeMs = System.currentTimeMillis();
    }
    
    public void incrementMessagesFailedToPublish() {
        messagesFailedToPublish.incrementAndGet();
    }
    
    public void incrementTagReadsSuccessful() {
        tagReadsSuccessful.incrementAndGet();
    }
    
    public void incrementTagReadsFailed() {
        tagReadsFailed.incrementAndGet();
    }
    
    public void incrementConnectionAttempts() {
        connectionAttempts.incrementAndGet();
    }
    
    public void incrementConnectionFailures() {
        connectionFailures.incrementAndGet();
    }
    
    public void recordSuccessfulConnection() {
        lastSuccessfulConnectionMs = System.currentTimeMillis();
    }

    public void incrementBatchPublished(int metricCount) {
        if (metricCount <= 0) {
            return;
        }
        batchMessagesPublished.incrementAndGet();
        batchMetricsPublished.addAndGet(metricCount);
        batchFlushes.incrementAndGet();
        updateBatchMaxSize(metricCount);
    }
    
    // Getters
    
    public long getMessagesPublished() {
        return messagesPublished.get();
    }
    
    public long getMessagesFailedToPublish() {
        return messagesFailedToPublish.get();
    }
    
    public long getTagReadsSuccessful() {
        return tagReadsSuccessful.get();
    }
    
    public long getTagReadsFailed() {
        return tagReadsFailed.get();
    }
    
    public long getConnectionAttempts() {
        return connectionAttempts.get();
    }
    
    public long getConnectionFailures() {
        return connectionFailures.get();
    }

    public long getBatchMessagesPublished() {
        return batchMessagesPublished.get();
    }

    public long getBatchMetricsPublished() {
        return batchMetricsPublished.get();
    }

    public long getBatchFlushes() {
        return batchFlushes.get();
    }

    public long getBatchMaxSize() {
        return batchMaxSize.get();
    }
    
    public long getStartTimeMs() {
        return startTimeMs;
    }
    
    public long getLastPublishTimeMs() {
        return lastPublishTimeMs;
    }
    
    public long getLastSuccessfulConnectionMs() {
        return lastSuccessfulConnectionMs;
    }
    
    public long getUptimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }
    
    /**
     * Gets uptime in human-readable format (e.g., "2d 5h 30m")
     */
    public String getUptimeDisplay() {
        long uptimeMs = getUptimeMs();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public long getTimeSinceLastPublishMs() {
        if (lastPublishTimeMs == 0) {
            return -1; // Never published
        }
        return System.currentTimeMillis() - lastPublishTimeMs;
    }
    
    /**
     * Calculates publish success rate as percentage (0-100)
     */
    public double getPublishSuccessRate() {
        long total = messagesPublished.get() + messagesFailedToPublish.get();
        if (total == 0) {
            return 100.0;
        }
        return (messagesPublished.get() * 100.0) / total;
    }
    
    /**
     * Calculates tag read success rate as percentage (0-100)
     */
    public double getTagReadSuccessRate() {
        long total = tagReadsSuccessful.get() + tagReadsFailed.get();
        if (total == 0) {
            return 100.0;
        }
        return (tagReadsSuccessful.get() * 100.0) / total;
    }
    
    /**
     * Calculates connection success rate as percentage (0-100)
     */
    public double getConnectionSuccessRate() {
        long attempts = connectionAttempts.get();
        if (attempts == 0) {
            return 100.0;
        }
        long successes = attempts - connectionFailures.get();
        return (successes * 100.0) / attempts;
    }
    
    /**
     * Resets all statistics
     */
    public void reset() {
        messagesPublished.set(0);
        messagesFailedToPublish.set(0);
        tagReadsSuccessful.set(0);
        tagReadsFailed.set(0);
        connectionAttempts.set(0);
        connectionFailures.set(0);
        batchMessagesPublished.set(0);
        batchMetricsPublished.set(0);
        batchFlushes.set(0);
        batchMaxSize.set(0);
        lastPublishTimeMs = 0;
        lastSuccessfulConnectionMs = 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ModuleStatistics{uptime=%dms, published=%d, failed=%d, successRate=%.1f%%, " +
            "tagReads=%d, tagReadsFailed=%d, connections=%d, connectionFailures=%d}",
            getUptimeMs(),
            messagesPublished.get(),
            messagesFailedToPublish.get(),
            getPublishSuccessRate(),
            tagReadsSuccessful.get(),
            tagReadsFailed.get(),
            connectionAttempts.get(),
            connectionFailures.get()
        );
    }
    
    /**
     * Returns a detailed multi-line statistics report
     */
    public String getDetailedReport() {
        long uptimeSec = getUptimeMs() / 1000;
        long uptimeMin = uptimeSec / 60;
        long uptimeHrs = uptimeMin / 60;
        
        StringBuilder sb = new StringBuilder();
        sb.append("MQTT UNS Publisher Module Statistics\n");
        sb.append("=====================================\n");
        sb.append(String.format("Uptime: %d hours, %d minutes, %d seconds\n", 
            uptimeHrs, uptimeMin % 60, uptimeSec % 60));
        sb.append(String.format("Started: %tF %<tT\n\n", startTimeMs));
        
        sb.append("Publishing:\n");
        sb.append(String.format("  Messages Published: %d\n", messagesPublished.get()));
        sb.append(String.format("  Messages Failed: %d\n", messagesFailedToPublish.get()));
        sb.append(String.format("  Success Rate: %.2f%%\n", getPublishSuccessRate()));
        if (lastPublishTimeMs > 0) {
            sb.append(String.format("  Last Publish: %tF %<tT (%d ms ago)\n", 
                lastPublishTimeMs, getTimeSinceLastPublishMs()));
        } else {
            sb.append("  Last Publish: Never\n");
        }
        sb.append("\n");
        
        sb.append("Tag Reading:\n");
        sb.append(String.format("  Successful Reads: %d\n", tagReadsSuccessful.get()));
        sb.append(String.format("  Failed Reads: %d\n", tagReadsFailed.get()));
        sb.append(String.format("  Success Rate: %.2f%%\n", getTagReadSuccessRate()));
        sb.append("\n");
        
        sb.append("Connections:\n");
        sb.append(String.format("  Connection Attempts: %d\n", connectionAttempts.get()));
        sb.append(String.format("  Connection Failures: %d\n", connectionFailures.get()));
        sb.append(String.format("  Success Rate: %.2f%%\n", getConnectionSuccessRate()));
        if (lastSuccessfulConnectionMs > 0) {
            sb.append(String.format("  Last Successful Connection: %tF %<tT\n", 
                lastSuccessfulConnectionMs));
        }

        if (batchMessagesPublished.get() > 0) {
            sb.append("\nBatch Publishing:\n");
            sb.append(String.format("  Batch Messages Published: %d\n", batchMessagesPublished.get()));
            sb.append(String.format("  Metrics Published: %d\n", batchMetricsPublished.get()));
            sb.append(String.format("  Batch Flushes: %d\n", batchFlushes.get()));
            sb.append(String.format("  Max Batch Size: %d\n", batchMaxSize.get()));
        }
        
        return sb.toString();
    }

    private void updateBatchMaxSize(int metricCount) {
        long current;
        do {
            current = batchMaxSize.get();
            if (metricCount <= current) {
                return;
            }
        } while (!batchMaxSize.compareAndSet(current, metricCount));
    }
}
