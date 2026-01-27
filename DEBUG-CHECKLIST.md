# Debug Checklist for Web UI Loading Issue

## Current Status
- ✅ Module builds successfully (306 KB)
- ✅ JavaScript file is accessible at: http://localhost:8088/res/mqtt-uns-publisher/mqtt-config.js
- ✅ Module structure verified locally: exports `{ Configuration: function }`
- ✅ REST API routes mounted with access control
- ❌ Page shows: "ERROR: Failed to load Module: mqtt-config Component: Configuration"

## Things to Check in Browser

### 1. Console Tab (F12 > Console)
Look for these debug messages (should appear when page loads):
```
[MQTT Module] index.tsx loaded
[MQTT Module] React available: true
[MQTT Module] ConfigurationComponent type: function
[MQTT Module] Exported Configuration: function
```

**If you DON'T see these messages:** The module isn't being loaded by SystemJS at all.

**If you DO see these messages:** The module loads but Ignition can't find the component.

### 2. Console Errors
Look for any RED error messages (not orange warnings) that mention:
- "mqtt" or "Configuration"
- "SystemJS"
- "Module not found"
- "React is not defined"
- Any stack traces

Ignore these (they're normal):
- Source map 404 errors (orange warnings)
- Messages about system.min.js.map, named-register.min.js.map, amd.min.js.map

### 3. Network Tab (F12 > Network)
1. Clear the network log
2. Reload the page
3. Filter by "mqtt" or search for "mqtt-config"
4. Look for: `mqtt-config.js`

**Questions:**
- Is the request there? (Yes/No)
- What's the status code? (200, 404, 500, etc.)
- What's the size? (Should be ~39-40 KB)
- Click on it and go to "Response" tab - what do you see?

### 4. Page Error Message
The exact text showing on the page (not console). Please copy the COMPLETE message:
```
ERROR: Failed to load

Module: mqtt-config

Component: Configuration
```

Is there MORE text after "Component: Configuration"? Any additional error details?

## Files to Verify

### In Browser - Access Directly
Open these URLs and verify what you see:

1. **http://localhost:8088/res/mqtt-uns-publisher/mqtt-config.js**
   - Should show: JavaScript code (minified)
   - Should NOT show: 404 error

2. **http://localhost:8088/data/mqtt-uns-publisher/status**
   - Should show: JSON data (might be empty: `{}`)
   - Should NOT show: 404 or 500 error

### In Gateway Logs
Check `logs/wrapper.log` for:

**GOOD signs:**
```
INFO  [MqttGatewayHook] Mounting REST API routes
INFO  [MqttGatewayHook] Mounted 5 REST API routes with open access
INFO  [MqttGatewayHook] Registered MQTT UNS Publisher web UI in Gateway navigation
```

**BAD signs:**
```
ERROR Unable to mount routes
ERROR Access control must be specified
ERROR Error registering web UI
```

## Module Configuration Summary

Current configuration that SHOULD work:

### Java (MqttGatewayHook.java)
```java
SystemJsModule jsModule = new SystemJsModule(
    "mqtt-config",  // Module name (simple, no dots)
    "/res/mqtt-uns-publisher/mqtt-config.js"
);
.mount("/mqtt-uns-publisher", "Configuration", jsModule);
```

### Webpack (webpack.config.js)
```javascript
library: {
    name: 'mqtt-config',  // Matches Java module name
    type: 'amd'
    // NO export: 'default'
}
externals: {
    'react': 'React',
    'react-dom': 'ReactDOM'
}
```

### TypeScript (index.tsx)
```typescript
export const Configuration = ConfigurationComponent;  // Named export
```

### Verified Locally
```
Module name: mqtt-config
Module exports: { Configuration: function }
✅ Structure is correct
```

## Next Steps Based on Findings

### If debug messages DON'T appear:
→ Module isn't loading. Check:
- Network tab for 404 on mqtt-config.js
- Console for SystemJS errors about module resolution
- Gateway logs for resource mounting errors

### If debug messages DO appear:
→ Module loads but component not found. Check:
- What the debug messages say about React availability
- Console for React-related errors
- Try class component instead of functional component

### If mqtt-config.js returns 404:
→ Resources not mounted. Check:
- `getMountedResourceFolder()` returns "mounted"
- File exists at: `mqtt-gateway/src/main/resources/mounted/mqtt-config.js`
- Module was rebuilt after adding the file

### If mqtt-config.js loads but React errors:
→ External dependency issue. Try:
- Different external format in webpack
- Bundling React instead of external
- Using global window.React

## Information Needed

Please provide:
1. [ ] Do `[MQTT Module]` debug messages appear? (Yes/No)
2. [ ] If yes, what do they say?
3. [ ] Screenshot of Network tab filtered for "mqtt"
4. [ ] Any RED errors in Console (copy/paste full error)
5. [ ] Complete error text from the page itself
6. [ ] Response when accessing mqtt-config.js URL directly

This will help pinpoint exactly where the failure is occurring.
