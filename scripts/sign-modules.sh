#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

KEYSTORE="${MODULE_SIGNING_KEYSTORE:-$REPO_ROOT/certs/module-signing-keystore.jks}"
CERT_CHAIN="${MODULE_SIGNING_CHAIN:-$REPO_ROOT/certs/module-signing-chain.p7b}"
ALIAS="${MODULE_SIGNING_ALIAS:-ignition-mqtt-self-signed}"
PASSWORD="${MODULE_SIGNING_PASSWORD:-ignition-mqtt}"

if [ ! -f "$KEYSTORE" ] || [ ! -f "$CERT_CHAIN" ]; then
    "$REPO_ROOT/scripts/create-self-signed-module-cert.sh"
fi

SIGNER_JAR="$("$REPO_ROOT/scripts/download-module-signer.sh" | tail -n 1)"

sign_one() {
    local module_in="$1"
    local module_out="$2"

    if [ ! -f "$module_in" ]; then
        echo "Missing unsigned module: $module_in" >&2
        return 1
    fi

    echo "Signing $(basename "$module_in") -> $(basename "$module_out")"
    rm -f "$module_out"
    java -jar "$SIGNER_JAR" \
        -keystore="$KEYSTORE" \
        -keystore-pwd="$PASSWORD" \
        -alias="$ALIAS" \
        -alias-pwd="$PASSWORD" \
        -chain="$CERT_CHAIN" \
        -module-in="$module_in" \
        -module-out="$module_out"
}

TARGET="${1:-both}"

case "$TARGET" in
    uns)
        UNS_IN="$(find "$REPO_ROOT/mqtt-uns-module/build" -name "*.unsigned.modl" -type f | head -n 1)"
        UNS_OUT="${UNS_IN/.unsigned.modl/.modl}"
        sign_one "$UNS_IN" "$UNS_OUT"
        ;;
    sparkplug)
        SPARK_IN="$(find "$REPO_ROOT/mqtt-sparkplug-module/build" -name "*.unsigned.modl" -type f | head -n 1)"
        SPARK_OUT="${SPARK_IN/.unsigned.modl/.modl}"
        sign_one "$SPARK_IN" "$SPARK_OUT"
        ;;
    both)
        "$0" uns
        "$0" sparkplug
        ;;
    *)
        echo "Usage: $0 [uns|sparkplug|both]" >&2
        exit 1
        ;;
esac
