# MQTT UNS Publisher Module for Ignition 8.3

An Ignition module that publishes tag data to an external MQTT broker in a Unified Namespace (UNS) structure with configurable JSON payloads.

## Features

- **Multi-Broker Support**: Connect to multiple MQTT brokers simultaneously
- **Advanced Topic Mapping**: Map different tag folders to different brokers with custom topic prefixes
- **MQTT Integration**: Works with any MQTT 3.1.1 broker (Mosquitto, HiveMQ, EMQX, etc.)
- **Tag Monitoring**: Event-driven tag subscriptions with real-time change notifications
- **Web-Based Configuration**: Modern React UI for easy setup and monitoring
- **Customizable Payloads**: JSON structure with value, quality, timestamp, and optional metadata
- **Statistics & Health Monitoring**: Real-time performance metrics and health status dashboard
- **Robust Connection Handling**: Automatic reconnection with exponential backoff
- **Change Detection**: Configurable deadband filtering and quality change detection
- **Thread-Safe Operations**: Concurrent tag change handling and MQTT publishing

## Requirements

- **Ignition**: 8.3.0 or later (Standard or Enterprise Edition)
  - **NOT compatible with Maker Edition** - Third-party modules are not supported on Maker Edition
- **Java**: 17 (for building from source)
- **Node.js**: 16+ and npm (for building the web UI from source)
- **MQTT Broker**: Any MQTT 3.1.1 compatible broker

## Building

Use the unified build script:

```bash
./build.sh
```

You will be prompted to choose:
1. Build UNS Module
2. Build Sparkplug Module
3. Build Both

Non-interactive usage:

```bash
./build.sh uns
./build.sh sparkplug
./build.sh both
```

Or use Gradle directly:

```bash
./gradlew :mqtt-uns-module:build
./gradlew :mqtt-sparkplug-module:build
```

Built `.modl` files will be located at:
- `mqtt-uns-module/build/`
- `mqtt-sparkplug-module/build/`

## Installation

### Download Pre-built Module

Download the latest `.modl` file from the [Releases page](https://github.com/JonathanGrocott/ignition-mqtt/releases).

### Or Build from Source

```bash
./build.sh
```

Follow the prompt to build the UNS module. The compiled `.modl` file will be located at `mqtt-uns-module/build/`.

### Install in Ignition
1. Navigate to your Ignition Gateway web interface (typically `http://localhost:8088`)
2. Go to **Config > Modules**
2. Go to **Config > Modules**
3. Scroll down and click **Install or Upgrade a Module**
4. Select the `.modl` file
5. Click **Install**
6. Wait for the module to load (should show "Running" status)
7. Navigate to **Config > MQTT UNS Publisher** to access the web configuration UI

### Development Mode

For development, you may want to allow unsigned modules. Add this line to your `data/ignition.conf` file:

```
wrapper.java.additional.[index]=-Dignition.allowunsignedmodules=true
```

Replace `[index]` with the next available index number.

## Configuration

### Web UI (Recommended)

The module provides a modern web interface for all configuration needs. See the "Web-Based Configuration" section above for details on the three main sections:
- Broker Configuration
- Tag Publishing Configuration  
- Status Dashboard

### Multi-Broker Setup Example

1. **Add Brokers**: Create multiple broker connections (e.g., one for local Mosquitto, one for cloud HiveMQ)
2. **Create Topic Mappings**: Map different tag folders to different brokers
   - Map `[default]Site1/Production` → Broker 1, Topic: `enterprise/site1/production`
   - Map `[default]Site2/Quality` → Broker 2, Topic: `enterprise/site2/quality`
3. **Enable and Monitor**: Enable the mappings and watch the Status Dashboard

Brokers only connect when they have enabled topic mappings, avoiding unnecessary connections.

### Legacy JSON Configuration

For backwards compatibility, JSON file configuration is still supported at:

```
<ignition-data>/mqtt-uns-config.json
```

**Note**: The web UI is the recommended configuration method. JSON configuration is provided for backwards compatibility and advanced use cases only.

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
    "publishOnQualityChange": true,
    "includeMetadata": true,
    "topicOverrides": {
      "[default]TestTags/Temperature": "factory/zone1/temp"
    }
  }
}
```

See `mqtt-uns-config-combined-example.json` in the repository for a complete configuration example.

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
- `tagProviders`: List of tag providers to monitor
- `tagFolders`: Specific tag folders to monitor (format: `[provider]Path/To/Folder`)
- `valueDeadband`: Minimum value change to trigger publish
- `publishOnQualityChange`: Publish when quality changes
- `includeMetadata`: Include tag metadata in payload
- `topicMappings`: Array of topic mapping objects with:
  - `sourcePattern`: Tag path pattern (e.g., `[default]Site1/Area2`)
  - `topicPrefix`: MQTT topic prefix (e.g., `enterprise/site1/area2`)
  - `brokerId`: ID of the broker to publish to
  - `enabled`: Enable/disable this mapping

## Quick Start

1. **Download and Install**: Get the latest `.modl` file from [Releases](https://github.com/JonathanGrocott/ignition-mqtt/releases) and install via Config > Modules
2. **Setup Broker**: Navigate to Config > MQTT UNS Publisher > Broker Configuration and add your MQTT broker
3. **Configure Publishing**: Go to Tag Publishing Configuration and create topic mappings for your tags
4. **Monitor**: Check the Status Dashboard to verify brokers are connected and tags are publishing

## MQTT Broker Setup

The module works with any MQTT 3.1.1 compatible broker. Popular options:

**Local Development (Mosquitto)**:
```bash
# macOS
brew install mosquitto
brew services start mosquitto

