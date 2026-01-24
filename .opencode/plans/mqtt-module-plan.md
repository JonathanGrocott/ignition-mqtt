# Ignition 8.3 MQTT Unified Namespace Module - Development Plan

## Executive Summary

This plan outlines the development of an Ignition 8.3 SDK module that enables publishing tags to an external MQTT broker in a Unified Namespace (UNS) structure. The module will publish tag data as JSON payloads with configurable topic mappings and customizable payload formats.

## Project Overview

### Module Purpose
Create a gateway-scoped module that:
- Monitors selected tag providers/folders for tag value changes
- Publishes tag data (value, quality, timestamp, metadata) as JSON to MQTT broker
- Supports configurable MQTT topic structure based on tag paths
- Allows users to customize JSON payload format
- Provides Gateway web interface for configuration

### Technology Stack
- **Build System**: Gradle with Ignition Module Tools Plugin
- **Java Version**: Java 17 (JDK 17)
- **Ignition Version**: 8.3.0+
- **MQTT Client Library**: Eclipse Paho MQTT Java Client (org.eclipse.paho:org.eclipse.paho.client.mqttv3)
- **JSON Processing**: Gson (included with Ignition)
- **Module Scopes**: Gateway only (no Designer/Client scopes needed)

---

## Architecture Overview

### Module Structure
```
ignition-mqtt/
├── mqtt-gateway/              # Gateway scope implementation
│   ├── src/main/java/
│   │   └── com/mycompany/mqtt/gateway/
│   │       ├── MqttGatewayHook.java              # Main module hook
│   │       ├── MqttPublisherManager.java         # Manages MQTT connections
│   │       ├── TagSubscriptionManager.java       # Manages tag subscriptions
│   │       ├── MqttTopicMapper.java              # Maps tags to MQTT topics
│   │       ├── JsonPayloadBuilder.java           # Builds JSON payloads
│   │       ├── config/
│   │       │   ├── MqttModuleSettings.java       # Module settings record
│   │       │   └── MqttConfigResource.java       # Config storage
│   │       └── web/
│   │           ├── MqttConfigPage.java           # Gateway config page
│   │           └── routes/                       # Web API routes
│   └── src/main/resources/
│       └── mounted/                              # Static web resources
│           └── mqtt-config-page/
│               ├── index.html
│               ├── config.js
│               └── styles.css
├── mqtt-common/               # Common scope (shared models)
│   └── src/main/java/
│       └── com/mycompany/mqtt/common/
│           ├── MqttModuleConstants.java
│           └── models/
│               ├── MqttBrokerConfig.java
│               ├── TagPublishConfig.java
│               └── PayloadTemplate.java
├── build.gradle.kts           # Root build file
└── settings.gradle.kts        # Module settings
```

### Key Components

#### 1. MqttGatewayHook
**Responsibilities**:
- Entry point for the module lifecycle
- Initializes MqttPublisherManager and TagSubscriptionManager
- Registers web resources and configuration pages
- Handles module startup/shutdown

**Key Methods**:
- `setup(GatewayContext)` - Initialize services
- `startup(LicenseState)` - Start MQTT connections and tag subscriptions
- `shutdown()` - Clean shutdown of MQTT and tag subscriptions
- `getMountedResourceFolder()` - Return web resources
- `getMountPathAlias()` - Return web mount path (/mqtt-module)

#### 2. MqttPublisherManager
**Responsibilities**:
- Manages MQTT client connections to broker
- Handles connection lifecycle (connect, reconnect, disconnect)
- Publishes messages to MQTT broker
- Maintains connection health monitoring

**Key Methods**:
- `connect(MqttBrokerConfig)` - Establish MQTT connection
- `publish(String topic, String payload, int qos, boolean retained)` - Publish to broker
- `disconnect()` - Close MQTT connection
- `isConnected()` - Check connection status
- `reconnect()` - Handle reconnection with exponential backoff

**Dependencies**:
- Eclipse Paho MqttClient

#### 3. TagSubscriptionManager
**Responsibilities**:
- Subscribe to tag value changes via Ignition Tag Manager
- Filter tags based on provider/folder configuration
- Trigger MQTT publications on tag value changes
- Handle tag quality changes and deadband logic

