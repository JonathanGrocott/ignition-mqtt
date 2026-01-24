# Phase 2: MQTT Connection Management - Testing Guide

## What's New in Phase 2

Phase 2 adds complete MQTT broker connection management:

- ✅ **MqttPublisherManager** - Full connection lifecycle management
- ✅ **Reconnection Logic** - Exponential backoff with configurable max attempts
- ✅ **Configuration Storage** - JSON file-based config persistence
- ✅ **Connection Health** - State tracking and monitoring
- ✅ **Thread Safety** - Concurrent operation support

## Components Added

### 1. MqttBrokerConfig
Configuration model with all MQTT connection parameters:
- Broker URL (tcp:// or ssl://)
- Client ID (unique identifier)
- Authentication (username/password)
- TLS support flag
- QoS level (0, 1, 2)
- Retained messages flag
- Keep-alive interval
- Connection timeout
- Clean session flag

### 2. ConnectionState Enum
Tracks connection status:
- `DISCONNECTED` - Not connected
- `CONNECTING` - Attempting connection
- `CONNECTED` - Successfully connected
- `RECONNECTING` - Connection lost, retrying
- `ERROR` - Failed, stopped trying

### 3. MqttPublisherManager
Main MQTT connection manager:
- Connects to broker with configuration
- Publishes messages (with QoS and retain)
- Auto-reconnects with exponential backoff
- Thread-safe operations
- Graceful shutdown

### 4. ConfigurationManager
Simple JSON-based configuration:
- Loads config from `data/mqtt-uns-config.json`
- Saves config with validation
- Returns defaults if no config exists

## Installation & Testing

### Prerequisites

1. **MQTT Broker** - You need an MQTT broker running. Options:
   
   **Option A: Mosquitto (Recommended)**
   ```bash
   # macOS
   brew install mosquitto
   brew services start mosquitto
   # Runs on tcp://localhost:1883
   
   # Or run without service
   /opt/homebrew/opt/mosquitto/sbin/mosquitto -c /opt/homebrew/etc/mosquitto/mosquitto.conf
   ```
   
   **Option B: Docker**
   ```bash
   docker run -d -p 1883:1883 --name mosquitto eclipse-mosquitto:latest
   ```
   
   **Option C: HiveMQ Cloud** (free tier available)
   - Sign up at https://www.hivemq.com/cloud/
   - Create a cluster and use provided URL/credentials

2. **MQTT Client Tool** (for testing)
   ```bash
   # macOS
   brew install mosquitto  # includes mosquitto_sub/pub
   
   # Or use MQTT Explorer GUI
   brew install --cask mqtt-explorer
   ```

### Step 1: Build the Module

```bash
./build.sh
```

Output: `build/MQTT-UNS-Publisher.unsigned.modl`

### Step 2: Install on Ignition Gateway

1. Open Gateway web interface: http://localhost:8088
2. Go to **Config > Modules**
3. Click **Install or Upgrade a Module**
4. Select `build/MQTT-UNS-Publisher.unsigned.modl`
5. Click **Install**
6. Wait for module to appear as "Running"

### Step 3: Configure MQTT Connection

**Method 1: Copy Example Config**

```bash
# Find your Ignition data directory (usually one of these):
# /var/lib/ignition/data/
# /usr/local/ignition/data/
# C:\Program Files\Inductive Automation\Ignition\data\

# Copy example config
cp mqtt-uns-config-example.json /path/to/ignition/data/mqtt-uns-config.json

# Edit the config
nano /path/to/ignition/data/mqtt-uns-config.json
```

**Method 2: Create Config Manually**

Create `/path/to/ignition/data/mqtt-uns-config.json`:

```json
{
  "brokerUrl": "tcp://localhost:1883",
  "clientId": "ignition-mqtt-test",
  "username": "",
  "password": "",
  "useTls": false,
  "qos": 1,
  "retained": false,
  "keepAlive": 60,
  "connectionTimeout": 30,
  "cleanSession": true
}
```

**For HiveMQ Cloud or other secured brokers:**
```json
{
  "brokerUrl": "tcp://your-broker.hivemq.cloud:1883",
  "clientId": "ignition-mqtt-publisher",
  "username": "your-username",
  "password": "your-password",
  "useTls": true,
  "qos": 1,
  "retained": false,
  "keepAlive": 60,
  "connectionTimeout": 30,
  "cleanSession": true
}
```

### Step 4: Restart Module to Apply Config

After creating the config file:

1. Go to **Config > Modules**
2. Find "MQTT UNS Publisher"
3. Click **Restart Module**

Or restart the entire Gateway.

### Step 5: Verify Connection

**Check Gateway Logs**

Location: `logs/wrapper.log`

Look for:
```
[MQTT UNS Publisher] Loaded MQTT configuration: tcp://localhost:1883
[MQTT UNS Publisher] Initiating MQTT connection to tcp://localhost:1883
[MqttPublisherManager] Successfully connected to MQTT broker: tcp://localhost:1883
```

If connection fails, you'll see:
```
[MqttPublisherManager] Failed to connect to MQTT broker: tcp://localhost:1883 - Connection refused
[MqttPublisherManager] Scheduling reconnection attempt 1 in 1000 ms
```

**Monitor with MQTT Client**

In a terminal, subscribe to all topics to see when the client connects:

```bash
mosquitto_sub -h localhost -t '#' -v
```

You won't see any messages yet (we're not publishing), but the broker logs will show the connection.

