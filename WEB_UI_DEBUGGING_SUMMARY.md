# Ignition MQTT UNS Publisher - Web UI Integration Issue

## Project Overview
Building a **Gateway Web UI** for an Ignition 8.3.0 MQTT UNS Publisher module using React/TypeScript. The module needs a configuration interface accessible from the Ignition Gateway web interface.

## Current Status: Component Loads But Crashes During Render

### ✅ What's Working
1. **Module builds successfully** - 442 KB .modl file
2. **Module installs and runs** - Shows "Running" status in Gateway
3. **Navigation works** - "MQTT UNS Publisher" appears under Connections
4. **JavaScript loads** - HTTP 200 for `/res/mqtt-uns-publisher/mqtt-config.js`
5. **SystemJS module registered** - Module appears in `System.entries()`
6. **React bundled correctly** - React/ReactDOM included (no external dependencies)
7. **Component starts rendering** - `[Configuration] Component rendering started` logs appear
8. **Wrapper function works** - Correctly receives props from Ignition

### ❌ The Problem
- Page shows **"Application Error"** with unauthorized.png image
- Component renders but then **crashes before completing**
- **No React error message visible** in console logs
- Likely caught by Ignition's error boundary, but actual error not shown

### Console Logs (Latest)
```
=== MQTT MODULE LOADING ===
[MQTT Module] React available: true
[MQTT Module] ReactDOM available: true
[MQTT Module] Configuration wrapper called
[MQTT Module] args: Array [ {baseApi: ...}, {} ]
[MQTT Module] Called with args - creating React element with props
[Configuration] Component rendering started 2
[Configuration] Component rendering started 2  // Called twice, then crashes
GET http://localhost:8088/res/sys/img/authentication/unauthorized.png
```

**Notice:** We see component render start but never see:
- `[Configuration] useEffect triggered`
- `[Configuration] Fetching broker config...`
- `[Configuration] Rendering main UI`

This means the component crashes **during initial render**, before completing.

## Technical Architecture

### Frontend Stack
- **React 18** (bundled, not external)
- **TypeScript**
- **Webpack 5** (UMD format)
- **Build output:** Self-contained bundle at `mqtt-gateway/src/main/resources/mounted/mqtt-config.js`

### Backend Stack
- **Ignition SDK 8.3.0**
- **Java 17**
- **REST API:** Routes registered at `/data/mqtt-uns-publisher/*`
- **Database:** PersistentRecord classes for config storage

### File Structure
```
mqtt-gateway/
├── web-ui/
│   ├── src/
│   │   ├── index.tsx              # Entry point with wrapper
│   │   ├── components/
│   │   │   ├── Configuration.tsx  # Main component
│   │   │   ├── BrokerSettings.tsx
│   │   │   ├── TagSelection.tsx
│   │   │   └── StatusDashboard.tsx
│   │   ├── api.ts                 # REST API client
│   │   ├── types.ts
│   │   └── styles.css
│   ├── webpack.config.js
│   └── package.json
└── src/main/
    ├── java/.../MqttGatewayHook.java  # Registers web UI
    ├── java/.../web/MqttConfigRoute.java  # REST endpoints
    └── resources/mounted/mqtt-config.js  # Generated bundle
```

## Key Implementation Details

### 1. Webpack Configuration (`mqtt-gateway/web-ui/webpack.config.js`)
```javascript
output: {
    filename: 'mqtt-config.js',
    path: path.resolve(__dirname, '../src/main/resources/mounted'),
    library: {
        name: 'com.inductiveautomation.mqtt.uns.gateway',
        type: 'umd',
        umdNamedDefine: true,
        export: 'default'
    },
    globalObject: 'this'
},
// NO externals - React/ReactDOM bundled
```

**Generates AMD define:**
```javascript
define("com.inductiveautomation.mqtt.uns.gateway", [], factory);
```