**Key Methods**:
- `subscribeToTags(List<TagPath>)` - Subscribe to tag changes
- `unsubscribeAll()` - Remove all subscriptions
- `handleTagChange(TagChangeEvent)` - Process tag changes
- `applyDeadband(QualifiedValue, QualifiedValue)` - Check if change exceeds deadband

**Dependencies**:
- `GatewayContext.getTagManager()`
- `TagManager.subscribeAsync()`

#### 4. MqttTopicMapper
**Responsibilities**:
- Generate MQTT topics from tag paths
- Support direct tag path mapping with customization
- Handle topic sanitization (replace invalid characters)
- Apply user-defined topic overrides

**Key Methods**:
- `mapTagToTopic(TagPath tagPath)` - Convert tag path to MQTT topic
- `applyTopicOverride(TagPath, String customTopic)` - Use custom mapping
- `sanitizeTopic(String topic)` - Clean topic string
- `parseTopicTemplate(String template, TagPath tag)` - Future: template support

**Topic Mapping Strategy**:
```
Tag Path: [default]Site/Area/Line/Device/Temperature
Default MQTT Topic: default/site/area/line/device/temperature
```

#### 5. JsonPayloadBuilder
**Responsibilities**:
- Build JSON payloads from tag data
- Support default payload structure
- Allow user-defined custom payload templates
- Include tag metadata (datatype, units, engineering limits)

**Default Payload Format**:
```json
{
  "timestamp": 1706140800000,
  "value": 72.5,
  "quality": "Good",
  "tagPath": "[default]Site/Area/Temperature",
  "metadata": {
    "dataType": "Float8",
    "units": "°F",
    "engUnit": "Fahrenheit",
    "engLow": 0.0,
    "engHigh": 212.0
  }
}
```

**Custom Payload Support**:
- JSON template with placeholders: `{value}`, `{timestamp}`, `{quality}`, `{tagPath}`, `{metadata.*}`
- Stored in configuration
- Validated on save

#### 6. Configuration System

**MqttBrokerConfig** (stored in Config Resources):
```java
public class MqttBrokerConfig {
    String brokerUrl;          // tcp://localhost:1883
    String clientId;           // ignition-mqtt-publisher
    String username;           // Optional
    SecretConfig password;     // Uses new SecretConfig API
    boolean useTls;            // Enable TLS
    int qos;                   // 0, 1, or 2
    boolean retained;          // Retain messages
    int keepAlive;             // Keep alive interval (seconds)
    int connectionTimeout;     // Connection timeout (seconds)
}
```

**TagPublishConfig**:
```java
public class TagPublishConfig {
    List<String> tagProviders;          // Selected providers
    List<String> tagFolders;            // Selected folders within providers
    Map<String, String> topicOverrides; // Tag path -> custom MQTT topic
    String payloadTemplate;             // Custom JSON template (null = default)
    boolean includeMetadata;            // Include tag metadata in payload
    double valueDeadband;               // Publish only if value change > deadband
    boolean publishOnQualityChange;     // Publish on quality change
}
```

#### 7. Gateway Web Interface

**Configuration Page Features**:
1. **MQTT Broker Settings Tab**
   - Broker URL input
   - Client ID
   - Authentication (username/password with SecretConfig)
   - TLS settings
   - QoS selection (0, 1, 2)
   - Retained flag
   - Connection status indicator

2. **Tag Selection Tab**
   - Tag provider multi-select dropdown
   - Folder browser/tree view
   - Selected tags/folders list
   - Add/Remove buttons

3. **Topic Mapping Tab**
   - Default mapping preview
   - Topic override table (tag path -> custom topic)
   - Topic sanitization preview

4. **Payload Configuration Tab**
   - Default payload preview with sample tag
   - Custom payload template editor (JSON with placeholders)
   - Template validation
   - Include metadata checkbox

5. **Status & Diagnostics Tab**
   - Connection status
   - Last published message preview
   - Publish rate statistics
   - Error log

**Web Technology**:
- Backend: Gateway API routes using `AbstractApiRoute`
- Frontend: Vanilla JS + HTML + CSS (avoid complex frameworks for simplicity)
- Configuration stored using new Config Resource API (8.3)

---

## Implementation Phases

### Phase 1: Project Setup & Module Skeleton (Estimated: 2-3 days)

