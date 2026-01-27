# Migration Guide: Polling → Event-Driven & JSON Config → Gateway Web UI

This guide explains how to transition the MQTT UNS Publisher module from polling to event-driven tag monitoring and from JSON file configuration to Gateway web UI configuration.

---

## Part 1: Polling → Event-Driven Tag Monitoring

### Current Implementation (Polling-Based)

**How it works:**
- `ScheduledExecutorService` polls tags every `pollRateMs` (default: 1000ms)
- Reads all tags with `GatewayTagManager.readAsync()`
- Compares new values to last published values
- Publishes if value change exceeds deadband or quality changes

**Advantages:**
- ✅ Simple and reliable
- ✅ Proven to work in Ignition 8.3
- ✅ Easy to debug
- ✅ Configurable poll rate

**Disadvantages:**
- ❌ Slight latency (up to poll rate duration)
- ❌ Wastes CPU on tags that don't change
- ❌ Not truly real-time

### Proposed Event-Driven Implementation

**How it would work:**
- Subscribe to tag changes with `GatewayTagManager.subscribeAsync()`
- Receive callbacks immediately when tags change
- Publish directly from callback (no comparison needed)

**Advantages:**
- ✅ True real-time (instant notifications)
- ✅ More efficient (no unnecessary polling)
- ✅ Lower CPU usage for slow-changing tags

**Disadvantages:**
- ❌ More complex error handling
- ❌ Requires careful threading (callbacks on different threads)
- ❌ May be less stable in Ignition 8.3 (reason for current polling approach)

---

### Migration Steps

#### Step 1: Research the Event-Driven API

The planning docs reference `subscribeAsync()` but it's not implemented. You need to:

1. **Test if the API exists:**
```java
GatewayTagManager tagManager = gatewayContext.getTagManager();

// Check if subscribeAsync() method exists
try {
    Method subscribeMethod = tagManager.getClass().getMethod(
        "subscribeAsync",
        List.class,  // List<TagPath>
        BiConsumer.class  // BiConsumer<TagPath, TagChangeEvent>
    );
    logger.info("subscribeAsync() method found!");
} catch (NoSuchMethodException e) {
    logger.warn("subscribeAsync() method not available in this SDK version");
}
```

2. **Check Ignition SDK documentation** (if accessible):
   - https://www.sdk-docs.inductiveautomation.com/docs/8.3/
   - Search for "tag subscription" or "tag change listener"

3. **Check SDK examples repository**:
   - https://github.com/inductiveautomation/ignition-sdk-examples
   - Branch: `ignition-8.3`
   - Look for tag subscription examples

#### Step 2: Create Event-Driven Tag Subscription Manager

If the API exists, create a new implementation:

**File:** `mqtt-gateway/src/main/java/com/inductiveautomation/ignition/examples/mqtt/gateway/EventDrivenTagSubscriptionManager.java`