### 2. Component Wrapper (`mqtt-gateway/web-ui/src/index.tsx`)
```typescript
const ConfigurationWrapper: any = function(this: any, ...args: any[]) {
    console.log('[MQTT Module] Configuration wrapper called');
    console.log('[MQTT Module] args:', args);
    
    if (args.length >= 1) {
        const props = args[0] && typeof args[0] === 'object' ? args[0] : {};
        return React.createElement(ConfigurationComponent, props);
    }
    
    return React.createElement(ConfigurationComponent);
};

export default { Configuration: ConfigurationWrapper };
```

**Why:** Ignition calls the exported function with `(props, context)` where props contains `{baseApi: ...}` (Redux Toolkit Query API).

### 3. Java Registration (`MqttGatewayHook.java:370-390`)
```java
SystemJsModule jsModule = new SystemJsModule(
    "com.inductiveautomation.mqtt.uns.gateway",
    "/res/mqtt-uns-publisher/mqtt-config.js"
);

gatewayContext.getWebResourceManager()
    .getNavigationModel()
    .getConnections()
    .addCategory("mqtt-uns-publisher", cat -> cat
        .label("MQTT UNS Publisher")
        .position(100)
        .addPage("settings", page -> page
            .position(10)
            .mount("/mqtt-uns-publisher", "Configuration", jsModule)
        )
    );
```

### 4. Configuration Component (`Configuration.tsx`)
```typescript
const Configuration: React.FC = () => {
    console.log('[Configuration] Component rendering started');
    
    const [activeTab, setActiveTab] = useState<TabType>('broker');
    const [brokerConfig, setBrokerConfig] = useState<MqttBrokerConfig | null>(null);
    const [tagConfig, setTagConfig] = useState<MqttTagConfig | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        console.log('[Configuration] useEffect triggered - loading configuration');
        loadConfiguration();
    }, []);

    const loadConfiguration = async () => {
        const brokerResponse = await getBrokerConfig();
        // ... API calls
    };
    
    // Render tabs and content
    return (
        <div className="mqtt-config-page">
            <header className="page-header">
                <h1>MQTT UNS Publisher Configuration</h1>
            </header>
            <div className="tabs">
                <button>Broker Settings</button>
                <button>Tag Publishing</button>
                <button>Status & Statistics</button>
            </div>
            {/* Tab content */}
        </div>
    );
};
```

## What We've Tried

### Evolution of Fixes
1. ✅ **File not accessible (404)** → Fixed: Files in `src/main/resources/mounted/` auto-served
2. ✅ **SystemJS module ID wrong** → Fixed: Changed to match `.mount()` expectation
3. ✅ **AMD module name mismatch** → Fixed: Webpack library name matches SystemJS ID
4. ✅ **Wrong module format** → Fixed: Changed from AMD to UMD
5. ✅ **React/ReactDOM not found** → Fixed: Bundled instead of treating as externals
6. ✅ **Component not callable** → Fixed: Added wrapper function
7. ❌ **Component crashes during render** → CURRENT ISSUE

### SystemJS Modules Available in Ignition
```javascript
Array.from(System.entries()).map(([name]) => name)
// Returns:
[
  "http://localhost:8088/res/sys/js/react.js",
  "http://localhost:8088/res/sys/js/react-dom.js",
  "http://localhost:8088/res/sys/js/react-redux.js",
  "http://localhost:8088/res/sys/js/redux-toolkit.js",
  "http://localhost:8088/res/mqtt-uns-publisher/mqtt-config.js",
  // ... more
]
```

**Note:** React IS available in Ignition's SystemJS, but we bundle our own copy to avoid version conflicts.

## The Mystery: Why Does It Crash?

### Evidence
1. Component **starts** rendering (logged)
2. React hooks **don't run** (no useEffect log)
3. **Unauthorized.png** appears (Ignition error boundary)
4. **No error message** in console
5. Component is called **twice** with same args, then fails

