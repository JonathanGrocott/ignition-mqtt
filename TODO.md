# TODO

## Known Issue: Gateway Config Page Switching
- Symptom: From "MQTT UNS Publisher → Configuration", clicking "MQTT SparkplugB Publisher → Configuration" does nothing (no network calls, no console errors). Switching to a different gateway page first and then back works.
- Repro: In Gateway UI, open UNS Configuration page, then click Sparkplug Configuration page in Connections nav.
- Status: Not fixed yet. Standard React component exports are in place and navigation IDs were made unique, but navigation still does not trigger.
- Suspected areas to investigate:
  - Ignition navigation model caching between pages
  - SystemJS module resolution on repeated navigation
  - Gateway web UI shell click handling / navigation listener
  - Potential requirement to implement specific export signature or navigation hook