```java
package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TagPublishConfig;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Event-driven tag subscription manager.
 * Subscribes to tag changes and publishes immediately when values change.
 */
public class EventDrivenTagSubscriptionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(EventDrivenTagSubscriptionManager.class);
    
    private final GatewayContext gatewayContext;
    private final MqttPublisherManager publisherManager;
    private final MqttTopicMapper topicMapper;
    private final JsonPayloadBuilder payloadBuilder;
    private final ModuleStatistics statistics;
    
    private TagPublishConfig config;
    private final Map<TagPath, QualifiedValue> lastPublishedValues = new ConcurrentHashMap<>();
    private final List<TagPath> monitoredTags = Collections.synchronizedList(new ArrayList<>());
    
    // Store subscription future for cleanup
    private CompletableFuture<?> subscriptionFuture;
    
    public EventDrivenTagSubscriptionManager(
            GatewayContext context, 
            MqttPublisherManager publisherManager,
            ModuleStatistics statistics) {
        this.gatewayContext = context;
        this.publisherManager = publisherManager;
        this.statistics = statistics;
        this.topicMapper = new MqttTopicMapper();
        this.payloadBuilder = new JsonPayloadBuilder();
    }
    
    /**
     * Starts monitoring tags with event-driven subscriptions
     */
    public void start(TagPublishConfig config) {
        this.config = config;
        
        if (!config.isEnabled()) {
            logger.info("Tag publishing is disabled");
            return;
        }
        
        logger.info("Starting event-driven tag subscription manager");
        
        // Discover tags to monitor
        List<TagPath> tags = discoverTags();
        
        if (tags.isEmpty()) {
            logger.warn("No tags found to monitor. Check your configuration.");
            return;
        }
        
        monitoredTags.addAll(tags);
        logger.info("Subscribing to {} tags", monitoredTags.size());
        
        // Subscribe to tag changes
        subscribeToTags();
    }
    
    /**
     * Subscribes to tag changes using event-driven API
     */
    private void subscribeToTags() {
        try {
            GatewayTagManager tagManager = gatewayContext.getTagManager();
            
            // Create tag change handler
            BiConsumer<TagPath, QualifiedValue> changeHandler = (tagPath, newValue) -> {
                handleTagChange(tagPath, newValue);
            };
            
            // Subscribe to all monitored tags
            // NOTE: This API may not exist or may have different signature
            // You'll need to adjust based on actual SDK
            subscriptionFuture = tagManager.subscribeAsync(monitoredTags, changeHandler);
            
            logger.info("Successfully subscribed to {} tags", monitoredTags.size());
            
        } catch (Exception e) {
            logger.error("Failed to subscribe to tags. Falling back to polling?", e);
            // Could fall back to polling here if event-driven fails
        }
    }
    
    /**
     * Handles a tag value change event
     */
    private void handleTagChange(TagPath tagPath, QualifiedValue newValue) {
        try {
            // Check if we should publish this change
            if (shouldPublish(tagPath, newValue)) {
                publishTagValue(tagPath, newValue);
                lastPublishedValues.put(tagPath, newValue);
            }
        } catch (Exception e) {
            logger.error("Error handling tag change for {}: {}", tagPath, e.getMessage(), e);
            statistics.incrementTagReadsFailed();
        }
    }
    
    /**
     * Determines if a tag value should be published
     * (Same logic as polling version)
     */
    private boolean shouldPublish(TagPath tagPath, QualifiedValue newValue) {
        QualifiedValue lastValue = lastPublishedValues.get(tagPath);
        
        // First read - always publish
        if (lastValue == null) {
            return true;
        }
        
        // Check quality change
        if (config.isPublishOnQualityChange()) {
            if (!Objects.equals(lastValue.getQuality(), newValue.getQuality())) {
                return true;
            }
        }
        
        // Check value deadband
        double deadband = config.getValueDeadband();
        if (deadband > 0.0) {
            if (!exceedsDeadband(lastValue.getValue(), newValue.getValue(), deadband)) {
                return false;
            }
        } else {
            if (Objects.equals(lastValue.getValue(), newValue.getValue())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if value change exceeds deadband
     */
    private boolean exceedsDeadband(Object oldValue, Object newValue, double deadband) {
        if (oldValue instanceof Number && newValue instanceof Number) {
            double oldNum = ((Number) oldValue).doubleValue();
            double newNum = ((Number) newValue).doubleValue();
            double delta = Math.abs(newNum - oldNum);
            return delta > deadband;
        }
        return !Objects.equals(oldValue, newValue);
    }
    
    /**
     * Publishes a tag value to MQTT
     * (Same as polling version)
     */
    private void publishTagValue(TagPath tagPath, QualifiedValue value) {
        try {
            // Map tag to topic
            String topic = topicMapper.mapTagToTopic(tagPath);
            
            // Check for custom topic override
            Map<String, String> topicOverrides = config.getTopicOverrides();
            if (topicOverrides != null && topicOverrides.containsKey(tagPath.toStringFull())) {
                String customTopic = topicOverrides.get(tagPath.toStringFull());
                topic = topicMapper.applyTopicOverride(tagPath, customTopic);
            }
            
            // Build payload
            String payload = payloadBuilder.buildPayload(tagPath, value, config.isIncludeMetadata());
            
            // Publish to MQTT
            publisherManager.publish(topic, payload);
            
            logger.trace("Published {}: {} to {}", tagPath, value.getValue(), topic);
            
        } catch (Exception e) {
            logger.error("Error publishing tag {}: {}", tagPath, e.getMessage());
        }
    }
    
    /**
     * Discovers tags to monitor
     * (Same as polling version - use existing TagSubscriptionManager code)
     */
    private List<TagPath> discoverTags() {
        // Copy implementation from TagSubscriptionManager.discoverTags()
        // and browseTagsRecursive()
        return new ArrayList<>();  // TODO: Implement
    }
    
    /**
     * Shuts down the subscription manager
     */
    public void shutdown() {
        logger.info("Shutting down event-driven tag subscription manager");
        
        // Cancel subscription
        if (subscriptionFuture != null && !subscriptionFuture.isDone()) {
            subscriptionFuture.cancel(true);
        }
        
        monitoredTags.clear();
        lastPublishedValues.clear();
        
        logger.info("Event-driven tag subscription manager shut down");
    }
    
    /**
     * Gets the number of tags currently being monitored
     */
    public int getMonitoredTagCount() {
        return monitoredTags.size();
    }
}
```

#### Step 3: Add Configuration Toggle

