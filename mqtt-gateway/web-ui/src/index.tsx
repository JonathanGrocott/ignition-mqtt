import React from 'react';
import ReactDOM from 'react-dom';
import ConfigurationComponent from './components/Configuration';
import ErrorBoundary from './components/ErrorBoundary';

// IMMEDIATE debug logging - runs as soon as file is parsed
console.log('=== MQTT MODULE LOADING ===');
console.log('[MQTT Module] TOP OF FILE - Script is being parsed');
console.log('[MQTT Module] timestamp:', new Date().toISOString());

// Debug logging to help diagnose the issue
console.log('[MQTT Module] index.tsx loaded');
console.log('[MQTT Module] React available:', typeof React !== 'undefined');
console.log('[MQTT Module] ReactDOM available:', typeof ReactDOM !== 'undefined');
console.log('[MQTT Module] ConfigurationComponent type:', typeof ConfigurationComponent);

// For Ignition Gateway SystemJS loading
// Based on logs, Ignition calls with different signatures to probe the component type
// Let's handle all cases and log what we receive
console.log('[MQTT Module] Exporting ConfigurationComponent with wrapper');

// Wrapper that handles different calling conventions
const ConfigurationWrapper: any = function(this: any, ...args: any[]) {
    console.log('[MQTT Module] Configuration wrapper called');
    console.log('[MQTT Module] this:', this);
    console.log('[MQTT Module] args:', args);
    console.log('[MQTT Module] args.length:', args.length);
    
    // Check if called with 'new' keyword
    if (new.target) {
        console.log('[MQTT Module] Called with new keyword (constructor)');
        // Return an instance
        return React.createElement(ConfigurationComponent);
    }
    
    // Check calling patterns
    if (args.length === 0) {
        console.log('[MQTT Module] Called with no args - returning React element');
        return React.createElement(ConfigurationComponent);
    }
    
    if (args.length >= 1) {
        console.log('[MQTT Module] Called with args - creating React element with props');
        const props = args[0] && typeof args[0] === 'object' ? args[0] : {};
        console.log('[MQTT Module] Props for component:', props);
        return React.createElement(ErrorBoundary, { children: React.createElement(ConfigurationComponent, props) });
    }
    
    // Check if first arg looks like a DOM element
    if (args[0] && args[0].nodeType) {
        console.log('[MQTT Module] Called with DOM element - mounting component');
        ReactDOM.render(
            React.createElement(ErrorBoundary, { children: React.createElement(ConfigurationComponent) }), 
            args[0]
        );
        return;
    }
    
    console.log('[MQTT Module] Fallback - returning React element');
    return React.createElement(ErrorBoundary, { children: React.createElement(ConfigurationComponent) });
};

// Make it look like a React component
ConfigurationWrapper.displayName = 'Configuration';

const moduleExports = {
    Configuration: ConfigurationWrapper
};

// Log after export
console.log('[MQTT Module] Module exports:', moduleExports);
console.log('[MQTT Module] Configuration type:', typeof moduleExports.Configuration);
console.log('[MQTT Module] Configuration component:', moduleExports.Configuration);

// Try to log to window for debugging
if (typeof window !== 'undefined') {
    (window as any).MQTT_MODULE_DEBUG = {
        loaded: true,
        timestamp: new Date().toISOString(),
        exports: moduleExports,
        React: typeof React,
        ReactDOM: typeof ReactDOM
    };
    console.log('[MQTT Module] Debug info stored in window.MQTT_MODULE_DEBUG');
}

console.log('=== MQTT MODULE EXPORT ===');
console.log('[MQTT Module] About to export:', moduleExports);

// Try to manually verify SystemJS registration after module loads
if (typeof window !== 'undefined') {
    setTimeout(() => {
        const System = (window as any).System;
        if (System) {
            const registered = System.get('com.inductiveautomation.mqtt.uns.gateway');
            console.log('[MQTT Module] SystemJS registration check:', registered);
            if (!registered) {
                console.error('[MQTT Module] WARNING: Module not registered in SystemJS!');
                
                // Try to list all registered modules
                if (System.entries) {
                    console.log('[MQTT Module] Attempting to list all SystemJS modules:');
                    const entries = Array.from(System.entries());
                    console.log('[MQTT Module] Registered modules:', entries);
                } else if (System.registry) {
                    console.log('[MQTT Module] SystemJS registry:', Object.keys(System.registry));
                } else if (System._loader && System._loader.moduleRecords) {
                    console.log('[MQTT Module] Module records:', Object.keys(System._loader.moduleRecords));
                }
            } else {
                console.log('[MQTT Module] ✓ Module successfully registered!');
            }
        }
    }, 500); // Increased timeout to give SystemJS more time
}

export default moduleExports;
