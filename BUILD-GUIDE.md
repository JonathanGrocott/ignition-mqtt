# Quick Build Guide

## One-Command Build

The easiest way to build the complete module (frontend + backend):

### macOS/Linux:
```bash
./build.sh
```

### Windows:
```cmd
build.bat
```

This single script will:
1. ✓ Check prerequisites (Node.js, npm, Java, Gradle)
2. ✓ Install npm dependencies (if needed)
3. ✓ Build React frontend → `mqtt-config.js`
4. ✓ Build Java backend → `.modl` file
5. ✓ Show you the final module location

## What You Need

- **Node.js 16+** and npm
- **Java 11+**
- **Gradle** (wrapper included)

## Output

The build script creates:
- `mqtt-gateway/src/main/resources/mounted/mqtt-config.js` - Frontend bundle
- `build/MQTT-UNS-Publisher.unsigned.modl` - Complete module file

## Manual Build (if needed)

### Frontend Only:
```bash
cd mqtt-gateway/web-ui
npm install
npm run build
```

### Backend Only:
```bash
./gradlew clean build
```

## Install in Ignition

1. Go to `http://localhost:8088` (your Gateway)
2. Navigate to **Config → System → Modules**
3. Click **Install or Upgrade a Module**
4. Upload `build/MQTT-UNS-Publisher.unsigned.modl`
5. Restart Gateway when prompted
6. Navigate to **Config → MQTT UNS Publisher** to see the Web UI

## Troubleshooting

**"Node.js not found"**
- Install from https://nodejs.org/ (LTS version)

**"Java not found"**
- Install OpenJDK 11+: `brew install openjdk@17` (macOS)

**"Frontend build failed"**
- Delete `mqtt-gateway/web-ui/node_modules`
- Run `npm install` again

**"Gradle build failed"**
- Check Java version: `java -version`
- Should be Java 11 or higher

## Development Workflow

### Working on Frontend:
```bash
cd mqtt-gateway/web-ui
npm run dev  # Auto-rebuild on file changes
```

Then in another terminal:
```bash
./gradlew build  # Rebuild module
```

### Quick Iteration:
1. Make frontend changes
2. Run `./build.sh`
3. Upload to Gateway
4. Test changes

The build script is smart - it only reinstalls npm dependencies if `node_modules/` is missing, making subsequent builds faster.
