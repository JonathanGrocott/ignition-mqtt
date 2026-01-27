# Gateway UI Configuration Research

## Executive Summary

Based on research of the Ignition 8.3 SDK documentation and official examples, there are **TWO excellent approaches** to integrate module configuration into the Gateway UI, completely replacing JSON file configuration:

### ✅ **Option 1: PersistentRecords + React Web UI** (Recommended)
- **Native Ignition approach** - Used by all built-in modules (OPC-UA, Alarm Notification, etc.)
- **Best user experience** - Integrated into Gateway web interface
- **Automatic replication** - Config synced to redundant Gateways automatically
- **Rich UI** - Full React components, validation, real-time updates
- **Moderate complexity** - Backend: PersistentRecords, Frontend: React/TypeScript

### ❌ **Option 2: Legacy Config Files** (Current approach)
- **Not recommended** - No built-in modules use this approach
- **Poor UX** - Manual JSON editing, no validation, requires Gateway restart
- **No redundancy** - Must manually sync files to redundant nodes
- **Simple** - Easy to implement but not production-quality

## Recommended Approach: PersistentRecords + React Web UI

This is the **official Ignition way** to build module configuration. All built-in modules use this approach.

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Gateway Web UI (React)                    │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Configuration Page (/app/mqtt-uns-publisher)         │ │
│  │  - Broker settings form                               │ │
│  │  - Tag selection browser                              │ │
│  │  - Topic mapping table                                │ │
│  │  - Status dashboard                                   │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            ↕ HTTP/JSON
┌─────────────────────────────────────────────────────────────┐
│              Gateway Backend (Java)                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  REST API Routes (/data/mqtt-uns-publisher/*)        │ │
│  │  - GET  /config    → Read configuration               │ │
│  │  - POST /config    → Save configuration               │ │
│  │  - GET  /status    → Module health/stats              │ │
│  │  - POST /test      → Test MQTT connection             │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  PersistentRecord (ORM)                               │ │
│  │  - MqttBrokerConfigRecord                             │ │
│  │  - MqttTagConfigRecord                                │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│         Internal Database (SQLite/Postgres)                  │
│  - Automatic schema creation                                 │
│  - Automatic replication to redundant Gateways              │
│  - Transaction support                                       │
│  - Record change listeners                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Part 1: Backend - PersistentRecords

### What are PersistentRecords?

PersistentRecords are Ignition's ORM (Object-Relational Mapping) system for storing configuration data in the internal database. They provide:

- **Automatic table creation** - Define fields, Ignition creates the table
- **Type safety** - StringField, IntField, BooleanField, etc.
- **Relationships** - ReferenceField to link records together
- **Redundancy** - Automatically replicated to backup Gateways
- **Change listeners** - Get notified when records are modified
- **Transaction support** - ACID guarantees for data consistency

### Step 1: Define PersistentRecord Classes

Create Java classes that define your configuration schema:

**File:** `mqtt-gateway/src/main/java/com/inductiveautomation/ignition/examples/mqtt/gateway/records/MqttBrokerConfigRecord.java`

```java
package com.inductiveautomation.ignition.examples.mqtt.gateway.records;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import simpleorm.dataset.SFieldFlags;

/**
 * Stores MQTT broker connection configuration in the internal database.
 * This replaces the JSON file-based MqttBrokerConfig.
 */
public class MqttBrokerConfigRecord extends PersistentRecord {
    
    // Meta object - required for all PersistentRecords
    public static final RecordMeta<MqttBrokerConfigRecord> META = 
        new RecordMeta<>(MqttBrokerConfigRecord.class, "MqttBrokerConfig");
    
    // Primary key - auto-incrementing ID
    public static final IdentityField Id = new IdentityField(META);
    
    // Configuration name (e.g., "Main MQTT Broker")
    public static final StringField Name = new StringField(META, "Name", SFieldFlags.SMANDATORY, SFieldFlags.SDESCRIPTIVE);
    
    // Broker connection settings
    public static final StringField BrokerUrl = new StringField(META, "BrokerUrl", SFieldFlags.SMANDATORY)
        .setDefault("tcp://localhost:1883");
    
    public static final StringField ClientId = new StringField(META, "ClientId", SFieldFlags.SMANDATORY)
        .setDefault("ignition-mqtt-publisher");
    
    public static final StringField Username = new StringField(META, "Username");
    
    public static final StringField Password = new StringField(META, "Password");
    
    public static final IntField Qos = new IntField(META, "Qos")
        .setDefault(1);
    
    public static final BooleanField Retained = new BooleanField(META, "Retained")
        .setDefault(false);
    
    public static final BooleanField CleanSession = new BooleanField(META, "CleanSession")
        .setDefault(true);
    
    public static final IntField ConnectionTimeout = new IntField(META, "ConnectionTimeout")
        .setDefault(30);
    
    public static final IntField KeepAliveInterval = new IntField(META, "KeepAliveInterval")
        .setDefault(60);
    
    public static final BooleanField Enabled = new BooleanField(META, "Enabled")
        .setDefault(true);
    
    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
    
    // Convenience getters/setters
    public String getName() {
        return getString(Name);
    }
    
    public void setName(String name) {
        setString(Name, name);
    }
    
    public String getBrokerUrl() {
        return getString(BrokerUrl);
    }
    
    public void setBrokerUrl(String url) {
        setString(BrokerUrl, url);
    }
    
    // ... more getters/setters
}
```

**File:** `mqtt-gateway/src/main/java/com/inductiveautomation/ignition/examples/mqtt/gateway/records/MqttTagConfigRecord.java`

```java
package com.inductiveautomation.ignition.examples.mqtt.gateway.records;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import simpleorm.dataset.SFieldFlags;

/**
 * Stores tag publishing configuration.
 * Links to MqttBrokerConfigRecord via foreign key.
 */
public class MqttTagConfigRecord extends PersistentRecord {
    
    public static final RecordMeta<MqttTagConfigRecord> META = 
        new RecordMeta<>(MqttTagConfigRecord.class, "MqttTagConfig");
    
    public static final IdentityField Id = new IdentityField(META);
    
    // Foreign key to broker config
    public static final LongField BrokerConfigId = new LongField(META, "BrokerConfigId", SFieldFlags.SMANDATORY);
    public static final ReferenceField<MqttBrokerConfigRecord> BrokerConfig = 
        new ReferenceField<>(META, MqttBrokerConfigRecord.META, "BrokerConfig", BrokerConfigId);
    
    // Tag selection settings
    public static final StringField TagProviders = new StringField(META, "TagProviders", 4000); // JSON array
    public static final StringField TagFolders = new StringField(META, "TagFolders", 4000); // JSON array
    
    // Publishing settings
    public static final DoubleField ValueDeadband = new DoubleField(META, "ValueDeadband")
        .setDefault(0.1);
    
    public static final BooleanField PublishOnQualityChange = new BooleanField(META, "PublishOnQualityChange")
        .setDefault(true);
    
    public static final BooleanField IncludeMetadata = new BooleanField(META, "IncludeMetadata")
        .setDefault(true);
    
    // Topic overrides stored as JSON
    public static final StringField TopicOverrides = new StringField(META, "TopicOverrides", 8000);
    
    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
    
    // Getters/setters...
}
```

### Step 2: Register Records in GatewayHook

In your `MqttGatewayHook.setup()` method, register the records:

```java
@Override
public void setup(GatewayContext context) {
    this.gatewayContext = context;
    
    // Register PersistentRecords - this creates the tables in the internal database
    context.getSchemaUpdater().updatePersistentRecords(
        MqttBrokerConfigRecord.META,
        MqttTagConfigRecord.META
    );
    
    // Create default record if none exists
    ensureDefaultConfig(context);
    
    // ... rest of setup
}

private void ensureDefaultConfig(GatewayContext context) {
    try {
        PersistenceInterface db = context.getPersistenceInterface();
        
        // Check if any broker config exists
        SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
        List<MqttBrokerConfigRecord> existing = db.query(query);
        
        if (existing.isEmpty()) {
            // Create default config
            MqttBrokerConfigRecord defaultConfig = db.createNew(MqttBrokerConfigRecord.META);
            defaultConfig.setName("Default MQTT Broker");
            defaultConfig.setBrokerUrl("tcp://localhost:1883");
            defaultConfig.setClientId("ignition-mqtt-publisher");
            defaultConfig.setEnabled(false); // User must enable it
            
            db.save(defaultConfig);
            
            logger.info("Created default MQTT broker configuration");
        }
    } catch (Exception e) {
        logger.error("Error creating default configuration", e);
    }
}
```

### Step 3: Read/Write Records

Use `PersistenceInterface` to interact with records:

```java
public MqttBrokerConfigRecord loadConfig() {
    try {
        PersistenceInterface db = gatewayContext.getPersistenceInterface();
        
        // Query for enabled broker configs
        SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META)
            .eq(MqttBrokerConfigRecord.Enabled, true);
        
        List<MqttBrokerConfigRecord> configs = db.query(query);
        
        return configs.isEmpty() ? null : configs.get(0);
        
    } catch (Exception e) {
        logger.error("Error loading configuration", e);
        return null;
    }
}

