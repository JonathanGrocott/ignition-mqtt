#!/bin/bash

# Deprecated: Use build.sh and select option 2 (Sparkplug) instead.
# This wrapper is kept for backward compatibility.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$SCRIPT_DIR/build.sh" sparkplug