**Tasks**:
1. Create Gradle multi-module project structure
   - Configure `settings.gradle.kts`
   - Create `build.gradle.kts` for each scope (common, gateway)
   - Configure Ignition Module Tools plugin
   - Set up module metadata (id, name, version, description)

2. Implement basic `MqttGatewayHook`
   - Module lifecycle methods (setup, startup, shutdown)
   - Logging setup
   - Basic module info

3. Add Eclipse Paho MQTT dependency
   - Add to gateway build.gradle.kts
   - Verify dependency resolution

4. Create module signature and build
   - Generate unsigned .modl file
   - Test installation on Ignition 8.3

**Deliverables**:
- Buildable module skeleton
- Successfully installs on Ignition Gateway
- Shows in module list

**Success Criteria**:
- `./gradlew build` succeeds
- Module appears in Gateway > Config > Modules
- No errors in Gateway logs on startup/shutdown

---

### Phase 2: MQTT Connection Management (Estimated: 3-4 days)

**Tasks**:
1. Implement `MqttPublisherManager`
   - MQTT client initialization
   - Connection with configurable broker settings
   - Publish method implementation
   - Connection state management
   - Error handling and reconnection logic
   - Thread-safe operations

2. Create configuration models
   - `MqttBrokerConfig` record class
   - Use `SecretConfig` for password storage
   - Default values

3. Implement basic configuration storage
   - Use Config Resource API (8.3)
   - Load/save broker configuration
   - Handle configuration changes

4. Add connection health monitoring
   - Heartbeat mechanism
   - Reconnection with exponential backoff
   - Connection status events

5. Unit testing
   - Mock MQTT broker for tests
   - Connection/disconnection tests
   - Publish tests

**Deliverables**:
- Working MQTT connection to external broker
- Configuration persistence
- Reconnection handling

**Success Criteria**:
- Successfully connects to Mosquitto/HiveMQ test broker
- Publishes test messages
- Reconnects after broker restart
- Configuration persists across Gateway restarts

---

### Phase 3: Tag Subscription & Change Detection (Estimated: 4-5 days)

**Tasks**:
1. Implement `TagSubscriptionManager`
   - Subscribe to tag value changes using `TagManager.subscribeAsync()`
   - Handle `TagChangeEvent` callbacks
   - Manage subscription lifecycle
   - Filter tags by provider/folder

2. Implement tag filtering logic
   - Browse tag providers via `TagManager.browse()`
   - Filter by configured providers and folders
   - Build tag path list for subscription

3. Implement change detection logic
   - Value deadband checking
   - Quality change detection
   - Timestamp tracking

4. Create `TagPublishConfig` model
   - Tag provider/folder selection
   - Deadband settings
   - Quality change flag

5. Integration with `MqttPublisherManager`
   - Trigger publish on tag change
   - Pass tag data to payload builder

6. Testing
   - Create test tags in Ignition
   - Verify subscriptions are active
   - Verify change detection logic

**Deliverables**:
- Tag subscription system
- Change detection with deadband
- Integration with MQTT publisher

**Success Criteria**:
- Subscribes to tags in configured folders
- Detects value changes
- Applies deadband correctly
- Triggers MQTT publish on change

---

### Phase 4: Topic Mapping & JSON Payload Generation (Estimated: 3-4 days)

**Tasks**:
1. Implement `MqttTopicMapper`
   - Direct tag path to MQTT topic conversion
   - Topic sanitization (remove/replace invalid chars)
   - Topic override support
   - Case handling (convert to lowercase)

2. Implement `JsonPayloadBuilder`
   - Default payload structure
   - Tag metadata extraction from TagManager
   - JSON serialization with Gson
   - Custom template support (future)

3. Extract tag metadata
   - Data type
   - Engineering units
   - Engineering limits
   - Custom properties (if needed)

4. Testing
   - Topic generation tests
   - Payload generation tests
   - Metadata extraction tests

**Deliverables**:
- Topic mapping system
- JSON payload builder
- Metadata extraction

**Success Criteria**:
- Topics generated correctly from tag paths
- JSON payloads include value, quality, timestamp
- Metadata included when configured
- Topics sanitized properly

---

### Phase 5: Gateway Web Configuration Interface (Estimated: 5-7 days)

**Tasks**:
1. Create Gateway web configuration page
   - Extend `AbstractApiRoute` for REST endpoints
   - Create HTML/CSS/JS for UI
   - Mount web resources via `getMountedResourceFolder()`