public void saveConfig(MqttBrokerConfigRecord config) {
    try {
        PersistenceInterface db = gatewayContext.getPersistenceInterface();
        db.save(config);
        
        logger.info("Saved MQTT configuration: {}", config.getName());
        
    } catch (Exception e) {
        logger.error("Error saving configuration", e);
        throw new RuntimeException("Failed to save configuration", e);
    }
}
```

### Step 4: Listen for Config Changes

Use `RecordListener` to react when config changes:

```java
// In setup() method:
MqttBrokerConfigRecord.META.addRecordListener(new RecordListenerAdapter<MqttBrokerConfigRecord>() {
    @Override
    public void recordUpdated(MqttBrokerConfigRecord record) {
        logger.info("MQTT configuration changed: {}", record.getName());
        
        // Reconnect to broker with new settings
        publisherManager.disconnect();
        publisherManager.connect(convertToConfig(record));
    }
    
    @Override
    public void recordAdded(MqttBrokerConfigRecord record) {
        logger.info("New MQTT configuration added: {}", record.getName());
    }
    
    @Override
    public void recordDeleted(MqttBrokerConfigRecord record) {
        logger.info("MQTT configuration deleted: {}", record.getName());
        publisherManager.disconnect();
    }
});
```

---

## Part 2: Gateway Web UI - React Configuration Page

### Overview

Ignition 8.3 uses a modern React-based Gateway web interface. Modules can add pages to this interface using:

1. **SystemJsModule** - Defines a JavaScript module containing your React component
2. **NavigationModel** - Adds menu items to the Gateway sidebar
3. **Route mounting** - Serves your React bundle and API endpoints

### Step 1: Create React Application

**Project Structure:**
```
mqtt-gateway/
├── web-ui/                        # React application
│   ├── src/
│   │   ├── pages/
│   │   │   └── Configuration/
│   │   │       ├── Configuration.tsx    # Main config page
│   │   │       ├── BrokerSettings.tsx   # Broker form
│   │   │       ├── TagSelection.tsx     # Tag browser
│   │   │       ├── TopicMapping.tsx     # Topic overrides
│   │   │       └── StatusDashboard.tsx  # Health/stats
│   │   ├── api/
│   │   │   └── mqttConfig.ts           # API client
│   │   └── index.ts                    # Entry point
│   ├── package.json
│   ├── tsconfig.json
│   └── webpack.config.js
└── src/main/
    ├── java/                          # Java backend
    └── resources/
        └── mounted/                   # Static files served at /res/module-id/
