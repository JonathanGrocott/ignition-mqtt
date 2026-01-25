# MQTT Broker Setup Guide

This guide covers setting up popular MQTT brokers for use with the MQTT UNS Publisher module.

## Table of Contents

- [Mosquitto (Open Source)](#mosquitto-open-source)
- [HiveMQ Community Edition](#hivemq-community-edition)
- [EMQX](#emqx)
- [AWS IoT Core](#aws-iot-core)
- [Azure IoT Hub](#azure-iot-hub)
- [Testing Your Connection](#testing-your-connection)

---

## Mosquitto (Open Source)

Eclipse Mosquitto is a lightweight, open-source MQTT broker ideal for development and production use.

### Installation

**macOS (Homebrew):**
```bash
brew install mosquitto
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install mosquitto mosquitto-clients
```

**Docker:**
```bash
docker run -d --name mosquitto \
  -p 1883:1883 \
  -p 9001:9001 \
  eclipse-mosquitto
```

### Basic Configuration

Create `/etc/mosquitto/mosquitto.conf` (or use Docker volume):

```conf
# Basic configuration
listener 1883
allow_anonymous true

# Persistence
persistence true
persistence_location /var/lib/mosquitto/

# Logging
log_dest file /var/log/mosquitto/mosquitto.log
log_type all
```

### Configuration with Authentication

```conf
listener 1883
allow_anonymous false
password_file /etc/mosquitto/passwd

persistence true
persistence_location /var/lib/mosquitto/
```

Create password file:
```bash
sudo mosquitto_passwd -c /etc/mosquitto/passwd ignition
# Enter password when prompted
```

### Start Mosquitto

```bash
# macOS (Homebrew)
brew services start mosquitto

# Linux (systemd)
sudo systemctl start mosquitto
sudo systemctl enable mosquitto

# Docker
docker start mosquitto
```

### Ignition Configuration for Mosquitto

```json
{
  "broker": {
    "brokerUrl": "tcp://localhost:1883",
    "clientId": "ignition-mqtt-publisher",
    "username": "ignition",
    "password": "your-password",
    "qos": 1,
    "retained": false,
    "cleanSession": true,
    "connectionTimeout": 30,
    "keepAliveInterval": 60
  }
}
```

---

## HiveMQ Community Edition

HiveMQ CE is a Java-based MQTT broker with excellent performance and monitoring capabilities.

### Installation with Docker

```bash
docker run -d --name hivemq \
  -p 1883:1883 \
  -p 8080:8080 \
  hivemq/hivemq-ce
```

### Access Web UI

Navigate to `http://localhost:8080` to access the HiveMQ Control Center.

### Configuration

Create `config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<hivemq>
    <mqtt>
        <session-expiry>
            <max-interval>86400</max-interval>
        </session-expiry>
    </mqtt>
    
    <persistence>
        <mode>file</mode>
    </persistence>
</hivemq>
```

### Ignition Configuration for HiveMQ

```json
{
  "broker": {
    "brokerUrl": "tcp://localhost:1883",
    "clientId": "ignition-mqtt-publisher",
    "username": "",
    "password": "",
    "qos": 1,
    "retained": true,
    "cleanSession": false
  }
}
```

---

## EMQX

EMQX is a scalable, distributed MQTT broker with built-in monitoring dashboard.

### Installation with Docker

```bash
docker run -d --name emqx \
  -p 1883:1883 \
  -p 8083:8083 \
  -p 8883:8883 \
  -p 8084:8084 \
  -p 18083:18083 \
  emqx/emqx:latest
```

### Access Dashboard

Navigate to `http://localhost:18083`
- Default username: `admin`
- Default password: `public`

### Configuration

Access via dashboard or edit `/etc/emqx/emqx.conf`:

```conf
## MQTT Protocol
mqtt.max_packet_size = 1MB
mqtt.max_clientid_len = 65535
mqtt.max_topic_levels = 128

## Session
mqtt.session.max_inflight = 32
mqtt.session.max_awaiting_rel = 100

## Retention
mqtt.retain.max_payload_size = 1MB
mqtt.retain.max_retained_messages = 10000
```

### Ignition Configuration for EMQX

```json
{
  "broker": {
    "brokerUrl": "tcp://localhost:1883",
    "clientId": "ignition-mqtt-publisher",
    "username": "",
    "password": "",
    "qos": 1,
    "retained": true,
    "cleanSession": false
  }
}
```

---

## AWS IoT Core

AWS IoT Core is a managed cloud MQTT broker with built-in security and device management.

### Prerequisites

- AWS Account
- AWS CLI configured
- IoT device certificate and private key

### Setup Steps

1. **Create an IoT Thing:**
```bash
aws iot create-thing --thing-name ignition-publisher
```

2. **Create and attach certificate:**
```bash
aws iot create-keys-and-certificate \
  --set-as-active \
  --certificate-pem-outfile cert.pem \
  --public-key-outfile public.key \
  --private-key-outfile private.key
```

3. **Attach policy to certificate:**
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["iot:Publish", "iot:Connect"],
    "Resource": ["*"]
  }]
}
```

4. **Get your endpoint:**
```bash
aws iot describe-endpoint --endpoint-type iot:Data-ATS
```

### Ignition Configuration for AWS IoT Core

**Note:** AWS IoT Core requires TLS. The module currently doesn't support TLS, so this is for future reference.

```json
{
  "broker": {
    "brokerUrl": "ssl://your-endpoint.iot.us-east-1.amazonaws.com:8883",
    "clientId": "ignition-publisher",
    "qos": 1,
    "retained": false,
    "cleanSession": true,
    "useTls": true,
    "certFile": "/path/to/cert.pem",
    "keyFile": "/path/to/private.key",
    "caFile": "/path/to/AmazonRootCA1.pem"
  }
}
```

---

## Azure IoT Hub

Azure IoT Hub is Microsoft's managed IoT platform with MQTT support.

### Setup Steps

1. **Create IoT Hub** in Azure Portal

2. **Register Device:**
```bash
az iot hub device-identity create \
  --hub-name your-hub-name \
  --device-id ignition-publisher
```

3. **Get connection string:**
```bash
az iot hub device-identity connection-string show \
  --hub-name your-hub-name \
  --device-id ignition-publisher
```

### Ignition Configuration for Azure IoT Hub

**Note:** Azure IoT Hub uses special MQTT topic format and requires TLS.

```json
{
  "broker": {
    "brokerUrl": "ssl://your-hub.azure-devices.net:8883",
    "clientId": "ignition-publisher",
    "username": "your-hub.azure-devices.net/ignition-publisher/?api-version=2021-04-12",
    "password": "SharedAccessSignature sr=...",
    "qos": 1,
    "retained": false,
    "cleanSession": true
  }
}
```

---

## Testing Your Connection

### Using Mosquitto Client Tools

**Subscribe to all topics:**
```bash
mosquitto_sub -h localhost -t '#' -v
```

**Publish test message:**
```bash
mosquitto_pub -h localhost -t 'test/topic' -m 'Hello MQTT'
```

**With authentication:**
```bash
mosquitto_sub -h localhost -t '#' -v -u ignition -P your-password
```

### Using MQTT Explorer (GUI)

Download MQTT Explorer: http://mqtt-explorer.com

1. Launch MQTT Explorer
2. Click "+" to add new connection
3. Enter:
   - Name: Ignition Test
   - Host: localhost
   - Port: 1883
   - Username/Password (if configured)
4. Click "Connect"
5. Browse topics in the left sidebar

### Using Python (paho-mqtt)

```python
import paho.mqtt.client as mqtt

def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")
    client.subscribe("#")

def on_message(client, userdata, msg):
    print(f"{msg.topic}: {msg.payload.decode()}")

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

# client.username_pw_set("ignition", "password")  # If auth enabled
client.connect("localhost", 1883, 60)
client.loop_forever()
```

### Verifying Ignition Module Connection

Once you've installed the module in Ignition, check the Gateway logs:

```bash
tail -f <ignition-install-dir>/logs/wrapper.log
```

Look for:
```
INFO  [MqttGatewayHook] Successfully connected to MQTT broker: tcp://localhost:1883
INFO  [TagSubscriptionManager] Monitoring 15 tags
```

---

## Common Configuration Patterns

### High-Frequency Data (100ms polling)

```json
{
  "tags": {
    "enabled": true,
    "pollRateMs": 100,
    "valueDeadband": 0.01,
    "publishOnQualityChange": true,
    "includeMetadata": false
  }
}
```

### Low-Frequency Monitoring (5 seconds)

```json
{
  "tags": {
    "enabled": true,
    "pollRateMs": 5000,
    "valueDeadband": 1.0,
    "publishOnQualityChange": true,
    "includeMetadata": true
  }
}
```

### Quality-Based Publishing Only

```json
{
  "tags": {
    "enabled": true,
    "pollRateMs": 1000,
    "valueDeadband": 999999999,
    "publishOnQualityChange": true,
    "includeMetadata": true
  }
}
```

---

## Troubleshooting

### Connection Refused

**Problem:** `Connection refused` error in logs

**Solutions:**
1. Verify broker is running: `netstat -an | grep 1883`
2. Check firewall rules
3. Verify broker URL format: `tcp://hostname:port`
4. Test with mosquitto_pub/sub

### Authentication Failed

**Problem:** `Not authorized` or `Bad username or password`

**Solutions:**
1. Verify credentials in config
2. Check broker password file
3. Try with `allow_anonymous true` temporarily
4. Review broker authentication logs

### Messages Not Publishing

**Problem:** Module connected but no messages

**Solutions:**
1. Check tag configuration paths
2. Verify tags exist in Ignition
3. Review deadband settings (might be too high)
4. Check module logs for errors
5. Use health check: `getHealthStatus()` in console

### High CPU Usage

**Problem:** Module consuming excessive CPU

**Solutions:**
1. Increase `pollRateMs` (default: 1000ms)
2. Reduce number of monitored tags
3. Increase `valueDeadband` to filter noise
4. Check for tag read errors in logs

---

## Best Practices

1. **Use Retained Messages for State:** Set `retained: true` for device state topics
2. **Clean Session for Testing:** Use `cleanSession: true` during development
3. **QoS 1 for Important Data:** Use `qos: 1` for critical telemetry
4. **Topic Structure:** Follow UNS patterns like `enterprise/site/area/line/device/metric`
5. **Monitor Statistics:** Regularly check module statistics for health
6. **Deadband Tuning:** Set appropriate deadband to reduce unnecessary publishes
7. **Poll Rate Optimization:** Balance between latency and system load

---

## Additional Resources

- [MQTT.org](https://mqtt.org) - MQTT specification and docs
- [HiveMQ MQTT Essentials](https://www.hivemq.com/mqtt-essentials/) - Comprehensive MQTT guide
- [Unified Namespace (UNS)](https://www.unified-namespace.com) - UNS architecture patterns
- [Ignition SDK Docs](https://www.sdk-docs.inductiveautomation.com) - Ignition module development
