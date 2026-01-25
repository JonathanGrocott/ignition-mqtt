# Event-Driven Refactoring Summary

## Overview

The MQTT UNS Publisher module has been successfully refactored from a **polling-based** approach to an **event-driven** approach using the Ignition 8.3 SDK's `TagChangeListener` API.

## What Changed

### 1. TagSubscriptionManager.java (Refactored)

**Before (Polling):**
- Used `ScheduledExecutorService` to poll tags every `pollRateMs` (default 1000ms)
- Called `readAsync()` on all tags at fixed intervals
- Required executor management and thread coordination
- Had latency up to the poll interval

**After (Event-Driven):**
- Implements `TagChangeListener` interface
- Uses `subscribeAsync()` to register for tag change events
- Receives immediate callbacks when tag values change
- No polling overhead or latency
- More efficient CPU usage for slow-changing tags

**Key Implementation Details:**
```java
private class MqttTagChangeListener implements TagChangeListener {
    @Override
    public void tagChanged(TagChangeEvent event) {
        TagPath tagPath = event.getTagPath();
        QualifiedValue newValue = event.getValue();
        
        // Skip initial value if already published
        if (event.isInitial() && lastPublishedValues.containsKey(tagPath)) {
            return;
        }
        
        // Apply deadband and quality filtering
        if (shouldPublish(tagPath, newValue)) {
            publishTagValue(tagPath, newValue);
            lastPublishedValues.put(tagPath, newValue);
        }
    }
    
    @Override
    public boolean isLightweight() {
        // Return true to avoid leasing tags (more efficient)
        return true;
    }
}
```

**Subscription:**
```java
private void subscribeToTags() {
    GatewayTagManager tagManager = gatewayContext.getTagManager();
    
    List<TagChangeListener> listeners = new ArrayList<>();
    for (int i = 0; i < monitoredTags.size(); i++) {
        listeners.add(tagChangeListener);
    }
    
    CompletableFuture<Void> future = tagManager.subscribeAsync(monitoredTags, listeners);
    future.get(30, TimeUnit.SECONDS);
}
```

**Cleanup:**
```java
public void shutdown() {
    GatewayTagManager tagManager = gatewayContext.getTagManager();
    
    List<TagChangeListener> listeners = new ArrayList<>();
    for (int i = 0; i < monitoredTags.size(); i++) {
        listeners.add(tagChangeListener);
    }
    
    tagManager.unsubscribeAsync(monitoredTags, listeners).get(10, TimeUnit.SECONDS);
}
```

### 2. TagPublishConfig.java (Simplified)

**Removed Field:**
- `pollRateMs` - No longer needed with event-driven approach

**Removed Validation:**
- Poll rate range checks (100-60000ms)

**Before:**
```java
@SerializedName("pollRateMs")
private long pollRateMs;

public void validate() {
    // ...
    if (pollRateMs < 100) {
        throw new IllegalArgumentException("Poll rate must be at least 100ms");
    }
    if (pollRateMs > 60000) {
        throw new IllegalArgumentException("Poll rate cannot exceed 60 seconds");
    }
}
```

**After:**
```java
// pollRateMs field removed entirely
public void validate() {
    if (enabled && tagProviders.isEmpty() && tagFolders.isEmpty()) {
        throw new IllegalArgumentException(
            "Tag publishing is enabled but no providers or folders are configured"
        );
    }
    if (valueDeadband < 0) {
        throw new IllegalArgumentException("Value deadband cannot be negative");
    }
}
```

### 3. Configuration File (Updated)

**mqtt-uns-config-combined-example.json:**

**Before:**
```json
{
  "tags": {
    "enabled": true,
    "tagProviders": ["default"],
    "tagFolders": ["[default]TestTags"],
    "valueDeadband": 0.1,
    "pollRateMs": 1000,
    "publishOnQualityChange": true,
    "includeMetadata": true
  }
}
```

**After:**
```json
{
  "tags": {
    "enabled": true,
    "tagProviders": ["default"],
    "tagFolders": ["[default]TestTags"],
    "valueDeadband": 0.1,
    "publishOnQualityChange": true,
    "includeMetadata": true
  }
}
```

## Benefits

### 1. True Real-Time Response
- **Before:** Up to 1 second latency (default poll rate)
- **After:** Immediate notification when tags change

### 2. More Efficient
- **Before:** Continuous polling even when tags don't change
- **After:** Only processes actual changes

### 3. Scales Better
- **Before:** CPU usage = constant (all tags polled every interval)
- **After:** CPU usage proportional to change rate

**Example:**
- 1000 tags, 1% changing per second
- **Polling:** Reads all 1000 tags/second = 1000 reads/sec
- **Event-Driven:** Only 10 callbacks/second = 10 reads/sec (100x more efficient)

### 4. Simpler Code
- **Before:** Executor service, scheduled tasks, polling loops
- **After:** Simple listener callbacks, no thread management