```

**File:** `mqtt-gateway/web-ui/package.json`

```json
{
  "name": "mqtt-uns-publisher-ui",
  "version": "1.0.0",
  "scripts": {
    "build": "webpack --mode production",
    "dev": "webpack --mode development --watch"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "typescript": "^5.0.0",
    "webpack": "^5.80.0",
    "webpack-cli": "^5.0.0",
    "ts-loader": "^9.4.0"
  }
}
```

**File:** `mqtt-gateway/web-ui/src/pages/Configuration/Configuration.tsx`

```typescript
import React, { useState, useEffect } from 'react';
import { getMqttConfig, saveMqttConfig, testConnection } from '../../api/mqttConfig';
import BrokerSettings from './BrokerSettings';
import TagSelection from './TagSelection';
import StatusDashboard from './StatusDashboard';

export const Configuration: React.FC = () => {
    const [config, setConfig] = useState<MqttConfig | null>(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('broker');
    
    useEffect(() => {
        loadConfig();
    }, []);
    
    const loadConfig = async () => {
        try {
            const data = await getMqttConfig();
            setConfig(data);
        } catch (error) {
            console.error('Failed to load configuration', error);
        } finally {
            setLoading(false);
        }
    };
    
    const handleSave = async (newConfig: MqttConfig) => {
        try {
            await saveMqttConfig(newConfig);
            setConfig(newConfig);
            alert('Configuration saved successfully');
        } catch (error) {
            alert('Failed to save configuration: ' + error.message);
        }
    };
    
    if (loading) return <div>Loading...</div>;
    
    return (
        <div className="mqtt-config-page">
            <h1>MQTT UNS Publisher Configuration</h1>
            
            <div className="tabs">
                <button onClick={() => setActiveTab('broker')} 
                        className={activeTab === 'broker' ? 'active' : ''}>
                    Broker Settings
                </button>
                <button onClick={() => setActiveTab('tags')}
                        className={activeTab === 'tags' ? 'active' : ''}>
                    Tag Selection
                </button>
                <button onClick={() => setActiveTab('status')}
                        className={activeTab === 'status' ? 'active' : ''}>
                    Status
                </button>
            </div>
            
            <div className="tab-content">
                {activeTab === 'broker' && (
                    <BrokerSettings 
                        config={config} 
                        onSave={handleSave}
                        onTest={testConnection}
                    />
                )}
                {activeTab === 'tags' && (
                    <TagSelection 
                        config={config} 
                        onSave={handleSave}
                    />
                )}
                {activeTab === 'status' && (
                    <StatusDashboard />
                )}
            </div>
        </div>
    );
};

export default Configuration;
```

**File:** `mqtt-gateway/web-ui/src/api/mqttConfig.ts`

```typescript
const API_BASE = '/data/mqtt-uns-publisher';

export interface MqttConfig {
    id?: number;
    name: string;
    brokerUrl: string;
    clientId: string;
    username?: string;
    password?: string;
    qos: number;
    retained: boolean;
    enabled: boolean;
}

export async function getMqttConfig(): Promise<MqttConfig> {
    const response = await fetch(`${API_BASE}/config`);
    if (!response.ok) {
        throw new Error('Failed to load configuration');
    }
    return response.json();
}

export async function saveMqttConfig(config: MqttConfig): Promise<void> {
    const response = await fetch(`${API_BASE}/config`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
    });
    
    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Failed to save configuration');
    }
}