Add a setting to choose between polling and event-driven:

**Update TagPublishConfig.java:**
```java
@SerializedName("subscriptionMode")
private SubscriptionMode subscriptionMode;

public enum SubscriptionMode {
    POLLING,       // Use scheduled polling
    EVENT_DRIVEN   // Use event subscriptions
}

// In constructor:
this.subscriptionMode = SubscriptionMode.POLLING;  // Default to polling
```

#### Step 4: Update MqttGatewayHook

```java
@Override
public void setup(GatewayContext context) {
    this.gatewayContext = context;
    this.statistics = new ModuleStatistics();
    this.configManager = new ConfigurationManager(context);
    this.publisherManager = new MqttPublisherManager(statistics);
    
    // Create both managers (lazy initialization)
    // We'll choose which one to use in startup()
}

@Override
public void startup(LicenseState activationState) {
    // ... load config ...
    
    if (tagConfig != null && tagConfig.isEnabled()) {
        try {
            tagConfig.validate();
            
            // Choose subscription manager based on config
            if (tagConfig.getSubscriptionMode() == SubscriptionMode.EVENT_DRIVEN) {
                logger.info("Using event-driven tag subscriptions");
                this.tagSubscriptionManager = new EventDrivenTagSubscriptionManager(
                    gatewayContext, publisherManager, statistics
                );
            } else {
                logger.info("Using polling-based tag subscriptions");
                this.tagSubscriptionManager = new TagSubscriptionManager(
                    gatewayContext, publisherManager, statistics
                );
            }
            
            tagSubscriptionManager.start(tagConfig);
            
        } catch (Exception e) {
            logger.error("Failed to start tag subscriptions: {}", e.getMessage(), e);
        }
    }
}
```

#### Step 5: Test Thoroughly

**Test cases:**
1. Start with polling mode (proven to work)
2. Switch to event-driven mode
3. Compare latency and CPU usage
4. Test with high-frequency changes
5. Test with many tags (1000+)
6. Test reconnection scenarios
7. Test tag quality changes
8. Test deadband filtering

**Fallback Strategy:**
If event-driven doesn't work reliably, keep polling as default and make event-driven opt-in.

---

## Part 2: JSON Config → Gateway Web UI

### Current Implementation (JSON File-Based)

**How it works:**
- Configuration stored in `<ignition-data>/mqtt-uns-config.json`
- `ConfigurationManager` loads/saves using Gson
- Manual editing required
- No validation until module restart

**Advantages:**
- ✅ Simple to implement
- ✅ Version control friendly
- ✅ Easy to backup/restore

**Disadvantages:**
- ❌ Not user-friendly
- ❌ No runtime validation
- ❌ Requires Gateway restart to apply changes
- ❌ No visual feedback

### Proposed Web UI Implementation

**Components needed:**
1. REST API backend (`AbstractApiRoute`)
2. HTML/CSS/JavaScript frontend
3. Config Resource API (Ignition 8.3 feature)
4. Secret storage for passwords

---

### Migration Steps

#### Step 1: Create REST API Routes

**File:** `mqtt-gateway/src/main/java/com/inductiveautomation/ignition/examples/mqtt/gateway/web/MqttConfigRoute.java`

```java
package com.inductiveautomation.ignition.examples.mqtt.gateway.web;

import com.google.gson.Gson;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttModuleConfig;
import com.inductiveautomation.ignition.examples.mqtt.gateway.config.ConfigurationManager;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.routes.AbstractApiRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * REST API route for MQTT module configuration.
 * 
 * GET  /data/mqtt-module/config - Load current configuration
 * POST /data/mqtt-module/config - Save new configuration
 */
public class MqttConfigRoute extends AbstractApiRoute {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttConfigRoute.class);
    private final Gson gson = new Gson();
    private final ConfigurationManager configManager;
    
    public MqttConfigRoute(GatewayContext context) {
        this.configManager = new ConfigurationManager(context);
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        try {
            // Load configuration
            MqttModuleConfig config = configManager.loadModuleConfig();
            
            if (config == null) {
                // Return default empty config
                config = new MqttModuleConfig();
            }
            
            // Serialize to JSON
            String json = gson.toJson(config);
            
            // Send response
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json);
            
        } catch (Exception e) {
            logger.error("Error loading configuration", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                     "Failed to load configuration: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        try {
            // Parse JSON body
            MqttModuleConfig config = gson.fromJson(req.getReader(), MqttModuleConfig.class);
            
            // Validate configuration
            try {
                if (config.getBroker() != null) {
                    config.getBroker().validate();
                }
                if (config.getTags() != null) {
                    config.getTags().validate();
                }
            } catch (IllegalArgumentException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, 
                         "Invalid configuration: " + e.getMessage());
                return;
            }
            
            // Save configuration
            configManager.saveModuleConfig(config);
            
            logger.info("Configuration saved successfully");
            
            // Send success response
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"success\": true, \"message\": \"Configuration saved successfully\"}");
            
        } catch (Exception e) {
            logger.error("Error saving configuration", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                     "Failed to save configuration: " + e.getMessage());
        }
    }
    
    /**
     * Helper to send error responses
     */
    private void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }
}
```