2. Implement MQTT Broker Settings tab
   - Form inputs for broker config
   - Connection test button
   - Save/load configuration
   - Password field using secret storage

3. Implement Tag Selection tab
   - Tag provider dropdown (load from Gateway)
   - Folder browser UI
   - Selected items list
   - Add/remove functionality

4. Implement Topic Mapping tab
   - Preview default topic mappings
   - Table for custom topic overrides
   - Add/edit/delete overrides

5. Implement Payload Configuration tab
   - Default payload preview
   - Custom template editor (textarea)
   - Template validation
   - Metadata inclusion toggle

6. Implement Status & Diagnostics tab
   - Connection status indicator
   - Last published messages log
   - Publish rate counter
   - Error messages display

7. Backend REST API routes
   - GET/POST `/data/mqtt-module/config` - Load/save configuration
   - GET `/data/mqtt-module/providers` - List tag providers
   - GET `/data/mqtt-module/browse` - Browse tag folders
   - POST `/data/mqtt-module/test-connection` - Test MQTT connection
   - GET `/data/mqtt-module/status` - Get connection/publish status

8. Testing
   - UI functionality tests
   - API endpoint tests
   - Configuration persistence tests

**Deliverables**:
- Functional Gateway web interface
- Configuration management
- REST API backend

**Success Criteria**:
- Configuration page accessible at http://localhost:8088/data/mqtt-module/
- Can configure MQTT broker settings
- Can select tag providers/folders
- Can customize topic mappings
- Configuration saves and loads correctly
- Connection test works

---

### Phase 6: Custom Payload Templates (Estimated: 3-4 days)

**Tasks**:
1. Design template syntax
   - Placeholder format: `{value}`, `{timestamp}`, `{quality}`, etc.
   - Nested metadata access: `{metadata.dataType}`
   - Example template

2. Implement template parser
   - Parse template string
   - Replace placeholders with actual values
   - Handle missing values gracefully

3. Add template validation
   - Syntax checking
   - Validate placeholders
   - Preview with sample data

4. Update `JsonPayloadBuilder`
   - Support template mode
   - Fall back to default if template is null/invalid

5. Update configuration UI
   - Template editor in Payload Configuration tab
   - Validation feedback
   - Preview with sample tag

6. Testing
   - Template parsing tests
   - Validation tests
   - Edge cases (missing placeholders, malformed JSON)

**Deliverables**:
- Custom payload template system
- Template validation
- UI integration

**Success Criteria**:
- Can define custom JSON structure
- Templates validated before save
- Placeholders replaced with actual tag data
- Invalid templates show errors in UI

---

### Phase 7: Testing & Bug Fixes (Estimated: 5-7 days)

**Tasks**:
1. End-to-end testing
   - Full workflow: configure -> subscribe -> publish
   - Test with various tag types (Int, Float, String, Boolean)
   - Test with different MQTT brokers (Mosquitto, HiveMQ)
   - Test reconnection scenarios

2. Load testing
   - High-frequency tag changes
   - Large number of tags
   - Memory and CPU profiling

3. Error handling improvements
   - Connection failures
   - Invalid configuration
   - MQTT broker unavailable
   - Tag subscription failures

4. Bug fixing
   - Address issues found in testing
   - Memory leaks
   - Thread safety issues

5. Logging improvements
   - Add appropriate log levels
   - Useful diagnostic messages
   - Structured logging

6. Documentation
   - Inline code documentation
   - User manual (Markdown)
   - Configuration examples

**Deliverables**:
- Stable, tested module
- Bug fixes
- Documentation

**Success Criteria**:
- Module handles 1000+ tags without issues
- Reconnects reliably after broker failures
- No memory leaks during 24-hour test
- All error conditions handled gracefully

---

### Phase 8: Documentation & Deployment (Estimated: 2-3 days)

**Tasks**:
1. Create user documentation
   - Installation instructions
   - Configuration guide
   - Topic mapping guide
   - Custom payload templates guide
   - Troubleshooting section

2. Create developer documentation
   - Architecture overview
   - Code organization
   - Extension points
   - Building from source

3. Create example configurations
   - Sample MQTT broker setup
   - Sample tag configurations
   - Sample custom templates

4. Module signing (optional)
   - Generate module signing certificate
   - Sign .modl file