**Check Mosquitto Logs** (if using Mosquitto):

```bash
# macOS Homebrew
tail -f /opt/homebrew/var/log/mosquitto/mosquitto.log

# Look for:
# New client connected from 127.0.0.1:xxxxx as ignition-mqtt-test
```

## Testing Reconnection

### Test 1: Restart Broker

```bash
# Stop broker
brew services stop mosquitto

# Watch Gateway logs - should see:
# [MqttPublisherManager] MQTT connection lost: Connection lost
# [MqttPublisherManager] Scheduling reconnection attempt 1 in 1000 ms
# [MqttPublisherManager] Scheduling reconnection attempt 2 in 2000 ms
# [MqttPublisherManager] Scheduling reconnection attempt 3 in 4000 ms

# Start broker
brew services start mosquitto

# Should see:
# [MqttPublisherManager] Successfully connected to MQTT broker: tcp://localhost:1883
```

### Test 2: Invalid Configuration

Edit config with invalid broker URL and restart module:

```json
{
  "brokerUrl": "tcp://nonexistent:1883",
  ...
}
```

Should see exponential backoff in logs:
```
Attempt 1: 1 second delay
Attempt 2: 2 second delay  
Attempt 3: 4 second delay
Attempt 4: 8 second delay
...
Attempt 10: 30 second delay (max)
```

After 10 attempts, should see:
```
[MqttPublisherManager] Exceeded maximum reconnection attempts (10). Giving up.
```

## Current Limitations

Phase 2 provides connection management only. The module:

- ✅ Connects to MQTT broker
- ✅ Reconnects automatically on failure
- ✅ Monitors connection health
- ❌ Does NOT yet subscribe to Ignition tags (Phase 3)
- ❌ Does NOT yet publish any data (Phase 3)
- ❌ Does NOT yet have web UI for config (Phase 5)

## Troubleshooting

### Connection Refused

**Problem:** `Connection refused (Connection refused)`

**Solutions:**
- Verify broker is running: `brew services list | grep mosquitto`
- Check broker is listening: `netstat -an | grep 1883` or `lsof -i :1883`
- Try connecting with mosquitto_pub: `mosquitto_pub -h localhost -t test -m "hello"`

### Unknown Host

**Problem:** `UnknownHostException: your-broker`

**Solutions:**
- Check broker URL is correct
- Verify DNS resolution: `ping your-broker`
- Try IP address instead of hostname

### Authentication Failed

**Problem:** `Not authorized (5)`

**Solutions:**
- Verify username/password in config
- Check broker authentication settings
- For Mosquitto, check `/opt/homebrew/etc/mosquitto/mosquitto.conf`

### Module Won't Start

**Problem:** Module shows error in status

**Solutions:**
- Check wrapper.log for Java exceptions
- Verify module is built for Ignition 8.3+
- Check all dependencies are included in .modl file

## Configuration Reference

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| brokerUrl | String | tcp://localhost:1883 | MQTT broker URL (tcp:// or ssl://) |
| clientId | String | ignition-mqtt-[random] | Unique client identifier |
| username | String | "" | MQTT username (optional) |
| password | String | "" | MQTT password (optional) |
| useTls | Boolean | false | Enable TLS/SSL (not yet implemented) |
| qos | Integer | 1 | Quality of Service (0, 1, or 2) |
| retained | Boolean | false | Retain published messages |
| keepAlive | Integer | 60 | Keep-alive interval (seconds) |
| connectionTimeout | Integer | 30 | Connection timeout (seconds) |
| cleanSession | Boolean | true | Clean session on connect |

## Next Steps

**Phase 3** will add:
- Tag subscription system
- Tag change detection
- Integration with MQTT publisher
- First actual data publishing!

---

**Phase 2 Status:** ✅ Complete  
**Module Size:** ~220 KB  
**Phase:** 2 of 8 Complete