Create additional routes for other functionality:

**File:** `mqtt-gateway/src/main/java/com/inductiveautomation/ignition/examples/mqtt/gateway/web/MqttStatusRoute.java`

```java
public class MqttStatusRoute extends AbstractApiRoute {
    
    private final MqttGatewayHook moduleHook;
    private final Gson gson = new Gson();
    
    public MqttStatusRoute(MqttGatewayHook hook) {
        this.moduleHook = hook;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        try {
            // Get health status
            ModuleHealthStatus health = moduleHook.getHealthStatus();
            
            // Get statistics
            ModuleStatistics stats = moduleHook.getStatistics();
            
            // Create status response
            Map<String, Object> status = new HashMap<>();
            status.put("health", health);
            status.put("statistics", stats);
            status.put("connectionState", moduleHook.getPublisherManager().getConnectionState());
            status.put("monitoredTagCount", moduleHook.getTagSubscriptionManager().getMonitoredTagCount());
            
            // Send JSON response
            String json = gson.toJson(status);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json);
            
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
```

**File:** `mqtt-gateway/src/main/java/com/inductiveautomation/ignition/examples/mqtt/gateway/web/TagBrowseRoute.java`

```java
public class TagBrowseRoute extends AbstractApiRoute {
    
    private final GatewayContext context;
    private final Gson gson = new Gson();
    
    public TagBrowseRoute(GatewayContext context) {
        this.context = context;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        try {
            String provider = req.getParameter("provider");
            String path = req.getParameter("path");
            
            if (provider == null) {
                // Return list of tag providers
                List<String> providers = getTagProviders();
                String json = gson.toJson(providers);
                resp.setContentType("application/json");
                resp.getWriter().write(json);
                return;
            }
            
            // Browse tags at the given path
            List<TagInfo> tags = browseTags(provider, path);
            String json = gson.toJson(tags);
            
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json);
            
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    private List<String> getTagProviders() {
        // Get list of tag providers from GatewayContext
        // Implementation depends on SDK API
        return new ArrayList<>();
    }
    
    private List<TagInfo> browseTags(String provider, String path) {
        // Browse tags using GatewayTagManager
        // Similar to TagSubscriptionManager.browseTagsRecursive()
        return new ArrayList<>();
    }
    
    static class TagInfo {
        String name;
        String path;
        boolean isFolder;
        String dataType;
    }
}
```

#### Step 2: Register Routes in Module Hook

Update `MqttGatewayHook.java`:

```java
@Override
public void mountRouteHandlers(RouteGroup routes) {
    routes.newRoute("/mqtt-module/config")
          .handler(new MqttConfigRoute(gatewayContext))
          .mount();
          
    routes.newRoute("/mqtt-module/status")
          .handler(new MqttStatusRoute(this))
          .mount();
          
    routes.newRoute("/mqtt-module/browse")
          .handler(new TagBrowseRoute(gatewayContext))
          .mount();
          
    routes.newRoute("/mqtt-module/test-connection")
          .handler(new TestConnectionRoute(gatewayContext))
          .mount();
          
    logger.info("Mounted MQTT module web routes");
}

@Override
public Optional<String> getMountedResourceFolder() {
    return Optional.of("mounted");
}

@Override
public Optional<String> getMountPathAlias() {
    return Optional.of("mqtt-module");
}
```

#### Step 3: Create Web UI Resources

