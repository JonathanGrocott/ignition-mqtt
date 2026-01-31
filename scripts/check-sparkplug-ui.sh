#!/bin/bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8088}"

echo "Checking Sparkplug UI endpoints on $BASE_URL"

check() {
  local path="$1"
  local url="$BASE_URL$path"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
  echo "$status $path"
}

check "/data/mqtt-sparkplug-publisher/ui/sparkplug-config.js"
check "/data/com.inductiveautomation.mqtt.sparkplugb/ui/sparkplug-config.js"
check "/data/mqtt-sparkplug-publisher/ping"
check "/data/com.inductiveautomation.mqtt.sparkplugb/ping"
check "/data/mqtt-sparkplug-publisher/config/broker"
check "/data/com.inductiveautomation.mqtt.sparkplugb/config/broker"
check "/res/mqtt-sparkplug-publisher/sparkplug-config.js"
check "/res/com.inductiveautomation.mqtt.sparkplugb/sparkplug-config.js"
