#!/bin/bash

###################################################################################
# MQTT SparkplugB Publisher Module - Complete Build Script
###################################################################################
# This script builds the SparkplugB module including:
# 1. React/TypeScript frontend (web UI)
# 2. Java backend (Gateway module)
# 3. Packages everything into a .modl file
###################################################################################

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project paths
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB_UI_DIR="$PROJECT_ROOT/mqtt-sparkplug-gateway/web-ui"
MOUNTED_DIR="$PROJECT_ROOT/mqtt-sparkplug-gateway/src/main/resources/mounted"
MODULE_MOUNTED_DIR="$PROJECT_ROOT/mqtt-sparkplug-module/src/main/resources/mounted"
SPARKPLUG_BUILD_DIR="$PROJECT_ROOT/mqtt-sparkplug-module/build"

echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   MQTT SparkplugB Publisher Module - Complete Build${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo ""

###################################################################################
# Step 1: Check Prerequisites
###################################################################################
echo -e "${YELLOW}[1/4] Checking prerequisites...${NC}"

# Set JAVA_HOME if not already set (macOS specific)
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
        export JAVA_HOME=/opt/homebrew/opt/openjdk@17
        echo -e "${BLUE}  → Setting JAVA_HOME to $JAVA_HOME${NC}"
    elif [ -d "/usr/lib/jvm/java-11-openjdk" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
        echo -e "${BLUE}  → Setting JAVA_HOME to $JAVA_HOME${NC}"
    fi
fi

# Check for Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java is not installed. Please install Java 11+ and try again.${NC}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1)
echo -e "${GREEN}✓ Java found: $JAVA_VERSION${NC}"

# Check for Gradle wrapper
if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
    echo -e "${RED}✗ Gradle wrapper not found. Please ensure gradlew exists in project root.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Gradle wrapper found${NC}"

echo ""

###################################################################################
# Step 2: Build Frontend (React/TypeScript)
###################################################################################
echo -e "${YELLOW}[2/4] Building frontend (React/TypeScript)...${NC}"

cd "$WEB_UI_DIR"

if [ ! -d "node_modules" ]; then
    echo -e "${BLUE}  → Installing npm dependencies...${NC}"
    npm install
else
    echo -e "${BLUE}  → npm dependencies already installed${NC}"
fi

echo -e "${BLUE}  → Building React application with webpack...${NC}"
npm run build

if [ ! -f "$MOUNTED_DIR/sparkplug-config.js" ]; then
    echo -e "${RED}✗ Frontend build failed: sparkplug-config.js not found${NC}"
    exit 1
fi

mkdir -p "$MODULE_MOUNTED_DIR"
cp "$MOUNTED_DIR/sparkplug-config.js" "$MODULE_MOUNTED_DIR/sparkplug-config.js"
if [ -f "$MOUNTED_DIR/sparkplug-config.js.map" ]; then
    cp "$MOUNTED_DIR/sparkplug-config.js.map" "$MODULE_MOUNTED_DIR/sparkplug-config.js.map"
fi

FILE_SIZE=$(du -h "$MOUNTED_DIR/sparkplug-config.js" | cut -f1)
echo -e "${GREEN}✓ Frontend built successfully: sparkplug-config.js ($FILE_SIZE)${NC}"
echo ""

###################################################################################
# Step 3: Build Backend (Java/Gradle)
###################################################################################
echo -e "${YELLOW}[3/4] Building backend (Java/Gradle)...${NC}"

cd "$PROJECT_ROOT"

# Clean previous builds
echo -e "${BLUE}  → Cleaning previous builds...${NC}"
./gradlew clean --quiet

# Build the module
echo -e "${BLUE}  → Compiling Java code and packaging module...${NC}"
./gradlew :mqtt-sparkplug-module:build

# Check if build succeeded
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Gradle build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Backend built successfully${NC}"
echo ""

###################################################################################
# Step 4: Locate and Display Output
###################################################################################
echo -e "${YELLOW}[4/4] Locating build artifacts...${NC}"

# Find the .modl file
MODL_FILE=$(find "$SPARKPLUG_BUILD_DIR" -name "*.modl" -type f | head -n 1)

if [ -z "$MODL_FILE" ]; then
    echo -e "${RED}✗ Could not find .modl file in build directory${NC}"
    exit 1
fi

MODL_SIZE=$(du -h "$MODL_FILE" | cut -f1)
MODL_NAME=$(basename "$MODL_FILE")

echo -e "${GREEN}✓ Module file created: $MODL_NAME ($MODL_SIZE)${NC}"
echo ""

###################################################################################
# Success Summary
###################################################################################
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}   Build Completed Successfully! 🎉${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}Module file location:${NC}"
echo -e "  $MODL_FILE"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "  1. Navigate to your Ignition Gateway web interface"
echo -e "  2. Go to Config → System → Modules"
echo -e "  3. Click 'Install or Upgrade a Module'"
echo -e "  4. Upload: $MODL_NAME"
echo -e "  5. After installation, navigate to Config → MQTT SparkplugB Publisher"
echo ""
echo -e "${GREEN}Happy publishing! 📡${NC}"