**File:** `mqtt-gateway/src/main/resources/mounted/index.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MQTT UNS Publisher Configuration</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div class="container">
        <h1>MQTT UNS Publisher Configuration</h1>
        
        <!-- Tab Navigation -->
        <div class="tabs">
            <button class="tab-button active" onclick="showTab('broker')">Broker Settings</button>
            <button class="tab-button" onclick="showTab('tags')">Tag Selection</button>
            <button class="tab-button" onclick="showTab('topics')">Topic Mapping</button>
            <button class="tab-button" onclick="showTab('status')">Status</button>
        </div>
        
        <!-- Broker Settings Tab -->
        <div id="broker-tab" class="tab-content active">
            <h2>MQTT Broker Settings</h2>
            
            <div class="form-group">
                <label for="brokerUrl">Broker URL</label>
                <input type="text" id="brokerUrl" placeholder="tcp://localhost:1883">
                <small>Format: tcp://hostname:port or ssl://hostname:port</small>
            </div>
            
            <div class="form-group">
                <label for="clientId">Client ID</label>
                <input type="text" id="clientId" placeholder="ignition-mqtt-publisher">
            </div>
            
            <div class="form-group">
                <label for="username">Username (optional)</label>
                <input type="text" id="username">
            </div>
            
            <div class="form-group">
                <label for="password">Password (optional)</label>
                <input type="password" id="password">
            </div>
            
            <div class="form-group">
                <label for="qos">Quality of Service</label>
                <select id="qos">
                    <option value="0">0 - At most once</option>
                    <option value="1" selected>1 - At least once</option>
                    <option value="2">2 - Exactly once</option>
                </select>
            </div>
            
            <div class="form-group checkbox">
                <label>
                    <input type="checkbox" id="retained">
                    Retain messages on broker
                </label>
            </div>
            
            <div class="form-group checkbox">
                <label>
                    <input type="checkbox" id="cleanSession" checked>
                    Clean session
                </label>
            </div>
            
            <button class="btn-primary" onclick="testConnection()">Test Connection</button>
            <div id="connection-test-result"></div>
        </div>
        
        <!-- Tag Selection Tab -->
        <div id="tags-tab" class="tab-content">
            <h2>Tag Selection</h2>
            
            <div class="form-group">
                <label for="tagProviders">Tag Providers</label>
                <select id="tagProviders" multiple size="5">
                    <!-- Populated by JavaScript -->
                </select>
            </div>
            
            <div class="form-group">
                <label>Tag Folders</label>
                <div id="tag-browser">
                    <!-- Tag tree populated by JavaScript -->
                </div>
            </div>
            
            <div class="form-group">
                <label>Selected Folders</label>
                <ul id="selected-folders">
                    <!-- Populated by JavaScript -->
                </ul>
            </div>
            
            <div class="form-group">
                <label for="pollRateMs">Poll Rate (ms)</label>
                <input type="number" id="pollRateMs" value="1000" min="100" max="60000" step="100">
                <small>Minimum: 100ms, Maximum: 60000ms</small>
            </div>
            
            <div class="form-group">
                <label for="valueDeadband">Value Deadband</label>
                <input type="number" id="valueDeadband" value="0.1" min="0" step="0.01">
                <small>Minimum value change to trigger publish</small>
            </div>
            
            <div class="form-group checkbox">
                <label>
                    <input type="checkbox" id="publishOnQualityChange" checked>
                    Publish on quality change
                </label>
            </div>
            
            <div class="form-group checkbox">
                <label>
                    <input type="checkbox" id="includeMetadata" checked>
                    Include metadata in payload
                </label>
            </div>
        </div>
        
        <!-- Topic Mapping Tab -->
        <div id="topics-tab" class="tab-content">
            <h2>Topic Mapping</h2>
            
            <p>Default mapping converts tag paths to MQTT topics:</p>
            <div class="example">
                <code>[default]TestTags/Temperature</code> → <code>default/testtags/temperature</code>
            </div>
            
            <h3>Custom Topic Overrides</h3>
            <table id="topic-overrides-table">
                <thead>
                    <tr>
                        <th>Tag Path</th>
                        <th>MQTT Topic</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="topic-overrides-body">
                    <!-- Populated by JavaScript -->
                </tbody>
            </table>
            
            <button class="btn-secondary" onclick="addTopicOverride()">Add Override</button>
        </div>
        
        <!-- Status Tab -->
        <div id="status-tab" class="tab-content">
            <h2>Module Status</h2>
            
            <div class="status-panel">
                <h3>Connection Status</h3>
                <div id="connection-status" class="status-indicator">
                    <span class="status-dot"></span>
                    <span class="status-text">Loading...</span>
                </div>
            </div>
            
            <div class="status-panel">
                <h3>Statistics</h3>
                <table class="stats-table">
                    <tr>
                        <td>Monitored Tags:</td>
                        <td id="stat-monitored-tags">-</td>
                    </tr>
                    <tr>
                        <td>Messages Published:</td>
                        <td id="stat-messages-published">-</td>
                    </tr>
                    <tr>
                        <td>Messages Failed:</td>
                        <td id="stat-messages-failed">-</td>
                    </tr>
                    <tr>
                        <td>Publish Success Rate:</td>
                        <td id="stat-success-rate">-</td>
                    </tr>
                    <tr>
                        <td>Uptime:</td>
                        <td id="stat-uptime">-</td>
                    </tr>
                </table>
            </div>
            
            <div class="status-panel">
                <h3>Health</h3>
                <div id="health-status">
                    <div class="health-indicator">
                        <span id="health-level">-</span>
                    </div>
                    <p id="health-message">-</p>
                </div>
            </div>
            
            <button class="btn-secondary" onclick="refreshStatus()">Refresh Status</button>
        </div>
        
        <!-- Save/Cancel Buttons -->
        <div class="actions">
            <button class="btn-primary" onclick="saveConfiguration()">Save Configuration</button>
            <button class="btn-secondary" onclick="loadConfiguration()">Cancel</button>
        </div>
        
        <div id="save-result"></div>
    </div>
    
    <script src="config.js"></script>
</body>
</html>
```