# Test with:
mosquitto_sub -h localhost -t '#' -v
```

**Cloud Services**:
- HiveMQ Cloud (free tier available)
- AWS IoT Core
- Azure IoT Hub
- EMQX Cloud

For detailed setup instructions for various brokers, see the [MQTT documentation](https://mqtt.org/).

## Development Status

**Current Version**: 1.0.9 - Production Ready ✅

This module is production-ready with full multi-broker support and web UI configuration.

### Recent Updates (v1.0.9)
- ✅ Fixed broker connection when saving tag configuration
- ✅ Enhanced debug logging for troubleshooting
- ✅ Improved broker lifecycle management

### Completed Features
- ✅ Multi-broker support with dynamic connection management
- ✅ Web-based configuration UI (React + REST API)
- ✅ Database-backed configuration storage
- ✅ Advanced topic mapping with per-broker routing
- ✅ Real-time statistics and health monitoring
- ✅ Event-driven tag monitoring
- ✅ Automatic broker connection/disconnection based on topic mappings
- ✅ Test connection functionality

### Future Enhancements
- [ ] TLS/SSL support for secure MQTT connections
- [ ] Sparkplug B protocol support
- [ ] Custom payload templates
- [ ] Batch publishing for high-volume scenarios
- [ ] Comprehensive unit and integration tests

### Known Limitations
- TLS/SSL connections not yet supported (plaintext MQTT only)
- Sparkplug B protocol not yet supported
- Not compatible with Ignition Maker Edition (third-party modules not supported)

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
│           ├── TopicMapping.java
│           └── MqttModuleConfig.java
├── mqtt-gateway/                     # Gateway scope (main implementation)
│   ├── src/main/java/.../gateway/
│   │   ├── MqttGatewayHook.java         # Module entry point & lifecycle
│   │   ├── MultiBrokerManager.java      # Multi-broker connection management
│   │   ├── TagSubscriptionManager.java  # Tag monitoring & publishing
│   │   ├── MqttTopicMapper.java         # Topic mapping logic
│   │   ├── JsonPayloadBuilder.java      # JSON payload generation
│   │   ├── ModuleStatistics.java        # Performance metrics
│   │   ├── ModuleHealthStatus.java      # Health monitoring
│   │   ├── config/
│   │   │   └── ConfigurationManager.java  # Database config management
│   │   ├── records/                     # Database persistence
│   │   │   ├── MqttBrokerConfigRecord.java
│   │   │   ├── MqttTagConfigRecord.java
│   │   │   └── RecordMapper.java
│   │   └── web/                         # REST API
│   │       ├── MqttDataRoutes.java      # Config CRUD endpoints
│   │       └── MqttStatusRoute.java     # Status/statistics endpoint
│   ├── web-ui/                          # React web interface
│   │   ├── src/
│   │   │   ├── components/
│   │   │   │   ├── Configuration.tsx    # Main container
│   │   │   │   ├── BrokerManagement.tsx # Broker config UI
│   │   │   │   ├── TagConfiguration.tsx # Topic mapping UI
│   │   │   │   └── StatusDashboard.tsx  # Real-time monitoring
│   │   │   ├── api.ts                   # REST API client
│   │   │   ├── index.tsx                # Entry point
│   │   │   └── styles.css               # Styles
│   │   ├── package.json
│   │   └── webpack.config.js
│   └── src/main/resources/
│       └── mounted/                     # Webpack build output
│           └── mqtt-config.js           # Bundled React app
├── .github/workflows/                # CI/CD
│   ├── build.yml                     # Build on push
│   └── release.yml                   # Create releases on tags
├── build.gradle.kts                  # Root build configuration
├── settings.gradle.kts               # Gradle project settings
├── mqtt-uns-config-combined-example.json  # Example configuration
└── README.md                         # This file
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

The module uses a manager-based architecture with multi-broker support:

1. **MqttGatewayHook**: Module lifecycle management and coordination
2. **ConfigurationManager**: Database configuration storage and retrieval
3. **MultiBrokerManager**: Manages multiple MQTT broker connections simultaneously
4. **TagSubscriptionManager**: Event-driven tag change detection and publishing to appropriate brokers
5. **MqttTopicMapper**: Tag path to MQTT topic conversion with custom mappings
6. **JsonPayloadBuilder**: Configurable JSON payload generation
7. **ModuleStatistics**: Real-time performance metrics tracking
8. **ModuleHealthStatus**: Health monitoring and diagnostics
9. **REST API Routes**: Web UI backend (broker config, tag config, status, test connection)

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

**Quick Test:**
1. Install module from [Releases](https://github.com/JonathanGrocott/ignition-mqtt/releases)
2. Set up Mosquitto broker: `brew install mosquitto && brew services start mosquitto`
3. Subscribe to all topics: `mosquitto_sub -h localhost -t '#' -v`
4. Configure broker and topic mappings via web UI
5. Watch MQTT messages appear in real-time!

**Verify Installation:**
- Module shows "Running" status in Config > Modules
- Web UI accessible at Config > MQTT UNS Publisher
- Check Gateway logs for startup messages
- Status Dashboard shows broker connection status

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
1. Reduce number of monitored tags
2. Increase `valueDeadband` to filter noise and reduce unnecessary publishes

## Performance Tuning

**High Sensitivity (Low Deadband):**
```json
{
  "tags": {
    "valueDeadband": 0.01,
    "includeMetadata": false
  }
}
```

**Low Sensitivity (High Deadband):**
```json
{
  "tags": {
    "valueDeadband": 1.0,
    "includeMetadata": true
  }
}
```

**Quality-Based Only (no value changes):**
```json
{
  "tags": {
    "valueDeadband": 999999999,
    "publishOnQualityChange": true
  }
}
```

## Contributing

This is a reference implementation for Ignition SDK 8.3+ module development demonstrating:
- Multi-module Gradle project structure with Kotlin DSL
- Gateway module lifecycle management
- Multi-broker MQTT client integration (Eclipse Paho)
- Tag system integration with event-driven monitoring
- Database-backed configuration using PersistentRecords
- React-based web UI with REST API backend
- Real-time statistics and health monitoring
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