export async function testConnection(config: MqttConfig): Promise<boolean> {
    const response = await fetch(`${API_BASE}/test-connection`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
    });
    
    const result = await response.json();
    return result.success;
}
```

**File:** `mqtt-gateway/web-ui/webpack.config.js`

```javascript
const path = require('path');

module.exports = {
    entry: './src/index.ts',
    output: {
        filename: 'mqtt-config.js',
        path: path.resolve(__dirname, '../src/main/resources/mounted'),
        library: {
            name: 'MqttUnsPublisher',
            type: 'umd'
        }
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/
            }
        ]
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.js']
    },
    externals: {
        'react': 'React',
        'react-dom': 'ReactDOM'
    }
};
```

### Step 2: Register React Page in GatewayHook

```java
@Override
public void setup(GatewayContext context) {
    this.gatewayContext = context;
    
    // Register PersistentRecords (see Part 1)
    context.getSchemaUpdater().updatePersistentRecords(...);
    
    // Create SystemJs module pointing to our bundled React app
    SystemJsModule jsModule = new SystemJsModule(
        "mqtt-uns-publisher.Configuration",        // Module name
        "/res/mqtt-uns-publisher/mqtt-config.js"   // Bundle path
    );
    
    // Add page to Gateway navigation (Config section)
    context.getWebResourceManager()
        .getNavigationModel()
        .getConfig()  // Add to "Config" section
        .addCategory("mqtt-uns-publisher", cat -> cat
            .label("MQTT UNS Publisher")
            .addPage("Configuration", page -> page
                .position(10)
                .mount("/mqtt-uns-publisher", "Configuration", jsModule)
            )
        );
    
    logger.info("Registered MQTT UNS Publisher web UI");
}

@Override
public Optional<String> getMountedResourceFolder() {
    // Tells Ignition to serve files from /mounted/ folder at /res/module-id/
    return Optional.of("mounted");
}