**File:** `mqtt-gateway/src/main/resources/mounted/config.js`

```javascript
// Global configuration object
let config = {
    broker: {},
    tags: {}
};

// Load configuration on page load
window.addEventListener('DOMContentLoaded', () => {
    loadConfiguration();
    loadTagProviders();
    refreshStatus();
    
    // Auto-refresh status every 5 seconds
    setInterval(refreshStatus, 5000);
});

/**
 * Load configuration from server
 */
async function loadConfiguration() {
    try {
        const response = await fetch('/data/mqtt-module/config');
        if (!response.ok) {
            throw new Error('Failed to load configuration');
        }
        
        config = await response.json();
        
        // Populate form fields
        populateForm(config);
        
    } catch (error) {
        console.error('Error loading configuration:', error);
        alert('Failed to load configuration: ' + error.message);
    }
}

/**
 * Populate form fields with configuration data
 */
function populateForm(config) {
    // Broker settings
    if (config.broker) {
        document.getElementById('brokerUrl').value = config.broker.brokerUrl || '';
        document.getElementById('clientId').value = config.broker.clientId || '';
        document.getElementById('username').value = config.broker.username || '';
        // Don't populate password for security
        document.getElementById('qos').value = config.broker.qos || 1;
        document.getElementById('retained').checked = config.broker.retained || false;
        document.getElementById('cleanSession').checked = config.broker.cleanSession !== false;
    }
    
    // Tag settings
    if (config.tags) {
        document.getElementById('pollRateMs').value = config.tags.pollRateMs || 1000;
        document.getElementById('valueDeadband').value = config.tags.valueDeadband || 0.1;
        document.getElementById('publishOnQualityChange').checked = config.tags.publishOnQualityChange !== false;
        document.getElementById('includeMetadata').checked = config.tags.includeMetadata !== false;
        
        // Populate selected folders
        populateSelectedFolders(config.tags.tagFolders || []);
        
        // Populate topic overrides
        populateTopicOverrides(config.tags.topicOverrides || {});
    }
}

/**
 * Collect configuration from form
 */
function collectConfiguration() {
    return {
        broker: {
            brokerUrl: document.getElementById('brokerUrl').value,
            clientId: document.getElementById('clientId').value,
            username: document.getElementById('username').value,
            password: document.getElementById('password').value,  // Only if changed
            qos: parseInt(document.getElementById('qos').value),
            retained: document.getElementById('retained').checked,
            cleanSession: document.getElementById('cleanSession').checked,
            connectionTimeout: 30,
            keepAliveInterval: 60
        },
        tags: {
            enabled: true,
            tagProviders: getSelectedTagProviders(),
            tagFolders: getSelectedTagFolders(),
            pollRateMs: parseInt(document.getElementById('pollRateMs').value),
            valueDeadband: parseFloat(document.getElementById('valueDeadband').value),
            publishOnQualityChange: document.getElementById('publishOnQualityChange').checked,
            includeMetadata: document.getElementById('includeMetadata').checked,
            topicOverrides: getTopicOverrides()
        }
    };
}

/**
 * Save configuration to server
 */
async function saveConfiguration() {
    try {
        const newConfig = collectConfiguration();
        
        const response = await fetch('/data/mqtt-module/config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(newConfig)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to save configuration');
        }
        
        const result = await response.json();
        
        document.getElementById('save-result').innerHTML = 
            '<div class="success">Configuration saved successfully!</div>';
        
        // Reload configuration
        setTimeout(loadConfiguration, 1000);
        
    } catch (error) {
        console.error('Error saving configuration:', error);
        document.getElementById('save-result').innerHTML = 
            '<div class="error">Failed to save configuration: ' + error.message + '</div>';
    }
}

/**
 * Test MQTT connection
 */
async function testConnection() {
    try {
        const brokerConfig = {
            brokerUrl: document.getElementById('brokerUrl').value,
            clientId: document.getElementById('clientId').value,
            username: document.getElementById('username').value,
            password: document.getElementById('password').value
        };
        
        const response = await fetch('/data/mqtt-module/test-connection', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(brokerConfig)
        });
        
        const result = await response.json();
        
        if (result.success) {
            document.getElementById('connection-test-result').innerHTML = 
                '<div class="success">Connection successful!</div>';
        } else {
            document.getElementById('connection-test-result').innerHTML = 
                '<div class="error">Connection failed: ' + result.error + '</div>';
        }
        
    } catch (error) {
        document.getElementById('connection-test-result').innerHTML = 
            '<div class="error">Connection test failed: ' + error.message + '</div>';
    }
}

/**
 * Load tag providers
 */
async function loadTagProviders() {
    try {
        const response = await fetch('/data/mqtt-module/browse');
        const providers = await response.json();
        
        const select = document.getElementById('tagProviders');
        select.innerHTML = '';
        
        providers.forEach(provider => {
            const option = document.createElement('option');
            option.value = provider;
            option.textContent = provider;
            select.appendChild(option);
        });
        
    } catch (error) {
        console.error('Error loading tag providers:', error);
    }
}

/**
 * Refresh status information
 */
async function refreshStatus() {
    try {
        const response = await fetch('/data/mqtt-module/status');
        const status = await response.json();
        
        // Update connection status
        updateConnectionStatus(status.connectionState);
        
        // Update statistics
        updateStatistics(status.statistics);
        
        // Update health
        updateHealth(status.health);
        
    } catch (error) {
        console.error('Error refreshing status:', error);
    }
}

/**
 * Update connection status display
 */
function updateConnectionStatus(state) {
    const statusDiv = document.getElementById('connection-status');
    const dot = statusDiv.querySelector('.status-dot');
    const text = statusDiv.querySelector('.status-text');
    
    dot.className = 'status-dot status-' + state.toLowerCase();
    text.textContent = state;
}

/**
 * Update statistics display
 */
function updateStatistics(stats) {
    if (!stats) return;
    
    document.getElementById('stat-monitored-tags').textContent = stats.monitoredTagCount || 0;
    document.getElementById('stat-messages-published').textContent = stats.messagesPublished || 0;
    document.getElementById('stat-messages-failed').textContent = stats.messagesFailedToPublish || 0;
    
    const successRate = stats.publishSuccessRate || 100;
    document.getElementById('stat-success-rate').textContent = successRate.toFixed(2) + '%';
    
    const uptime = formatUptime(stats.uptimeMs || 0);
    document.getElementById('stat-uptime').textContent = uptime;
}

/**
 * Update health display
 */
function updateHealth(health) {
    if (!health) return;
    
    document.getElementById('health-level').textContent = health.healthLevel;
    document.getElementById('health-message').textContent = health.statusMessage;
    
    const healthDiv = document.querySelector('.health-indicator');
    healthDiv.className = 'health-indicator health-' + health.healthLevel.toLowerCase();
}

/**
 * Format uptime in human-readable form
 */
function formatUptime(ms) {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) {
        return `${days}d ${hours % 24}h ${minutes % 60}m`;
    } else if (hours > 0) {
        return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
        return `${minutes}m ${seconds % 60}s`;
    } else {
        return `${seconds}s`;
    }
}

/**
 * Tab switching
 */
function showTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Remove active from all buttons
    document.querySelectorAll('.tab-button').forEach(button => {
        button.classList.remove('active');
    });
    
    // Show selected tab
    document.getElementById(tabName + '-tab').classList.add('active');
    
    // Mark button as active
    event.target.classList.add('active');
}

// Additional helper functions for tag browsing, topic overrides, etc.
// ... (implementation details)
```