### Theories
1. **React version conflict:** We bundle React 18, Ignition uses React 18 too - possible duplicate React instances?
2. **Missing context:** Component needs Redux Provider but doesn't have it?
3. **Hook rule violation:** Something breaks React's hook rules during render?
4. **Import/export issue:** Some dependency not bundled correctly?
5. **CSS injection problem:** style-loader might fail in Ignition's environment?

### What We Need
**The actual React error message!** It's being caught by Ignition's error boundary but not displayed in console.

## Questions for Browser-Enabled AI

1. **Check React DevTools:** Can you see the component tree? Where does it stop?

2. **Check for hidden errors:** 
   - Look in Console filter for "Errors" only
   - Check browser's Network tab for failed requests
   - Look for unhandled promise rejections
   - Check Application → Storage for any issues

3. **Inspect the error boundary:**
   - Can you find Ignition's error boundary in the React tree?
   - Can you access its error state?
   - Try: `document.querySelector('.error')` or similar

4. **Test the component directly:**
   ```javascript
   // In console, try rendering manually
   const { Configuration } = window.MQTT_MODULE_DEBUG.exports;
   const element = Configuration();
   console.log('Element:', element);
   ```

5. **Check React instance:**
   ```javascript
   // Are there multiple React instances?
   window.React = require('react'); // Does this work?
   ```

6. **Inspect the DOM:**
   - What HTML is actually on the page?
   - Is there a `<div>` where the component should mount?
   - Any error text in hidden elements?

## REST API Endpoints

These are registered and working:
- `GET /data/mqtt-uns-publisher/config/broker` - Get broker config
- `POST /data/mqtt-uns-publisher/config/broker` - Save broker config  
- `GET /data/mqtt-uns-publisher/config/tags` - Get tag config
- `POST /data/mqtt-uns-publisher/config/tags` - Save tag config
- `GET /data/mqtt-uns-publisher/status` - Get module status

**Note:** These return `{"success": true, "data": null}` when no records exist (defensive JSON parsing added).

## Build Commands

```bash
# Frontend only
cd mqtt-gateway/web-ui && npm run build

# Full module
./gradlew build

# Clean build
./gradlew clean build

# Output
build/MQTT-UNS-Publisher.unsigned.modl (442 KB)
```

## How to Test

1. Install module: Config → System → Modules → Install `.modl` file
2. Navigate to: Config → Connections → MQTT UNS Publisher → settings
3. Open browser DevTools console
4. Observe logs and behavior

## Critical Files to Review

1. **`mqtt-gateway/web-ui/src/index.tsx`** - Wrapper function
2. **`mqtt-gateway/web-ui/src/components/Configuration.tsx`** - Main component
3. **`mqtt-gateway/src/main/resources/mounted/mqtt-config.js`** - Generated bundle
4. **Browser console logs** - All debugging output
5. **Browser React DevTools** - Component tree inspection

## Success Criteria

When working, we should see:
```
[Configuration] Component rendering started
[Configuration] useEffect triggered - loading configuration
[Configuration] Fetching broker config...
[Configuration] Broker response: {success: true, data: null}
[Configuration] Fetching tag config...
[Configuration] Tag response: {success: true, data: null}
[Configuration] loadConfiguration completed
[Configuration] Rendering main UI, activeTab: broker
```

And the page should display:
- Header: "MQTT UNS Publisher Configuration"
- Three tabs: "Broker Settings", "Tag Publishing", "Status & Statistics"
- Form fields for MQTT configuration

## Next Steps

A browser-enabled AI should be able to:
1. **See the actual React error** (it's there, just hidden)
2. **Inspect the component tree** with React DevTools
3. **Test rendering directly** in the console
4. **Check for React version conflicts** or missing providers
5. **Determine if CSS injection is failing**

The component code itself is simple and should work - the issue is likely in **how it integrates with Ignition's React environment**.