@Override
public Optional<String> getMountPathAlias() {
    // Shorter URL: /res/mqtt-uns-publisher/ instead of /res/com.example.mqtt.uns/
    return Optional.of("mqtt-uns-publisher");
}
```

### Step 3: Create REST API Routes

```java
@Override
public void mountRouteHandlers(RouteGroup routes) {
    // GET /data/mqtt-uns-publisher/config
    routes.newRoute("/config")
        .handler(new MqttConfigRoute(gatewayContext))
        .get();
    
    // GET /data/mqtt-uns-publisher/status
    routes.newRoute("/status")
        .handler(new MqttStatusRoute(this))
        .get();
    
    // POST /data/mqtt-uns-publisher/test-connection
    routes.newRoute("/test-connection")
        .handler(new TestConnectionRoute())
        .post();
    
    logger.info("Mounted MQTT UNS Publisher API routes");
}
```

**File:** `MqttConfigRoute.java`

```java
package com.inductiveautomation.ignition.examples.mqtt.gateway.web;

import com.google.gson.Gson;
import com.inductiveautomation.ignition.examples.mqtt.gateway.records.MqttBrokerConfigRecord;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteHandler;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistenceInterface;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import simpleorm.dataset.SQuery;

public class MqttConfigRoute extends RouteHandler {
    
    private final GatewayContext context;
    private final Gson gson = new Gson();
    
    public MqttConfigRoute(GatewayContext context) {
        this.context = context;
    }
    
    @Override
    public Object handleRequest(RequestContext requestContext) throws Exception {
        if (requestContext.getMethod().equals("GET")) {
            return handleGet();
        } else if (requestContext.getMethod().equals("POST")) {
            return handlePost(requestContext);
        }
        
        throw new UnsupportedOperationException("Method not supported: " + requestContext.getMethod());
    }
    