5. Create release package
   - Build final .modl file
   - Package documentation
   - Create release notes

6. Deployment testing
   - Fresh Ignition installation
   - Module installation
   - Configuration from scratch
   - Verify all features work

**Deliverables**:
- Complete documentation
- Signed module file
- Release package

**Success Criteria**:
- Documentation is clear and complete
- Module installs and works on clean Gateway
- All features functional
- Ready for production use

---

## Technical Details

### Ignition 8.3 SDK Key APIs

#### Module Lifecycle
```java
public class MqttGatewayHook extends AbstractGatewayModuleHook {
    @Override
    public void setup(GatewayContext context) {
        // Initialize module resources
        this.context = context;
        // Register configuration resources
    }
    
    @Override
    public void startup(LicenseState activationState) {
        // Start MQTT connections
        // Start tag subscriptions
    }
    
    @Override
    public void shutdown() {
        // Clean shutdown
        // Disconnect MQTT
        // Unsubscribe tags
    }
}
```

#### Tag Subscription (8.3 API)
```java
TagManager tagManager = context.getTagManager();

List<TagPath> tagPaths = Arrays.asList(
    TagPathParser.parse("[default]Folder/Tag1"),
    TagPathParser.parse("[default]Folder/Tag2")
);

CompletableFuture<List<TagChangeListener>> future = 
    tagManager.subscribeAsync(tagPaths, (tagPath, event) -> {
        // Handle tag change
        QualifiedValue value = event.getValue();
        handleTagChange(tagPath, value);
    });
```

#### Configuration Resources (8.3 New API)
```java
public class MqttConfigResource implements JsonResource {
    private MqttBrokerConfig brokerConfig;
    private TagPublishConfig tagConfig;
    
    // Getters and setters
    
    @Override
    public String getResourceType() {
        return "mqtt.config";
    }
}

// In GatewayHook:
@Override
public void setup(GatewayContext context) {
    context.getProjectManager().addResourceType(
        new ResourceType("mqtt.config", MqttConfigResource.class)
    );
}
```

#### Secret Storage (8.3 New API)
```java
SecretConfig secretConfig = resource.getBrokerPassword();
Secret<?> secret = Secret.create(context, secretConfig);
Plaintext plaintext = secret.getPlaintext();
try {
    String password = plaintext.getAsString(StandardCharsets.UTF_8);
    // Use password for MQTT connection
} finally {
    plaintext.clear(); // Clear from memory
}
```

#### Web Routes (8.3 API)
```java
public class MqttConfigRoute extends AbstractApiRoute {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // Load configuration
        MqttConfigResource config = loadConfig();
        
        // Serialize to JSON
        String json = new Gson().toJson(config);
        
        // Send response
        resp.setContentType("application/json");
        resp.getWriter().write(json);
    }
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // Parse JSON body
        MqttConfigResource config = new Gson().fromJson(
            req.getReader(), 
            MqttConfigResource.class
        );
        
        // Save configuration
        saveConfig(config);
        
        // Send success response
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}

// Register route in GatewayHook:
@Override
public void mountRouteHandlers(RouteGroup routes) {
    routes.newRoute("/mqtt-module/config")
          .handler(new MqttConfigRoute())
          .mount();
}
```

### MQTT Integration

#### Eclipse Paho MQTT Client Usage
```java
MqttClient client = new MqttClient(
    "tcp://localhost:1883",     // Broker URL
    "ignition-mqtt-client",     // Client ID
    new MemoryPersistence()     // Persistence
);

MqttConnectOptions options = new MqttConnectOptions();
options.setUserName("username");
options.setPassword("password".toCharArray());
options.setCleanSession(true);
options.setConnectionTimeout(30);
options.setKeepAliveInterval(60);

// Connect
client.connect(options);

// Publish
String topic = "site/area/device/temperature";
String payload = "{\"value\": 72.5, \"timestamp\": 1706140800000}";
MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
message.setQos(1);
message.setRetained(false);
client.publish(topic, message);

// Disconnect
client.disconnect();
```

