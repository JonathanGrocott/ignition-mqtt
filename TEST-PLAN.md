# MQTT UNS Publisher Module - End-to-End Test Plan

## Installation

**Module File:** `build/MQTT-UNS-Publisher.unsigned.modl` (354 KB, built Jan 25 12:13 PM)

1. Navigate to Gateway Config: `http://localhost:8088/web/config/system.modules`
2. Remove old version if present
3. Install new module
4. Restart Gateway if prompted

## Test 1: Broker Configuration & Connection

### Steps:
1. Navigate to: `http://localhost:8088/web/config/mqtt.uns.publisher.configuration`
2. Go to "Broker Settings" tab
3. Configure MQTT broker:
   - **Broker URL:** `tcp://localhost:1883` (or your MQTT broker)
   - **Client ID:** `ignition-mqtt-test`
   - **Username/Password:** (if required)
   - **QoS:** 1
   - **Keep Alive:** 60
   - **Connection Timeout:** 30
   - Check "Enable MQTT publishing"

4. Click **"Test Connection"** button
   - ✅ Should show: "Connection successful! (Xms)"
   - ❌ If fails: Check broker is running and accessible

5. Click **"Save Configuration"** button
   - ✅ Should show: "Configuration saved successfully"
   - Config should persist on page reload

### Verify in Gateway Logs:
```bash
tail -f /usr/local/ignition/logs/wrapper.log | grep -i mqtt
```

Expected log entries:
```
Successfully connected to MQTT broker: tcp://localhost:1883
Saved broker configuration: tcp://localhost:1883
```

---

## Test 2: Tag Publishing Configuration

### Prerequisites:
- At least one tag provider exists (default provider is fine)
- Some tags exist to publish

### Steps:
1. Go to "Tag Publishing" tab
2. Configure tag publishing:
   - Check "Enable tag publishing"
   - **Tag Providers:** Select "default" (or your provider)
   - **Tag Folders:** Add a folder path like `[default]TestTags`
   - **Include Metadata:** Checked
   - **Publish on Quality Change:** Checked
   - **Value Deadband:** 0.0

3. Click **"Save Configuration"**
   - ✅ Should show: "Configuration saved successfully"

### Verify in Gateway Logs:
```
Saved tag configuration: Default Tag Publishing
Starting event-driven tag subscription manager
Subscribed to X tags from Y providers
```

---

## Test 3: End-to-End MQTT Publishing

### Prerequisites:
1. **MQTT Broker Running:**
   ```bash
   # If using Mosquitto locally:
   brew services start mosquitto
   # Or Docker:
   docker run -d -p 1883:1883 eclipse-mosquitto
   ```

2. **MQTT Client for Monitoring:**
   ```bash
   # Install mosquitto clients
   brew install mosquitto
   
   # Subscribe to all topics
   mosquitto_sub -h localhost -t '#' -v
   ```

### Steps:

1. **Create Test Tags** (if needed):
   - Go to: `http://localhost:8088/web/config/tags.browse`
   - Create a folder: `TestTags` under `[default]`
   - Create tags:
     - `TestTags/Temperature` (Integer, value: 72)
     - `TestTags/Humidity` (Integer, value: 45)
     - `TestTags/Status` (String, value: "OK")

2. **Configure Module to Publish These Tags:**
   - Broker Settings: Enable and save with `tcp://localhost:1883`
   - Tag Publishing: Add `[default]TestTags` folder, enable, and save

3. **Monitor MQTT Broker:**
   ```bash
   mosquitto_sub -h localhost -t '#' -v
   ```

4. **Change Tag Values:**
   - Go to Tag Browser
   - Edit `Temperature` value from 72 → 75
   - Edit `Humidity` value from 45 → 50

5. **Verify MQTT Messages Published:**

Expected output in `mosquitto_sub`:
```
uns/ignition/default/TestTags/Temperature {"value":75,"timestamp":1706213456789,"quality":"Good"}
uns/ignition/default/TestTags/Humidity {"value":50,"timestamp":1706213457123,"quality":"Good"}
```

### Troubleshooting:

**No messages received:**
1. Check Gateway logs for errors:
   ```bash
   tail -f /usr/local/ignition/logs/wrapper.log | grep -i "mqtt\|error\|exception"
   ```

2. Verify connection state in Status tab:
   - Should show "CONNECTED"
   - Messages Published count should increase

3. Check broker accessibility:
   ```bash
   mosquitto_pub -h localhost -t test -m "hello"
   ```

**Tags not monitored:**
- Verify tag folder path exactly matches (e.g., `[default]TestTags`)
- Check tag provider name is correct
- Ensure "Enable tag publishing" is checked

---

## Test 4: Status Dashboard

### Steps:
1. Go to "Status" tab
2. Verify displays:
   - **Health Status:** Should be "Healthy" (green) when connected
   - **Connection State:** "CONNECTED"
   - **Broker URL:** Your configured broker
   - **Messages Published:** Should increase when tags change
   - **Success Rate:** Should be ~100%
   - **Monitored Tag Count:** Number of tags being watched

### Expected Behavior:
- Status updates automatically (polling every few seconds)
- Statistics reflect actual publishing activity
- Connection state matches actual broker connection

---

## Test 5: Configuration Persistence

