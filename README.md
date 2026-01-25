# MQTT UNS Publisher Module for Ignition 8.3

An Ignition module that publishes tag data to an external MQTT broker in a Unified Namespace (UNS) structure with configurable JSON payloads.

## Features

- **MQTT Integration**: Connect to external MQTT brokers (Mosquitto, HiveMQ, EMQX, etc.)
- **Tag Monitoring**: Poll-based tag monitoring with configurable intervals (100ms - 60s)
- **Flexible Topic Mapping**: Direct tag path to MQTT topic conversion with custom overrides
- **Customizable Payloads**: JSON structure with value, quality, timestamp, and optional metadata
- **Statistics & Health Monitoring**: Real-time performance metrics and health status
- **Robust Connection Handling**: Automatic reconnection with exponential backoff
- **Change Detection**: Configurable deadband filtering and quality change detection
- **Thread-Safe Operations**: Concurrent tag polling and MQTT publishing

## Requirements

- **Ignition**: 8.3.0 or later (Standard or Enterprise Edition - NOT Maker Edition)
- **Java**: 17
- **Gradle**: 7.6 or later
- **MQTT Broker**: Mosquitto, HiveMQ, EMQX, or any MQTT 3.1.1 compatible broker

**Important:** This module will NOT work on Ignition Maker Edition, as Maker Edition does not support third-party modules.

## Building

Use the included build script:

```bash
./build.sh
```

Or use Gradle directly:

```bash
./gradlew clean build
```

The compiled `.modl` file will be located at `build/MQTT-UNS-Publisher.unsigned.modl` (approximately 250KB).

## Installation

1. Build the module (see above)
2. Navigate to your Ignition Gateway web interface (typically `http://localhost:8088`)
3. Go to **Config > Modules**
4. Scroll down and click **Install or Upgrade a Module**
5. Select the `MQTT-UNS-Publisher.unsigned.modl` file from the `build/` directory
6. Click **Install**
7. Wait for the module to load (should show "Running" status)
8. Navigate to **Config > Connections > MQTT UNS Publisher** to access the web configuration UI

### Development Mode

For development, you may want to allow unsigned modules. Add this line to your `data/ignition.conf` file:

```
wrapper.java.additional.[index]=-Dignition.allowunsignedmodules=true
```

Replace `[index]` with the next available index number.

## Configuration

The module can be configured in two ways:

### Option 1: Web UI (Recommended)

Navigate to **Config > Connections > MQTT UNS Publisher** in the Gateway web interface. The web UI provides three tabs:

1. **Broker Settings**: Configure MQTT broker connection parameters
   - Broker URL, client ID, credentials
   - QoS, retained messages, connection timeout
   - Test connection functionality

2. **Tag Publishing**: Select tags and configure publishing behavior
   - Choose tag providers or specific folders
   - Set polling rate and deadband filtering
   - Configure topic overrides
   - Enable/disable metadata publishing

3. **Status & Statistics**: Monitor real-time module performance
   - Connection status and uptime
   - Publish statistics (success/failure rates)
   - Tag read statistics
   - Health monitoring with auto-refresh

### Option 2: JSON File (Legacy)

The module can also be configured via a JSON file located in your Ignition data directory:

```
<ignition-data>/mqtt-uns-config.json
```

### Example Configuration

```json
{
  "broker": {
    "brokerUrl": "tcp://localhost:1883",
    "clientId": "ignition-mqtt-publisher",
    "username": "",
    "password": "",
    "qos": 1,
    "retained": false,
    "cleanSession": true,
    "connectionTimeout": 30,
    "keepAliveInterval": 60
  },
  "tags": {
    "enabled": true,
    "tagProviders": ["default"],
    "tagFolders": ["[default]TestTags"],
    "valueDeadband": 0.1,
    "pollRateMs": 1000,
    "publishOnQualityChange": true,
    "includeMetadata": true,
    "topicOverrides": {
      "[default]TestTags/Temperature": "factory/zone1/temp"
    }
  }
}
```

See `mqtt-uns-config-combined-example.json` for a complete example.

### Configuration Options