#### Connection State Management
```java
public class MqttPublisherManager {
    private MqttClient client;
    private MqttBrokerConfig config;
    private final ScheduledExecutorService reconnectScheduler;
    private int reconnectAttempts = 0;
    
    public void connect() {
        try {
            if (client == null || !client.isConnected()) {
                client = new MqttClient(config.getBrokerUrl(), 
                                       config.getClientId(), 
                                       new MemoryPersistence());
                
                MqttConnectOptions options = buildConnectOptions();
                client.connect(options);
                
                logger.info("Connected to MQTT broker: {}", config.getBrokerUrl());
                reconnectAttempts = 0;
            }
        } catch (MqttException e) {
            logger.error("Failed to connect to MQTT broker", e);
            scheduleReconnect();
        }
    }
    
    private void scheduleReconnect() {
        long delay = Math.min(30000, 1000 * (long) Math.pow(2, reconnectAttempts));
        reconnectScheduler.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
        reconnectAttempts++;
    }
}
```

### JSON Payload Examples

#### Default Payload
```json
{
  "timestamp": 1706140800000,
  "value": 72.5,
  "quality": "Good",
  "tagPath": "[default]Site/Area/Temperature",
  "metadata": {
    "dataType": "Float8",
    "units": "°F",
    "engUnit": "Fahrenheit",
    "engLow": 0.0,
    "engHigh": 212.0
  }
}
```

#### Custom Payload Template Example
```json
{
  "v": {value},
  "t": {timestamp},
  "q": "{quality}",
  "meta": {
    "type": "{metadata.dataType}",
    "unit": "{metadata.units}"
  }
}
```

---

## Configuration Examples

### MQTT Broker Configuration
```json
{
  "brokerUrl": "tcp://mqtt.example.com:1883",
  "clientId": "ignition-mqtt-publisher-001",
  "username": "ignition",
  "password": "<encrypted>",
  "useTls": false,
  "qos": 1,
  "retained": false,
  "keepAlive": 60,
  "connectionTimeout": 30
}
```

### Tag Publish Configuration
```json
{
  "tagProviders": ["default", "edge"],
  "tagFolders": [
    "[default]Site/Area1",
    "[default]Site/Area2/Line1",
    "[edge]Sensors"
  ],
  "topicOverrides": {
    "[default]Site/Area1/Temperature": "custom/topic/temp",
    "[default]Site/Area2/Line1/Pressure": "line1/pressure"
  },
  "payloadTemplate": null,
  "includeMetadata": true,
  "valueDeadband": 0.1,
  "publishOnQualityChange": true
}
```

---

## Dependencies

### Gradle Dependencies (gateway scope)
```kotlin
dependencies {
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:8.3.0")
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:8.3.0")
    
    // MQTT Client
    modlImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    
    // Logging (provided by Ignition)
    compileOnly("ch.qos.logback:logback-classic:1.2.9")
    compileOnly("org.slf4j:slf4j-api:1.7.32")
    
    // JSON (provided by Ignition)
    compileOnly("com.google.code.gson:gson:2.8.9")
}
```

---

## Risk Assessment & Mitigation

### Risk 1: MQTT Connection Stability
**Risk**: Network issues or broker downtime could cause connection failures
**Mitigation**:
- Implement robust reconnection logic with exponential backoff
- Buffer messages locally when disconnected (optional future enhancement)
- Connection health monitoring
- User-configurable timeouts

### Risk 2: High Tag Change Rate
**Risk**: High-frequency tag changes could overwhelm MQTT broker or network
**Mitigation**:
- Implement rate limiting per tag
- Configurable publish intervals
- Deadband filtering to reduce unnecessary publishes
- Queue management with size limits

### Risk 3: Configuration Complexity
**Risk**: Users may find configuration complex, leading to misconfiguration
**Mitigation**:
- Provide sensible defaults
- Clear UI with validation
- Configuration templates/examples
- Built-in connection testing
- Comprehensive documentation

### Risk 4: Performance Impact
**Risk**: Module could impact Gateway performance with many tag subscriptions
**Mitigation**:
- Efficient tag subscription batching
- Use async APIs where possible
- Memory profiling during testing
- Configurable thread pool sizes
- Resource cleanup on shutdown

### Risk 5: Payload Customization Edge Cases
**Risk**: Custom payload templates might be invalid or cause errors
**Mitigation**:
- Template validation before saving
- Safe fallback to default payload
- Preview feature with sample data
- Error handling in template parsing
- Clear error messages

---

## Future Enhancements (Post-MVP)

### 1. Sparkplug B Support
- Implement Sparkplug B specification
- Birth/Death certificates
- DCMD support
- Metrics structure

