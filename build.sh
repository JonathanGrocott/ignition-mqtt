#!/bin/bash

###################################################################################
# Ignition MQTT Modules - Build Script
###################################################################################
# Builds one or both modules:
# 1) MQTT UNS Publisher
# 2) MQTT SparkplugB Publisher
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
UNS_WEB_UI_DIR="$PROJECT_ROOT/mqtt-gateway/web-ui"
UNS_MOUNTED_DIR="$PROJECT_ROOT/mqtt-gateway/src/main/resources/mounted"
UNS_BUILD_DIR="$PROJECT_ROOT/mqtt-uns-module/build"

SPARK_WEB_UI_DIR="$PROJECT_ROOT/mqtt-sparkplug-gateway/web-ui"
SPARK_MOUNTED_DIR="$PROJECT_ROOT/mqtt-sparkplug-gateway/src/main/resources/mounted"
SPARK_MODULE_MOUNTED_DIR="$PROJECT_ROOT/mqtt-sparkplug-module/src/main/resources/mounted"
SPARK_BUILD_DIR="$PROJECT_ROOT/mqtt-sparkplug-module/build"

CLEAN_DONE=0

print_header() {
    echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}   Ignition MQTT Modules - Build${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}"
    echo ""
}

set_java_home_if_needed() {
    local java_version
    java_version="$(java -version 2>&1 | awk -F\" '/version/ {print $2}' || true)"

    if [[ "$java_version" != 17.* ]]; then
        if [ -d "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home" ]; then
            export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home
            export PATH="$JAVA_HOME/bin:$PATH"
            echo -e "${BLUE}  → Setting JAVA_HOME to $JAVA_HOME${NC}"
        elif [ -d "/opt/homebrew/opt/openjdk@17" ]; then
            export JAVA_HOME=/opt/homebrew/opt/openjdk@17
            export PATH="$JAVA_HOME/bin:$PATH"
            echo -e "${BLUE}  → Setting JAVA_HOME to $JAVA_HOME${NC}"
        elif [ -d "/usr/lib/jvm/java-17-openjdk" ]; then
            export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
            export PATH="$JAVA_HOME/bin:$PATH"
            echo -e "${BLUE}  → Setting JAVA_HOME to $JAVA_HOME${NC}"
        fi
    fi
}

check_prereqs() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    set_java_home_if_needed

    if ! command -v node &> /dev/null; then
        echo -e "${RED}✗ Node.js is not installed. Please install Node.js 16+ and try again.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Node.js found: $(node --version)${NC}"

    if ! command -v npm &> /dev/null; then
        echo -e "${RED}✗ npm is not installed. Please install npm and try again.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ npm found: $(npm --version)${NC}"

    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ Java is not installed. Please install Java 17+ and try again.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"

    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        echo -e "${RED}✗ Gradle wrapper not found. Please ensure gradlew exists in project root.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Gradle wrapper found${NC}"
    echo ""
}

ensure_gradle_clean() {
    if [ "$CLEAN_DONE" -eq 0 ]; then
        echo -e "${BLUE}  → Cleaning previous builds...${NC}"
        (cd "$PROJECT_ROOT" && ./gradlew clean --quiet)
        CLEAN_DONE=1
    fi
}

build_uns_ui() {
    echo -e "${YELLOW}[UNS] Building frontend (React/TypeScript)...${NC}"
    cd "$UNS_WEB_UI_DIR"
    if [ ! -d "node_modules" ]; then
        echo -e "${BLUE}  → Installing npm dependencies...${NC}"
        npm install
    else
        echo -e "${BLUE}  → npm dependencies already installed${NC}"
    fi
    echo -e "${BLUE}  → Building React application with webpack...${NC}"
    npm run build
    if [ ! -f "$UNS_MOUNTED_DIR/mqtt-config.js" ]; then
        echo -e "${RED}✗ Frontend build failed: mqtt-config.js not found${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Frontend built successfully: mqtt-config.js ($(du -h "$UNS_MOUNTED_DIR/mqtt-config.js" | cut -f1))${NC}"
    echo ""
}

build_sparkplug_ui() {
    echo -e "${YELLOW}[Sparkplug] Building frontend (React/TypeScript)...${NC}"
    cd "$SPARK_WEB_UI_DIR"
    if [ ! -d "node_modules" ]; then
        echo -e "${BLUE}  → Installing npm dependencies...${NC}"
        npm install
    else
        echo -e "${BLUE}  → npm dependencies already installed${NC}"
    fi
    echo -e "${BLUE}  → Building React application with webpack...${NC}"
    npm run build
    if [ ! -f "$SPARK_MOUNTED_DIR/sparkplug-config.js" ]; then
        echo -e "${RED}✗ Frontend build failed: sparkplug-config.js not found${NC}"
        exit 1
    fi
    mkdir -p "$SPARK_MODULE_MOUNTED_DIR"
    cp "$SPARK_MOUNTED_DIR/sparkplug-config.js" "$SPARK_MODULE_MOUNTED_DIR/sparkplug-config.js"
    if [ -f "$SPARK_MOUNTED_DIR/sparkplug-config.js.map" ]; then
        cp "$SPARK_MOUNTED_DIR/sparkplug-config.js.map" "$SPARK_MODULE_MOUNTED_DIR/sparkplug-config.js.map"
    fi
    echo -e "${GREEN}✓ Frontend built successfully: sparkplug-config.js ($(du -h "$SPARK_MOUNTED_DIR/sparkplug-config.js" | cut -f1))${NC}"
    echo ""
}

