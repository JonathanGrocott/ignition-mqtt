#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MODL_PATH="${1:-$REPO_ROOT/mqtt-sparkplug-module/build/MQTT-SparkplugB-Publisher.unsigned.modl}"

if [ ! -f "$MODL_PATH" ]; then
  echo "Missing modl: $MODL_PATH"
  exit 1
fi

python3 - "$MODL_PATH" <<'PY'
import io
import sys
import zipfile

modl_path = sys.argv[1]
with zipfile.ZipFile(modl_path) as z:
    if "module.xml" not in z.namelist():
        print("module.xml not found")
        sys.exit(2)
    module_xml = z.read("module.xml").decode("utf-8", errors="replace")
    print("module.xml ok")
    jar_name = None
    for name in z.namelist():
        if name.startswith("mqtt-sparkplug-gateway") and name.endswith(".jar"):
            jar_name = name
            break
    if not jar_name:
        print("sparkplug gateway jar not found")
        sys.exit(3)
    jar_bytes = z.read(jar_name)
    with zipfile.ZipFile(io.BytesIO(jar_bytes)) as jar:
        if "mounted/sparkplug-config.js" not in jar.namelist():
            print("sparkplug-config.js missing in gateway jar")
            sys.exit(4)
    print("sparkplug-config.js found in gateway jar")
PY "$MODL_PATH"
