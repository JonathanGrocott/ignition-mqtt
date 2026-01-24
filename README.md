# MQTT UNS Publisher Module for Ignition 8.3

An Ignition module that publishes tag data to an external MQTT broker in a Unified Namespace (UNS) structure with configurable JSON payloads.

## Features

- **MQTT Integration**: Connect to external MQTT brokers (Mosquitto, HiveMQ, etc.)
- **Tag Monitoring**: Subscribe to tag changes from selected providers/folders
- **Flexible Topic Mapping**: Direct tag path to MQTT topic conversion with customization support
- **Customizable Payloads**: Default JSON structure with value, quality, timestamp, and metadata, plus custom template support
- **Gateway Web UI**: Complete configuration interface in the Gateway web pages
- **Robust Connection Handling**: Automatic reconnection with exponential backoff
- **Change Detection**: Configurable deadband and quality change detection

## Requirements

- Ignition 8.3.0 or later
- Java 17
- Gradle 7.6 or later
- External MQTT broker (for runtime)

## Building

To build the module:

```bash
./gradlew build
```

The compiled `.modl` file will be located in `build/` directory.

## Installation

1. Build the module (see above)
2. Navigate to your Ignition Gateway web interface (typically `http://localhost:8088`)
3. Go to **Config > Modules**
4. Scroll down and click **Install or Upgrade a Module**
5. Select the `MQTT-UNS-Publisher.modl` file
6. Click **Install**

### Development Mode

For development, you may want to allow unsigned modules. Add this line to your `data/ignition.conf` file:

```
wrapper.java.additional.[index]=-Dignition.allowunsignedmodules=true
```

Replace `[index]` with the next available index number.

## Configuration

After installation, configure the module via the Gateway web interface:

1. Navigate to **Config > MQTT UNS Publisher** (configuration UI coming in Phase 5)
2. Configure your MQTT broker settings
3. Select tag providers and folders to publish
4. Customize topic mappings (optional)
5. Customize JSON payload format (optional)

## Development Status

This module is currently under active development. Current phase: **Phase 1 - Project Setup**

### Completed
- [x] Gradle project structure
- [x] Module metadata and build configuration
- [x] Basic Gateway Hook implementation
- [x] Eclipse Paho MQTT dependency

### In Progress
- [ ] MQTT connection management (Phase 2)
- [ ] Tag subscription system (Phase 3)
- [ ] Topic mapping and payload generation (Phase 4)
- [ ] Gateway web UI (Phase 5)
- [ ] Custom payload templates (Phase 6)
- [ ] Testing and documentation (Phases 7-8)

## Project Structure

```
ignition-mqtt/
├── mqtt-common/              # Common scope (shared models)
├── mqtt-gateway/             # Gateway scope (main implementation)
├── build.gradle.kts          # Root build configuration
├── settings.gradle.kts       # Gradle settings
└── README.md                 # This file
```

## Architecture

See [docs/MQTT_MODULE_PLAN.md](.opencode/plans/mqtt-module-plan.md) for detailed architecture and implementation plan.

## License

Copyright © 2026. All rights reserved.

## Contributing

This is a reference implementation for Ignition SDK module development.