**Broker Settings:**
- `brokerUrl`: MQTT broker URL (tcp://host:port or ssl://host:port)
- `clientId`: Unique client identifier
- `username`/`password`: Authentication credentials (optional)
- `qos`: Quality of Service (0, 1, or 2)
- `retained`: Whether messages should be retained by broker
- `cleanSession`: Whether to use clean session

**Tag Settings:**
- `enabled`: Enable/disable tag publishing
- `tagProviders`: List of tag providers to monitor (or use tagFolders)
- `tagFolders`: Specific tag folders to monitor (format: `[provider]Path/To/Folder`)
- `valueDeadband`: Minimum value change to trigger publish
- `pollRateMs`: Tag polling interval in milliseconds (100-60000)
- `publishOnQualityChange`: Publish when quality changes (true/false)
- `includeMetadata`: Include tag metadata in payload (true/false)
- `topicOverrides`: Map specific tag paths to custom MQTT topics

### MQTT Broker Setup

See [MQTT-BROKER-SETUP.md](MQTT-BROKER-SETUP.md) for detailed broker setup instructions including:
- Mosquitto (open source)
- HiveMQ Community Edition
- EMQX
- AWS IoT Core
- Azure IoT Hub

## Development Status

This module is **production-ready** with full web UI configuration. Current phase: **Phase 6 Complete** ✅

### Completed Features
- [x] **Phase 1**: Project structure and module skeleton
- [x] **Phase 2**: MQTT connection management
  - [x] MqttPublisherManager with connection lifecycle
  - [x] Reconnection logic with exponential backoff
  - [x] Configuration persistence (JSON file-based)
  - [x] Connection health monitoring
  - [x] Thread-safe operations
- [x] **Phase 3**: Tag subscription system
  - [x] Polling-based tag monitoring (100ms - 60s configurable)
  - [x] Tag discovery from providers and folders
  - [x] Recursive tag browsing
  - [x] Deadband filtering
  - [x] Quality change detection
- [x] **Phase 4**: Topic mapping and payload generation
  - [x] Automatic tag path to MQTT topic mapping
  - [x] Topic sanitization (lowercase, underscore replacement)
  - [x] Custom topic overrides
  - [x] JSON payload builder with metadata
  - [x] Configurable metadata inclusion
- [x] **Phase 5**: Statistics & Health Monitoring
  - [x] Real-time statistics tracking
  - [x] Health status with HEALTHY/DEGRADED/UNHEALTHY levels
  - [x] Publish success rate calculation
  - [x] Tag read success rate calculation
  - [x] Connection success rate calculation
  - [x] Detailed statistics reporting
- [x] **Phase 6**: Gateway web configuration UI ✅
  - [x] React-based configuration interface
  - [x] REST API endpoints for configuration CRUD
  - [x] Database-backed configuration storage (PersistentRecord)
  - [x] Real-time status dashboard with auto-refresh
  - [x] Test MQTT connection functionality
  - [x] Three-tab interface (Broker Settings, Tag Publishing, Status Dashboard)

### Pending Features
- [ ] **Phase 7**: Advanced features
  - [ ] Custom payload templates
  - [ ] Sparkplug B protocol support
  - [ ] TLS/SSL support for secure brokers
  - [ ] Batch publishing for performance
- [ ] **Phase 8**: Testing & optimization
  - [ ] Unit tests
  - [ ] Integration tests with real MQTT brokers
  - [ ] Performance optimization
  - [ ] Load testing

### Known Limitations
- Poll-based rather than event-driven tag monitoring (acceptable for most use cases)
- No TLS/SSL support yet (plaintext MQTT only)
- No Sparkplug B protocol support yet
- Not compatible with Ignition Maker Edition

## Project Structure

```
ignition-mqtt/
├── mqtt-common/                      # Common scope (shared models)
│   └── src/main/java/.../common/
│       ├── MqttModuleConstants.java
│       └── model/
│           ├── ConnectionState.java
│           ├── MqttBrokerConfig.java
│           ├── TagPublishConfig.java
│           └── MqttModuleConfig.java
├── mqtt-gateway/                     # Gateway scope (main implementation)
│   ├── src/main/java/.../gateway/
│   │   ├── MqttGatewayHook.java         # Module entry point
│   │   ├── MqttPublisherManager.java    # MQTT connection management
│   │   ├── TagSubscriptionManager.java  # Tag polling & publishing
│   │   ├── MqttTopicMapper.java         # Tag-to-topic mapping
│   │   ├── JsonPayloadBuilder.java      # JSON payload generation
│   │   ├── ModuleStatistics.java        # Runtime statistics
│   │   ├── ModuleHealthStatus.java      # Health monitoring
│   │   ├── config/
│   │   │   └── ConfigurationManager.java  # JSON config loader
│   │   ├── records/                     # Database persistence
│   │   │   ├── MqttBrokerSettings.java
│   │   │   ├── MqttTagSettings.java
│   │   │   └── MqttConfigRecordListener.java
│   │   └── web/                         # REST API
│   │       ├── MqttConfigRoute.java
│   │       ├── MqttStatusRoute.java
│   │       └── TestConnectionRoute.java
│   ├── web-ui/                          # React web interface
│   │   ├── src/
│   │   │   ├── components/
│   │   │   │   ├── Configuration.tsx    # Main container
│   │   │   │   ├── BrokerSettings.tsx   # Broker config tab
│   │   │   │   ├── TagSelection.tsx     # Tag selection tab
│   │   │   │   └── StatusDashboard.tsx  # Status monitoring tab
│   │   │   ├── index.tsx                # Entry point
│   │   │   └── styles.css               # Global styles
│   │   ├── package.json
│   │   └── webpack.config.js
│   └── src/main/resources/
│       └── mounted/                     # Webpack output
│           └── mqtt-config.js           # Bundled React app
├── build.gradle.kts                  # Root build configuration
├── settings.gradle.kts               # Gradle settings
├── build.sh                          # Build script
├── BUILD-AND-TEST-PLAN.md           # Testing guide
├── MQTT-BROKER-SETUP.md             # Broker setup guide
├── mqtt-uns-config-combined-example.json  # Config example
└── README.md                        # This file
```

## Statistics & Health Monitoring

The module tracks comprehensive runtime statistics accessible via the `MqttGatewayHook`:

**Available Metrics:**
- Messages published (total and failed)
- Tag reads (successful and failed)
- Connection attempts and failures
- Publish success rate (%)
- Tag read success rate (%)
- Connection success rate (%)
- Module uptime
- Last publish timestamp
- Last successful connection timestamp

**Health Status Levels:**
- `HEALTHY`: Connected and operating normally (≥95% success rate)
- `DEGRADED`: Connected but experiencing issues (80-95% success rate or reconnecting)
- `UNHEALTHY`: Disconnected, errors, or high failure rate (<80% success rate)

Access health status in Gateway scripts:
```python
# Get module hook
hook = system.util.getGatewayHook("com.inductiveautomation.mqtt.uns")

# Get health status
health = hook.getHealthStatus()
print health.getHealthReport()

# Get statistics
stats = hook.getStatistics()
print stats.getDetailedReport()
```

## Architecture

The module uses a multi-manager architecture:

1. **MqttGatewayHook**: Module lifecycle manager and coordinator
2. **ConfigurationManager**: Loads/saves JSON configuration files
3. **MqttPublisherManager**: MQTT broker connection and publishing
4. **TagSubscriptionManager**: Tag polling and change detection
5. **MqttTopicMapper**: Tag path to MQTT topic conversion
6. **JsonPayloadBuilder**: JSON payload generation
7. **ModuleStatistics**: Runtime metrics tracking
8. **ModuleHealthStatus**: Health monitoring and diagnostics

### MQTT Payload Format

**Standard Payload** (with metadata):
```json
{
  "timestamp": 1706140800000,
  "value": 72.5,
  "quality": "Good",
  "qualityCode": 192,
  "tagPath": "[default]TestTags/Temperature",
  "metadata": {
    "dataType": "Float8"
  }
}
```

**Simple Payload** (without metadata):
```json
{
  "timestamp": 1706140800000,
  "value": 72.5,
  "quality": "Good",
  "qualityCode": 192,
  "tagPath": "[default]TestTags/Temperature"
}
```

### Topic Mapping Examples

| Tag Path | MQTT Topic |
|----------|-----------|
| `[default]TestTags/Temperature` | `default/testtags/temperature` |
| `[Production]Line1/Motor/Speed` | `production/line1/motor/speed` |
| `[Utilities]HVAC/Zone 2/Temp` | `utilities/hvac/zone_2/temp` |

Custom overrides allow any tag to publish to a specific topic.

## Testing

See [BUILD-AND-TEST-PLAN.md](BUILD-AND-TEST-PLAN.md) for comprehensive testing instructions including:
- Prerequisites and setup
- 14 detailed test scenarios
- Expected results
- Troubleshooting guide

**Quick Test:**
1. Install module on Ignition (Standard Edition)
2. Set up Mosquitto broker: `brew install mosquitto && brew services start mosquitto`
3. Subscribe to all topics: `mosquitto_sub -h localhost -t '#' -v`
4. Create JSON config file with test tags
5. Restart Gateway
6. Watch MQTT messages appear!

## Documentation

- **[BUILD-AND-TEST-PLAN.md](BUILD-AND-TEST-PLAN.md)** - Complete testing guide
- **[MQTT-BROKER-SETUP.md](MQTT-BROKER-SETUP.md)** - MQTT broker setup for various platforms
- **[mqtt-uns-config-combined-example.json](mqtt-uns-config-combined-example.json)** - Configuration example

## Troubleshooting

### Module Shows "Faulted" Status

**Problem:** Module status shows "Faulted - Not eligible for use with Ignition Maker Edition"

**Solution:** This module requires Ignition Standard or Enterprise Edition. Maker Edition does not support third-party modules. Download a trial of Standard Edition from Inductive Automation.

### Module Won't Load - "Unsigned Module"

**Problem:** Module rejected due to signature

**Solution:** Add to `ignition.conf`:
```
wrapper.java.additional.X=-Dignition.allowunsignedmodules=true
```
(Replace X with next available index number)

### Not Connecting to MQTT Broker

**Problem:** Module loaded but not publishing

**Solutions:**
1. Check broker is running: `netstat -an | grep 1883`
2. Verify configuration file exists at correct location
3. Check Gateway logs: `tail -f logs/wrapper.log`
4. Test broker with mosquitto_pub/sub
5. Verify broker URL format: `tcp://hostname:port`

### No Messages Publishing

**Problem:** Connected but no MQTT messages

**Solutions:**
1. Verify tags exist in Ignition
2. Check tag paths match configuration
3. Verify deadband isn't filtering all changes
4. Check module health: Use `getHealthStatus()` in Gateway scripts
5. Review statistics: Look for failed tag reads

### High CPU Usage

**Problem:** Module consuming excessive CPU

**Solutions:**
1. Increase `pollRateMs` (e.g., from 1000ms to 5000ms)
2. Reduce number of monitored tags
3. Increase `valueDeadband` to filter noise

## Performance Tuning

**High-Frequency Publishing (100ms):**
```json
{
  "tags": {
    "pollRateMs": 100,
    "valueDeadband": 0.01,
    "includeMetadata": false
  }
}
```

**Low-Frequency Monitoring (5s):**
```json
{
  "tags": {
    "pollRateMs": 5000,
    "valueDeadband": 1.0,
    "includeMetadata": true
  }
}
```

**Quality-Based Only (no value changes):**
```json
{
  "tags": {
    "pollRateMs": 1000,
    "valueDeadband": 999999999,
    "publishOnQualityChange": true
  }
}
```

## Contributing

This is a reference implementation for Ignition SDK 8.3 module development demonstrating:
- Multi-module Gradle project structure
- Gateway module lifecycle management
- MQTT client integration (Eclipse Paho)
- Tag system integration
- Configuration management
- Statistics and health monitoring
- Thread-safe concurrent operations

Feel free to use this as a template for your own Ignition modules!

## References

- [Ignition SDK Documentation](https://www.sdk-docs.inductiveautomation.com/docs/8.3/)
- [Ignition SDK Examples](https://github.com/inductiveautomation/ignition-sdk-examples)
- [Eclipse Paho MQTT Client](https://www.eclipse.org/paho/)
- [MQTT Protocol](https://mqtt.org/)
- [Unified Namespace (UNS)](https://www.unified-namespace.com/)

## License

Copyright © 2026. All rights reserved.
