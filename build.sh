#!/bin/bash

# Build script for MQTT UNS Publisher module

set -e  # Exit on error

# Set JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
        export JAVA_HOME=/opt/homebrew/opt/openjdk@17
        echo "Setting JAVA_HOME to $JAVA_HOME"
    else
        echo "Error: JAVA_HOME not set and openjdk@17 not found"
        echo "Please install OpenJDK 17: brew install openjdk@17"
        exit 1
    fi
fi

echo "Building MQTT UNS Publisher module..."
echo "Java version:"
"$JAVA_HOME/bin/java" -version

./gradlew clean build

echo ""
echo "✅ Build successful!"
echo ""
echo "Module file: build/MQTT-UNS-Publisher.unsigned.modl"
ls -lh build/*.modl
echo ""
echo "To install:"
echo "1. Go to http://localhost:8088 (your Gateway)"
echo "2. Config > Modules > Install or Upgrade a Module"
echo "3. Select build/MQTT-UNS-Publisher.unsigned.modl"
