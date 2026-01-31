import React from 'react';
import ReactDOM from 'react-dom';
import ConfigurationComponent from './components/Configuration';
import ErrorBoundary from './components/ErrorBoundary';

console.log('=== SPARKPLUG MODULE LOADING ===');
console.log('[Sparkplug Module] index.tsx loaded');
console.log('[Sparkplug Module] React available:', typeof React !== 'undefined');
console.log('[Sparkplug Module] ReactDOM available:', typeof ReactDOM !== 'undefined');
console.log('[Sparkplug Module] ConfigurationComponent type:', typeof ConfigurationComponent);

const ConfigurationWrapper: any = function(this: any, ...args: any[]) {
    console.log('[Sparkplug Module] Configuration wrapper called');
    console.log('[Sparkplug Module] this:', this);
    console.log('[Sparkplug Module] args:', args);
    console.log('[Sparkplug Module] args.length:', args.length);

    if (new.target) {
        console.log('[Sparkplug Module] Called with new keyword (constructor)');
        return React.createElement(ErrorBoundary, { children: React.createElement(ConfigurationComponent) });
    }

    if (args.length === 0) {
        console.log('[Sparkplug Module] Called with no args - returning React element');
        return React.createElement(ErrorBoundary, { children: React.createElement(ConfigurationComponent) });
    }

    if (args.length >= 1) {
        console.log('[Sparkplug Module] Called with args - creating React element with props');
        const props = args[0] && typeof args[0] === 'object' ? args[0] : {};
        return React.createElement(ErrorBoundary, { children: React.createElement(ConfigurationComponent, props) });
    }

    if (args[0] && args[0].nodeType) {
        console.log('[Sparkplug Module] Called with DOM element - mounting component');
        ReactDOM.render(
            React.createElement(ErrorBoundary, { children: React.createElement(ConfigurationComponent) }),
            args[0]
        );
        return;
    }

    console.log('[Sparkplug Module] Fallback - returning React element');
    return React.createElement(ErrorBoundary, { children: React.createElement(ConfigurationComponent) });
};

ConfigurationWrapper.displayName = 'Configuration';

const moduleExports = {
    Configuration: ConfigurationWrapper
};

console.log('[Sparkplug Module] Module exports:', moduleExports);
console.log('=== SPARKPLUG MODULE EXPORT ===');

if (typeof window !== 'undefined') {
    setTimeout(() => {
        const System = (window as any).System;
        if (System) {
            const registered = System.get('com.inductiveautomation.mqtt.sparkplugb.gateway');
            console.log('[Sparkplug Module] SystemJS registration check:', registered);
        }
    }, 500);
}

export default moduleExports;