### Steps:
1. Configure broker and tags (Tests 1 & 2)
2. Save all configurations
3. **Restart Ignition Gateway:**
   ```bash
   # SSH to server running Ignition
   systemctl restart ignition  # or service ignition restart
   ```
4. After restart, navigate back to module config page
5. Verify:
   - ✅ Broker configuration loaded correctly
   - ✅ Tag configuration loaded correctly
   - ✅ Module reconnects to MQTT automatically
   - ✅ Tag publishing resumes automatically

### Check Gateway Logs:
```
Loading MQTT configuration from database
Successfully connected to MQTT broker: tcp://localhost:1883
Starting event-driven tag subscription manager
Subscribed to X tags
```

---

## Test 6: Error Handling

### Test 6A: Invalid Broker Connection
1. Configure broker with invalid URL: `tcp://invalid-host:1883`
2. Click "Test Connection"
   - ✅ Should show error: "Connection failed: Connection refused" (or similar)
3. Click "Save Configuration" anyway
4. Check Status tab:
   - Health: "Unhealthy" (red)
   - Connection State: "DISCONNECTED" or "ERROR"
   - Status Message: Explains connection failure

### Test 6B: Broker Disconnect During Operation
1. Start with working configuration and publishing
2. Stop MQTT broker:
   ```bash
   brew services stop mosquitto
   # or: docker stop <mosquitto-container>
   ```
3. Check Status tab:
   - Should show "DISCONNECTED"
   - Messages Failed count may increase
4. Restart broker
5. Verify:
   - ✅ Module auto-reconnects (check logs for "reconnect attempt")
   - ✅ Publishing resumes automatically

---

## Success Criteria

### ✅ All Features Working:
1. Broker configuration saves and loads
2. Tag configuration saves and loads
3. Test connection validates broker connectivity
4. Tags are monitored when configured
5. Tag changes trigger MQTT publishes
6. Messages appear in MQTT broker with correct topics/payloads
7. Status dashboard shows accurate statistics
8. Configuration persists across Gateway restarts
9. Module auto-reconnects on broker disconnect
10. Error states handled gracefully with user feedback

---

## Performance Verification

### Expected Performance:
- **Tag Change Latency:** < 100ms from tag change to MQTT publish
- **Throughput:** Can handle 100+ tag changes per second
- **Memory:** < 50 MB additional heap usage
- **CPU:** < 5% CPU during normal operation

### Monitor Performance:
```bash
# Gateway logs with timestamps
tail -f /usr/local/ignition/logs/wrapper.log | grep -i mqtt

# System resource usage
top -pid $(pgrep -f ignition)
```

---

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| "Name must be NOT NULL" error | Database constraint | ✅ Fixed in v12:13 build |
| "Cannot cast String to Number" error | Type conversion | ✅ Fixed in v12:13 build |
| POST returns 404 | HTTP method not allowed | ✅ Fixed in v12:13 build |
| Tags not publishing | Folder path mismatch | Check exact path including `[provider]` |
| Connection fails | Broker not running | Start MQTT broker first |
| No messages in MQTT | Publishing disabled | Enable in both Broker and Tag config |

---

## Module Architecture

### Components:
- **MqttGatewayHook** - Main module entry point
- **MqttPublisherManager** - Handles MQTT connection and publishing
- **TagSubscriptionManager** - Monitors tags for changes
- **MqttTagChangeListener** - Event-driven tag change handler
- **ConfigurationManager** - Loads/saves configuration from database
- **MqttDataRoutes** - REST API endpoints for web UI
- **React Web UI** - Configuration interface

### Data Flow:
```
Tag Change Event → TagSubscriptionManager → MqttPublisherManager → MQTT Broker
                                                                         ↓
                                                                   Subscribers
```

### API Endpoints:
```
GET  /data/mqtt-uns-publisher/config/broker  - Load broker config
POST /data/mqtt-uns-publisher/config/broker  - Save broker config
GET  /data/mqtt-uns-publisher/config/tags    - Load tag config
POST /data/mqtt-uns-publisher/config/tags    - Save tag config
GET  /data/mqtt-uns-publisher/status         - Get module status
POST /data/mqtt-uns-publisher/test-connection - Test broker connection
```

---

## Next Steps

After verifying basic functionality:
1. Test with real production tags
2. Configure topic naming conventions
3. Customize payload templates (if needed)
4. Set appropriate deadbands for noisy tags
5. Monitor performance under load
6. Set up MQTT broker redundancy/clustering
7. Configure TLS/SSL for production security

---

## Support & Debugging

**Enable Debug Logging:**
1. Go to Gateway Config → Logging
2. Add logger: `com.inductiveautomation.ignition.examples.mqtt`
3. Set level to `DEBUG` or `TRACE`
4. Restart module

**Log Locations:**
- Gateway: `/usr/local/ignition/logs/wrapper.log`
- Module-specific: Look for `module-name=MQTT UNS Publisher`

**Database Inspection:**
```sql
-- View broker config
SELECT * FROM MqttBrokerConfig;

-- View tag config  
SELECT * FROM MqttTagConfig;
```

---

**Module Version:** 1.0.0-SNAPSHOT (Build 2026-01-25 12:13)
**Ignition Version:** 8.3.0
**Last Updated:** January 25, 2026