    private Object handleGet() {
        PersistenceInterface db = context.getPersistenceInterface();
        
        // Get all broker configs
        SQuery<MqttBrokerConfigRecord> query = new SQuery<>(MqttBrokerConfigRecord.META);
        List<MqttBrokerConfigRecord> configs = db.query(query);
        
        // Return first config (or could return all)
        if (configs.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return convertToJson(configs.get(0));
    }
    
    private Object handlePost(RequestContext requestContext) throws Exception {
        // Parse JSON body
        String body = requestContext.getRequestBody();
        MqttConfigDto dto = gson.fromJson(body, MqttConfigDto.class);
        
        PersistenceInterface db = context.getPersistenceInterface();
        
        MqttBrokerConfigRecord record;
        if (dto.id != null) {
            // Update existing
            record = db.find(MqttBrokerConfigRecord.META, dto.id);
            if (record == null) {
                throw new IllegalArgumentException("Config not found: " + dto.id);
            }
        } else {
            // Create new
            record = db.createNew(MqttBrokerConfigRecord.META);
        }
        
        // Update fields
        record.setName(dto.name);
        record.setBrokerUrl(dto.brokerUrl);
        record.setClientId(dto.clientId);
        record.setUsername(dto.username);
        record.setPassword(dto.password);
        record.setInt(MqttBrokerConfigRecord.Qos, dto.qos);
        record.setBoolean(MqttBrokerConfigRecord.Retained, dto.retained);
        record.setBoolean(MqttBrokerConfigRecord.Enabled, dto.enabled);
        
        // Save to database
        db.save(record);
        
        return Map.of("success", true, "id", record.getId());
    }
    
    private Map<String, Object> convertToJson(MqttBrokerConfigRecord record) {
        Map<String, Object> json = new HashMap<>();
        json.put("id", record.getId());
        json.put("name", record.getName());
        json.put("brokerUrl", record.getBrokerUrl());
        json.put("clientId", record.getString(MqttBrokerConfigRecord.ClientId));
        json.put("username", record.getString(MqttBrokerConfigRecord.Username));
        // Don't send password to client for security
        json.put("qos", record.getInt(MqttBrokerConfigRecord.Qos));
        json.put("retained", record.getBoolean(MqttBrokerConfigRecord.Retained));
        json.put("enabled", record.getBoolean(MqttBrokerConfigRecord.Enabled));
        return json;
    }
    
    // DTO for JSON deserialization
    private static class MqttConfigDto {
        Long id;
        String name;
        String brokerUrl;
        String clientId;
        String username;
        String password;
        int qos;
        boolean retained;
        boolean enabled;
    }
}
```

---

## Benefits of This Approach

### 1. **Professional User Experience**
- ✅ Native Gateway UI integration
- ✅ Real-time validation and feedback
- ✅ No manual file editing
- ✅ Consistent with other Ignition modules

### 2. **Automatic Redundancy**
- ✅ Config automatically synced to backup Gateways
- ✅ No manual file copying needed
- ✅ Guaranteed consistency across cluster

### 3. **Security**
- ✅ Password fields can use Ignition's secret system
- ✅ Role-based access control
- ✅ Audit trail for config changes

### 4. **Reliability**
- ✅ ACID transactions prevent partial updates
- ✅ Schema migrations handled automatically
- ✅ Data validation at database level

### 5. **Maintainability**
- ✅ Type-safe Java code (vs. fragile JSON parsing)
- ✅ Database queries instead of file I/O
- ✅ React components are reusable and testable

---

## Implementation Effort Estimate

### Backend (Java)
- **PersistentRecord classes:** 2-3 hours
- **REST API routes:** 3-4 hours
- **Record listeners & integration:** 2-3 hours
- **Testing:** 2-3 hours
- **Total:** ~10-13 hours

### Frontend (React)
- **Project setup (webpack, TypeScript):** 2-3 hours
- **Configuration forms:** 4-5 hours
- **Tag browser component:** 3-4 hours
- **Status dashboard:** 2-3 hours
- **API integration:** 2-3 hours
- **Styling:** 2-3 hours
- **Testing:** 2-3 hours
- **Total:** ~17-24 hours

### **Grand Total:** ~27-37 hours (3-5 days for one developer)

---

## Migration Plan from JSON Files

### Phase 1: Add PersistentRecords (Parallel to JSON)
1. Create PersistentRecord classes
2. Register in GatewayHook.setup()
3. Add migration code to read existing JSON and create records
4. Keep JSON loading as fallback

### Phase 2: Add Web UI
1. Create React application
2. Add REST API routes
3. Register page in navigation
4. Test end-to-end

### Phase 3: Deprecate JSON
1. Add warning log when JSON file is used
2. Remove JSON file support in next major version
3. Document migration path for users

### Sample Migration Code

```java
@Override
public void startup(LicenseState activationState) {
    // Try to load from database first
    MqttBrokerConfigRecord dbConfig = loadConfigFromDatabase();
    
    if (dbConfig == null) {
        // No database config, check for JSON file
        MqttBrokerConfig jsonConfig = configManager.loadConfig();
        
        if (jsonConfig != null) {
            logger.warn("Found JSON configuration file. Migrating to database...");
            
            // Migrate JSON to database
            dbConfig = migrateJsonToDatabase(jsonConfig);
            
            // Rename JSON file so it won't be used again
            renameJsonFile();
            
            logger.info("Migration complete. Configuration now stored in internal database.");
        }
    }
    
    if (dbConfig != null) {
        publisherManager.connect(convertToConfig(dbConfig));
    }
}

private MqttBrokerConfigRecord migrateJsonToDatabase(MqttBrokerConfig json) {
    PersistenceInterface db = gatewayContext.getPersistenceInterface();
    
    MqttBrokerConfigRecord record = db.createNew(MqttBrokerConfigRecord.META);
    record.setName("Migrated from JSON");
    record.setBrokerUrl(json.getBrokerUrl());
    record.setClientId(json.getClientId());
    record.setUsername(json.getUsername());
    record.setPassword(json.getPassword());
    // ... copy all fields
    
    db.save(record);
    return record;
}
```

---

## Example Modules for Reference

All these examples are in the official SDK repository:

1. **slack-alarm-notification** - Shows PersistentRecord for notification profiles
2. **webui-webpage** - Shows React page integration
3. **opc-ua-device** - Shows device settings with PersistentRecords
4. **user-source-profile** - Shows profile management with database

---

## Conclusion

**Recommendation:** Implement PersistentRecords + React Web UI

This is the **official Ignition way** and provides:
- ✅ Professional user experience
- ✅ Automatic redundancy
- ✅ Security and audit trail
- ✅ Type safety and reliability
- ✅ Future-proof architecture

**Effort:** 3-5 days of development time

**Benefit:** Production-quality configuration system that matches Ignition's native modules

The JSON file approach should only be used for prototypes or modules that will never be used in production environments.