### 5. Better Resource Cleanup
- **Before:** Cancel scheduled tasks, shutdown executor, await termination
- **After:** Simple `unsubscribeAsync()` call

## API Research Findings

### TagChangeListener Interface

**Package:** `com.inductiveautomation.ignition.common.tags.model.event`

**Methods:**
- `void tagChanged(TagChangeEvent event)` - Called when tag value changes
- `boolean isLightweight()` - Optional, prevents tag leasing (more efficient)
- `SecurityContext getSecurityContext()` - Optional, for permission handling

### TagChangeEvent Class

**Properties:**
- `TagPath getTagPath()` - The tag that changed
- `QualifiedValue getValue()` - New value with quality and timestamp
- `boolean isInitial()` - True for first callback after subscription
- `int[] getChangedArrayIndexes()` - For array tags (which indexes changed)

### TagManager Methods

**Subscription:**
- `subscribeAsync(TagPath tagPath, TagChangeListener listener)` - Single tag
- `subscribeAsync(List<TagPath> tagPaths, List<TagChangeListener> listeners)` - Batch

**Unsubscription:**
- `unsubscribeAsync(TagPath tagPath, TagChangeListener listener)` - Single tag
- `unsubscribeAsync(List<TagPath> tagPaths, List<TagChangeListener> listeners)` - Batch

**Documentation:**
- https://files.inductiveautomation.com/sdk/javadoc/ignition83/8.3.0/com/inductiveautomation/ignition/common/tags/model/TagManager.html
- https://files.inductiveautomation.com/sdk/javadoc/ignition83/8.3.0/com/inductiveautomation/ignition/common/tags/model/event/TagChangeListener.html

## Compatibility

### Breaking Changes

**Configuration Files:**
- Existing configs with `pollRateMs` will still load (Gson ignores unknown fields)
- The field is simply ignored if present
- No migration required

**Behavior:**
- Same deadband filtering logic
- Same quality change detection
- Same topic mapping and JSON payload generation
- Only difference: timing of publishes (now immediate instead of polled)

### Backward Compatibility

**Good News:**
- Old configuration files work without modification
- All existing features preserved
- No API changes for users
- Module behavior is strictly better (faster, more efficient)

## Testing Recommendations

### 1. Basic Functionality
- Start module with existing config
- Verify tags are discovered
- Verify subscriptions are created
- Change tag values in Ignition Designer
- Verify MQTT messages are published immediately

### 2. Performance Testing
- Subscribe to 100+ tags
- Change values rapidly
- Monitor CPU usage (should be lower than polling)
- Monitor memory usage
- Verify no memory leaks over time

### 3. Edge Cases
- Tag quality changes (Good → Bad)
- Initial values on subscription
- Rapid tag changes (within deadband)
- Module shutdown/restart
- MQTT disconnection/reconnection

### 4. Stress Testing
- 1000+ tags
- High change rate (100+ changes/sec)
- Long running (24+ hours)
- Verify statistics accuracy

## Statistics

All existing statistics remain accurate:
- `messagesPublished` - Incremented on successful publish
- `messagesFailedToPublish` - Incremented on publish error
- `tagReadsSuccessful` - Incremented on each tag change callback
- `tagReadsFailed` - Incremented on callback error

## Known Limitations

### Still Present
- No TLS/SSL support (plaintext MQTT only)
- No Gateway web UI (JSON file configuration only)
- Not compatible with Maker Edition (requires Standard/Enterprise)

### Resolved
- ✅ No more polling latency (now event-driven)
- ✅ No more configurable poll rate needed
- ✅ More efficient CPU usage

## Next Steps

### Phase 6: Gateway Web UI (Future)
- Create REST API routes
- Build HTML/CSS/JS configuration interface
- Add real-time status dashboard
- See MIGRATION-GUIDE.md for details

### Phase 7: Advanced Features (Future)
- TLS/SSL support
- Custom payload templates
- Conditional publishing rules
- Multiple broker support

## Conclusion

The refactoring from polling to event-driven subscriptions is a significant improvement:

**Technical Benefits:**
- ✅ Lower latency (immediate vs up to 1 second)
- ✅ More efficient (10-100x fewer reads)
- ✅ Simpler code (no executor service)
- ✅ Better SDK usage (using intended API)

**User Benefits:**
- ✅ Faster MQTT updates
- ✅ Lower CPU usage
- ✅ Same configuration (backward compatible)
- ✅ More reliable (fewer moving parts)

**Why Polling Was Originally Used:**
- Developer likely didn't know about `subscribeAsync()` API
- Ignition 8.3 SDK documentation may have been incomplete
- Event-driven API wasn't widely known or documented

**Why Event-Driven Is Better:**
- Uses official Ignition SDK API
- More efficient and scalable
- True real-time response
- Industry standard approach for tag monitoring
