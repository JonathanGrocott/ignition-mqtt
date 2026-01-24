# Quick Start Guide

## Phase 1 Complete! ✓

The basic module skeleton has been successfully created and built.

## What's Been Accomplished

1. **Project Structure**
   - Gradle multi-module build with Kotlin DSL
   - `mqtt-common` module for shared code
   - `mqtt-gateway` module for Gateway implementation
   - Gradle wrapper for consistent builds

2. **Module Configuration**
   - Module ID: `com.inductiveautomation.mqtt.uns`
   - Module Name: "MQTT UNS Publisher"
   - Version: 1.0.0-SNAPSHOT
   - Requires Ignition 8.3.0+
   - Gateway scope only

3. **Dependencies**
   - Eclipse Paho MQTT Client 1.2.5 (bundled)
   - Ignition SDK 8.3.0 (provided)
   - SLF4J logging (provided)
   - Gson for JSON (provided)

4. **Core Components**
   - `MqttGatewayHook` - Module lifecycle management
   - `MqttModuleConstants` - Shared constants
   - Basic logging infrastructure

## Building the Module

```bash
# Set JAVA_HOME (required for Gradle)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

# Build the module
./gradlew build

# The output will be in:
# build/MQTT-UNS-Publisher.unsigned.modl (216 KB)
```

## Installing the Module

1. Navigate to your Ignition Gateway web interface (http://localhost:8088)
2. Go to **Config > Modules**
3. Scroll down to **Install or Upgrade a Module**
4. Click **Choose File** and select `build/MQTT-UNS-Publisher.unsigned.modl`
5. Click **Install**

**Note**: Make sure your Gateway is configured to allow unsigned modules:
```
# In data/ignition.conf, add:
wrapper.java.additional.[N]=-Dignition.allowunsignedmodules=true
```

## Verifying Installation

After installation, you should see:
- Module appears in the module list as "MQTT UNS Publisher"
- Status shows as "Running"
- Check Gateway logs for startup messages:
  ```
  Setting up MQTT UNS Publisher module (ID: com.inductiveautomation.mqtt.uns)
  Starting up MQTT UNS Publisher module
  MQTT UNS Publisher module started successfully
  ```

## Project Structure

```
ignition-mqtt/
├── build/                           # Build output
│   └── MQTT-UNS-Publisher.unsigned.modl
├── mqtt-common/                     # Common scope
│   ├── build.gradle.kts
│   └── src/main/java/.../common/
│       └── MqttModuleConstants.java
├── mqtt-gateway/                    # Gateway scope
│   ├── build.gradle.kts
│   └── src/main/java/.../gateway/
│       └── MqttGatewayHook.java
├── gradle/                          # Gradle wrapper
├── build.gradle.kts                 # Root build config
├── settings.gradle.kts              # Module settings
├── gradlew                          # Gradle wrapper script
└── README.md
```

## What's Next?

Phase 2 will implement:
- MQTT connection management
- Configuration storage
- Reconnection logic
- Health monitoring

## Development Notes

- Uses Java 17 toolchain
- Gradle 8.5 build system
- Ignition Module Tools Plugin 0.3.0
- Module is marked as unsigned (development mode)

## Current Limitations

This is a skeleton module. It does not yet:
- Connect to MQTT brokers
- Subscribe to tags
- Publish any data

These features will be implemented in subsequent phases.

---

**Build Status**: ✅ Success  
**Module Size**: 216 KB  
**Phase**: 1 of 8 Complete
