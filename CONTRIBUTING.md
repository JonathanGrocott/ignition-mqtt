# Contributing

Thanks for your interest in contributing! This project is an Ignition 8.3 module set (MQTT UNS Publisher and MQTT SparkplugB Publisher).

## Getting Started

### Prerequisites
- Java 17 (JDK 17)
- Node.js 16+ and npm

### Build
```bash
./build.sh
```

If you only need one module:
```bash
./build.sh uns
./build.sh sparkplug
```

The output `.modl` files are written to:
- `mqtt-uns-module/build/`
- `mqtt-sparkplug-module/build/`

## Reporting Issues
Please include:
- Ignition version and edition
- Operating system
- Module version
- Steps to reproduce
- Relevant gateway logs

## Pull Requests
Keep PRs focused and small when possible. If you are changing behavior, include:
- A short summary of the change
- Any migration notes or config changes
- How you tested the change

## Code of Conduct
Be respectful and constructive in issues and PRs.
