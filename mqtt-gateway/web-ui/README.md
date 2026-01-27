# MQTT UNS Publisher - Gateway Web UI

This directory contains the React/TypeScript web UI for configuring the MQTT UNS Publisher module from the Ignition Gateway.

## Setup

Install dependencies:

```bash
npm install
```

## Build

Build for production (outputs to `../src/main/resources/mounted/mqtt-config.js`):

```bash
npm run build
```

Build for development with auto-rebuild on changes:

```bash
npm run dev
```

## Structure

```
web-ui/
├── src/
│   ├── components/
│   │   ├── Configuration.tsx       # Main page with tabs
│   │   ├── BrokerSettings.tsx      # MQTT broker configuration form
│   │   ├── TagSelection.tsx        # Tag publishing configuration
│   │   └── StatusDashboard.tsx     # Module health and statistics
│   ├── api.ts                      # API client for REST endpoints
│   ├── types.ts                    # TypeScript type definitions
│   ├── styles.css                  # UI styles
│   └── index.tsx                   # Entry point
├── package.json
├── tsconfig.json
└── webpack.config.js
```

## API Endpoints

The UI communicates with the following REST API endpoints:

- `GET /data/mqtt-uns-publisher/config/broker` - Get broker configuration
- `POST /data/mqtt-uns-publisher/config/broker` - Save broker configuration
- `GET /data/mqtt-uns-publisher/config/tags` - Get tag configuration
- `POST /data/mqtt-uns-publisher/config/tags` - Save tag configuration
- `GET /data/mqtt-uns-publisher/status` - Get module status and statistics
- `POST /data/mqtt-uns-publisher/test-connection` - Test MQTT broker connection

## Features

### Broker Settings Tab
- Configure MQTT broker connection (URL, client ID, credentials)
- TLS/SSL encryption support
- QoS, retained messages, clean session options
- Connection timeout and keep-alive settings
- Test connection before saving
- Enable/disable module

### Tag Publishing Tab
- Select tag providers to monitor
- Specify tag folders to publish
- Configure value deadband (prevent noise)
- Include/exclude metadata
- Publish on quality change
- Enable/disable tag publishing

### Status Dashboard Tab
- Real-time module health monitoring
- MQTT connection state
- Publishing statistics (success rate, message counts)
- Tag read statistics
- Uptime tracking
- Auto-refresh every 5 seconds

## Development

The React app is built with:
- React 18
- TypeScript 5
- Webpack 5
- CSS modules

External dependencies (React, ReactDOM) are provided by the Ignition Gateway and configured as externals in webpack to avoid bundling them.

## Integration

The built JavaScript bundle (`mqtt-config.js`) is:
1. Served by Ignition at `/res/mqtt-uns-publisher/mqtt-config.js`
2. Loaded as a SystemJS module in the Gateway web interface
3. Mounted at the path `/mqtt-uns-publisher` in the Config section
4. Registered in the Gateway navigation sidebar

When users save configuration changes via the UI, the module automatically:
- Saves to the internal database (PersistentRecords)
- Triggers RecordListeners in the module
- Reconnects to MQTT broker or restarts tag subscriptions with new settings
- No Gateway restart required