### 2. Bidirectional MQTT (MQTT Subscribe)
- Subscribe to MQTT topics
- Write values back to Ignition tags
- Command/control support

### 3. Advanced Topic Mapping
- ISA-95 hierarchy templates
- Regular expression-based mapping
- Conditional topic rules

### 4. Message Buffering
- Local buffer for offline messages
- Persistent storage option
- Buffer size limits and overflow handling

### 5. Store and Forward
- Integration with Ignition's store and forward
- Historical data publishing

### 6. Metrics and Analytics
- Publish rate metrics
- Message size analytics
- Connection uptime tracking
- Export to Grafana/Prometheus

### 7. Multi-Broker Support
- Publish to multiple brokers simultaneously
- Failover broker configuration
- Load balancing

---

## Success Metrics

### Functional Metrics
- Successfully connects to external MQTT broker
- Publishes tag changes with <100ms latency
- Handles 1000+ simultaneous tag subscriptions
- Reconnects within 30 seconds of broker restart
- Zero data loss during normal operation

### Performance Metrics
- Memory footprint: <100MB for 1000 tags
- CPU usage: <5% under normal load
- Publish throughput: >1000 messages/second

### Quality Metrics
- Zero critical bugs in production
- 95%+ unit test coverage
- Successful installation on fresh Ignition 8.3 Gateway
- Comprehensive documentation

---

## Project Timeline

| Phase | Duration | Dependencies | Deliverables |
|-------|----------|--------------|--------------|
| Phase 1: Project Setup | 2-3 days | None | Module skeleton |
| Phase 2: MQTT Connection | 3-4 days | Phase 1 | MQTT publisher manager |
| Phase 3: Tag Subscription | 4-5 days | Phase 1 | Tag change detection |
| Phase 4: Topic & Payload | 3-4 days | Phase 2, 3 | Topic mapper, JSON builder |
| Phase 5: Web Interface | 5-7 days | Phase 2, 3, 4 | Gateway config UI |
| Phase 6: Custom Templates | 3-4 days | Phase 4, 5 | Template system |
| Phase 7: Testing & Fixes | 5-7 days | All phases | Stable module |
| Phase 8: Documentation | 2-3 days | Phase 7 | Complete docs |

**Total Estimated Duration**: 27-37 days (~5-7 weeks)

---

## Getting Started Checklist

### Development Environment Setup
- [ ] Install JDK 17
- [ ] Install Gradle 7.6+
- [ ] Install Ignition 8.3.0+ Gateway
- [ ] Configure Ignition to allow unsigned modules
- [ ] Install Git
- [ ] Set up IDE (IntelliJ IDEA recommended)

### External Tools
- [ ] Install MQTT broker for testing (Mosquitto recommended)
- [ ] Install MQTT client for testing (MQTT Explorer or mosquitto_sub/pub)
- [ ] Set up test tag provider in Ignition

### Repository Setup
- [ ] Initialize Git repository
- [ ] Create Gradle project structure
- [ ] Configure Ignition Module Tools plugin
- [ ] Add .gitignore for Java/Gradle projects

### Module Metadata
- [ ] Define module ID (e.g., com.mycompany.mqtt)
- [ ] Define module name (e.g., "MQTT UNS Publisher")
- [ ] Set initial version (e.g., 1.0.0-SNAPSHOT)
- [ ] Add license information

---

## Conclusion

This plan provides a comprehensive roadmap for building a production-ready MQTT Unified Namespace module for Ignition 8.3. The phased approach allows for incremental development and testing, ensuring a stable and functional module at each stage.

The module will enable Ignition users to:
- Publish tag data to external MQTT brokers
- Structure data in a Unified Namespace format
- Customize MQTT topics and JSON payloads
- Configure and monitor via Gateway web interface

With the estimated timeline of 5-7 weeks, this project is achievable for a single developer with Ignition SDK experience, or could be accelerated with a small team.

## Next Steps

1. Review this plan and confirm requirements
2. Set up development environment
3. Begin Phase 1: Project Setup & Module Skeleton
4. Establish testing environment (Mosquitto broker, test tags)
5. Create Git repository and initial commit

---

**Document Version**: 1.0  
**Last Updated**: January 24, 2026  
**Author**: OpenCode AI Assistant