**File:** `mqtt-gateway/src/main/resources/mounted/styles.css`

```css
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    font-size: 14px;
    line-height: 1.6;
    color: #333;
    background-color: #f5f5f5;
    padding: 20px;
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    background: white;
    padding: 30px;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

h1 {
    color: #2c3e50;
    margin-bottom: 30px;
    font-size: 28px;
}

h2 {
    color: #34495e;
    margin-bottom: 20px;
    font-size: 20px;
}

h3 {
    color: #34495e;
    margin-bottom: 15px;
    font-size: 16px;
}

/* Tabs */
.tabs {
    display: flex;
    border-bottom: 2px solid #e0e0e0;
    margin-bottom: 30px;
}

.tab-button {
    background: none;
    border: none;
    padding: 12px 24px;
    cursor: pointer;
    font-size: 14px;
    color: #666;
    border-bottom: 2px solid transparent;
    margin-bottom: -2px;
    transition: all 0.3s;
}

.tab-button:hover {
    color: #2c3e50;
}

.tab-button.active {
    color: #3498db;
    border-bottom-color: #3498db;
    font-weight: 500;
}

.tab-content {
    display: none;
}

.tab-content.active {
    display: block;
}

/* Forms */
.form-group {
    margin-bottom: 20px;
}

.form-group label {
    display: block;
    margin-bottom: 5px;
    font-weight: 500;
    color: #555;
}

.form-group input[type="text"],
.form-group input[type="password"],
.form-group input[type="number"],
.form-group select {
    width: 100%;
    padding: 10px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
}

.form-group input:focus,
.form-group select:focus {
    outline: none;
    border-color: #3498db;
}

.form-group small {
    display: block;
    margin-top: 5px;
    color: #777;
    font-size: 12px;
}

.form-group.checkbox label {
    display: flex;
    align-items: center;
    font-weight: normal;
}

.form-group.checkbox input {
    width: auto;
    margin-right: 8px;
}

/* Buttons */
.btn-primary,
.btn-secondary {
    padding: 10px 20px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: background-color 0.3s;
}

.btn-primary {
    background-color: #3498db;
    color: white;
}

.btn-primary:hover {
    background-color: #2980b9;
}

.btn-secondary {
    background-color: #95a5a6;
    color: white;
}

.btn-secondary:hover {
    background-color: #7f8c8d;
}

/* Status indicators */
.status-indicator {
    display: flex;
    align-items: center;
    gap: 10px;
}

.status-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background-color: #95a5a6;
}

.status-dot.status-connected {
    background-color: #27ae60;
}

.status-dot.status-disconnected {
    background-color: #e74c3c;
}

.status-dot.status-connecting,
.status-dot.status-reconnecting {
    background-color: #f39c12;
}

/* Status panels */
.status-panel {
    background: #f9f9f9;
    padding: 20px;
    border-radius: 4px;
    margin-bottom: 20px;
}

.stats-table {
    width: 100%;
}

.stats-table td {
    padding: 8px 0;
}

.stats-table td:first-child {
    font-weight: 500;
    color: #555;
}

.stats-table td:last-child {
    text-align: right;
    color: #333;
}

/* Health indicator */
.health-indicator {
    padding: 15px;
    border-radius: 4px;
    text-align: center;
    font-size: 18px;
    font-weight: 500;
    margin-bottom: 10px;
}

.health-indicator.health-healthy {
    background-color: #d4edda;
    color: #155724;
}

.health-indicator.health-degraded {
    background-color: #fff3cd;
    color: #856404;
}

.health-indicator.health-unhealthy {
    background-color: #f8d7da;
    color: #721c24;
}

/* Messages */
.success {
    padding: 12px;
    background-color: #d4edda;
    color: #155724;
    border: 1px solid #c3e6cb;
    border-radius: 4px;
    margin-top: 15px;
}

.error {
    padding: 12px;
    background-color: #f8d7da;
    color: #721c24;
    border: 1px solid #f5c6cb;
    border-radius: 4px;
    margin-top: 15px;
}

/* Actions */
.actions {
    margin-top: 30px;
    display: flex;
    gap: 10px;
}

/* Example code blocks */
.example {
    background: #f5f5f5;
    padding: 15px;
    border-radius: 4px;
    margin: 15px 0;
    font-family: monospace;
    font-size: 13px;
}

.example code {
    color: #e74c3c;
}

/* Tables */
table {
    width: 100%;
    border-collapse: collapse;
    margin: 15px 0;
}

table th,
table td {
    padding: 10px;
    text-align: left;
    border-bottom: 1px solid #ddd;
}

table th {
    background-color: #f5f5f5;
    font-weight: 500;
    color: #555;
}
```

#### Step 4: Build and Test

1. **Build the module:**
```bash
./build.sh
```

2. **Install on Ignition Gateway**

3. **Access the web UI:**
```
http://localhost:8088/data/mqtt-module/index.html
```

4. **Test all functionality:**
   - Load configuration
   - Edit broker settings
   - Test connection
   - Browse tags
   - Save configuration
   - View status and statistics

#### Step 5: Add Config Resource API (Optional - More Advanced)

Replace JSON file storage with Ignition's Config Resource API for better integration.

See planning docs lines 651-671 for implementation details.

---

## Conclusion

**For Polling → Event-Driven:**
- Research if `subscribeAsync()` API exists and is stable
- Implement event-driven manager alongside polling
- Make it configurable (users choose)
- Keep polling as fallback/default

**For JSON Config → Web UI:**
- Create REST API routes with `AbstractApiRoute`
- Build HTML/CSS/JS frontend
- Mount resources in module hook
- Test thoroughly before deprecating JSON config
- Consider keeping both options for power users

**Recommended Approach:**
1. Start with Web UI (more user value)
2. Keep polling (proven reliable)
3. Add event-driven as experimental feature later
4. Test everything thoroughly on Ignition Standard Edition

Let me know if you want me to implement any of these changes!