build_uns_backend() {
    echo -e "${YELLOW}[UNS] Building backend (Java/Gradle)...${NC}"
    ensure_gradle_clean
    (cd "$PROJECT_ROOT" && ./gradlew :mqtt-uns-module:build)
    sign_module uns
    echo -e "${GREEN}✓ Backend built successfully${NC}"
    echo ""
}

build_sparkplug_backend() {
    echo -e "${YELLOW}[Sparkplug] Building backend (Java/Gradle)...${NC}"
    ensure_gradle_clean
    (cd "$PROJECT_ROOT" && ./gradlew :mqtt-sparkplug-module:build)
    sign_module sparkplug
    echo -e "${GREEN}✓ Backend built successfully${NC}"
    echo ""
}

sign_module() {
    local target="$1"
    if [ "${SKIP_MODULE_SIGNING:-0}" = "1" ]; then
        echo -e "${YELLOW}  → Skipping module signing because SKIP_MODULE_SIGNING=1${NC}"
        return
    fi

    echo -e "${BLUE}  → Signing module with IA module-signer...${NC}"
    "$PROJECT_ROOT/scripts/sign-modules.sh" "$target"
}

show_uns_artifact() {
    local modl_file
    modl_file=$(find "$UNS_BUILD_DIR" -name "*.modl" ! -name "*.unsigned.modl" -type f | head -n 1)
    if [ -z "$modl_file" ]; then
        modl_file=$(find "$UNS_BUILD_DIR" -name "*.unsigned.modl" -type f | head -n 1)
    fi
    if [ -z "$modl_file" ]; then
        echo -e "${RED}✗ Could not find UNS .modl file in build directory${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ UNS module file created: $(basename "$modl_file") ($(du -h "$modl_file" | cut -f1))${NC}"
    echo -e "  $modl_file"
    echo ""
}

show_sparkplug_artifact() {
    local modl_file
    modl_file=$(find "$SPARK_BUILD_DIR" -name "*.modl" ! -name "*.unsigned.modl" -type f | head -n 1)
    if [ -z "$modl_file" ]; then
        modl_file=$(find "$SPARK_BUILD_DIR" -name "*.unsigned.modl" -type f | head -n 1)
    fi
    if [ -z "$modl_file" ]; then
        echo -e "${RED}✗ Could not find Sparkplug .modl file in build directory${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Sparkplug module file created: $(basename "$modl_file") ($(du -h "$modl_file" | cut -f1))${NC}"
    echo -e "  $modl_file"
    echo ""
}

build_uns() {
    build_uns_ui
    build_uns_backend
    show_uns_artifact
}

build_sparkplug() {
    build_sparkplug_ui
    build_sparkplug_backend
    show_sparkplug_artifact
}

prompt_selection() {
    local choice
    local input="/dev/stdin"
    if [ -r /dev/tty ]; then
        input="/dev/tty"
    elif [ ! -t 0 ]; then
        echo -e "${YELLOW}No interactive terminal detected; defaulting to 'both'.${NC}" >&2
        echo "both"
        return
    fi

    while true; do
        echo "Select build target:" >&2
        echo "  1) Build UNS Module" >&2
        echo "  2) Build Sparkplug Module" >&2
        echo "  3) Build Both" >&2
        printf "Enter choice (1-3) [default 3]: " >&2
        read -r choice < "$input" || choice=""
        case "$choice" in
            1) echo "uns"; return ;;
            2) echo "sparkplug"; return ;;
            3) echo "both"; return ;;
            "") echo "both"; return ;;
            *) echo -e "${RED}Invalid selection. Please choose 1-3.${NC}" >&2 ;;
        esac
    done
}

main() {
    print_header
    check_prereqs

    local target="$1"
    if [ -z "$target" ]; then
        target=$(prompt_selection)
    fi

    case "$target" in
        uns)
            build_uns
            ;;
        sparkplug)
            build_sparkplug
            ;;
        both)
            build_uns
            build_sparkplug
            ;;
        *)
            echo -e "${RED}Invalid selection. Please run again and choose 1-3.${NC}"
            exit 1
            ;;
    esac

    echo -e "${GREEN}Build completed successfully.${NC}"
}

main "$@"
