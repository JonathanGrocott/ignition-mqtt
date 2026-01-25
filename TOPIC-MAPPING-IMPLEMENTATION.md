# Topic Mapping Feature Implementation

## Current Status
✅ Frontend UI completed with topic mapping interface
✅ CSS styles added for mapping UI
✅ Database field added (TopicMappings)
⏳ Backend implementation needed
⏳ Topic mapper logic update needed

## What's Been Done

### 1. Frontend (COMPLETED)
- Added `TopicMapping` interface to types.ts
- Added `SubscriptionStatus` and `ActiveTagSubscription` types
- Updated `TagSelection.tsx` with topic mapping UI:
  - Add/remove topic mappings
  - Enable/disable individual mappings
  - Visual display of source → topic transformations
- Added CSS styles for mapping interface

### 2. Database (COMPLETED)
- Added `TopicMappings` field to `MqttTagConfigRecord`:
  ```java
  public static final StringField TopicMappings = new StringField(META, "TopicMappings", 4000)
      .setDefault("[]");
  ```
- Added getters/setters: `getTopicMappingsJson()`, `setTopicMappingsJson()`

## What Needs To Be Done

### 3. Model Layer

**File:** `mqtt-common/src/main/java/.../model/TagPublishConfig.java`

Add topic mappings field:
```java
@SerializedName("topicMappings")
private List<TopicMapping> topicMappings;

// In constructor:
this.topicMappings = new ArrayList<>();

// Add getter/setter:
public List<TopicMapping> getTopicMappings() {
    return topicMappings;
}

public void setTopicMappings(List<TopicMapping> topicMappings) {
    this.topicMappings = topicMappings != null ? topicMappings : new ArrayList<>();
}
```

**Create new class:** `mqtt-common/src/main/java/.../model/TopicMapping.java`
```java
package com.inductiveautomation.ignition.examples.mqtt.common.model;

import com.google.gson.annotations.SerializedName;

public class TopicMapping {
    @SerializedName("id")
    private String id;
    
    @SerializedName("sourcePattern")
    private String sourcePattern;  // e.g., "[default]TestTags"
    
    @SerializedName("topicPrefix")
    private String topicPrefix;    // e.g., "enterprise/site1/area2"
    
    @SerializedName("enabled")
    private boolean enabled;
    
    // Constructors, getters, setters
    
    public TopicMapping() {
        this.enabled = true;
    }
    
    public TopicMapping(String sourcePattern, String topicPrefix) {
        this.sourcePattern = sourcePattern;
        this.topicPrefix = topicPrefix;
        this.enabled = true;
    }
    
    // ... getters and setters for all fields
}
```

### 4. Record Mapper

**File:** `mqtt-gateway/src/main/java/.../records/RecordMapper.java`

Update `toModel(MqttTagConfigRecord)`:
```java
// Add after line 82:
Type topicMappingsType = new TypeToken<List<TopicMapping>>(){}.getType();
List<TopicMapping> mappings = gson.fromJson(record.getTopicMappingsJson(), topicMappingsType);
config.setTopicMappings(mappings);
```

Update `fromModel(TagPublishConfig, MqttTagConfigRecord)`:
```java
// Add after line 105:
record.setTopicMappingsJson(gson.toJson(model.getTopicMappings()));
```

### 5. Data Routes

**File:** `mqtt-gateway/src/main/java/.../web/MqttDataRoutes.java`

In `saveTagConfig()`, add after line 427:
```java
if (data.containsKey("topicMappings")) {
    record.setTopicMappingsJson(gson.toJson(data.get("topicMappings")));
}
```

In `getTagConfig()`, add to response (after line 352):
```java
// Parse topic mappings
try {
    JsonElement mappings = JsonParser.parseString(record.getTopicMappingsJson());
    if (mappings.isJsonArray()) {
        data.add("topicMappings", mappings.getAsJsonArray());
    }
} catch (Exception e) {
    logger.warn("Error parsing topic mappings", e);
}
```

In `saveTagConfig()` response, add after line 460:
```java
// Parse topic mappings for response
try {
    JsonElement mappings = JsonParser.parseString(record.getTopicMappingsJson());
    if (mappings.isJsonArray()) {
        savedData.add("topicMappings", mappings.getAsJsonArray());
    }
} catch (Exception e) {
    logger.warn("Error parsing topic mappings in response", e);
}
```

### 6. Topic Mapper Enhancement

**File:** `mqtt-gateway/src/main/java/.../MqttTopicMapper.java`

Add method to apply topic mappings:
```java
private List<TopicMapping> topicMappings = new ArrayList<>();

public void setTopicMappings(List<TopicMapping> mappings) {
    this.topicMappings = mappings != null ? mappings : new ArrayList<>();
}

/**
 * Maps tag path to MQTT topic, applying custom topic mappings if they match
 */
public String mapTagToTopicWithMappings(TagPath tagPath) {
    String fullPath = tagPath.toStringFull();
    
    // Check if any topic mapping matches this tag
    for (TopicMapping mapping : topicMappings) {
        if (!mapping.isEnabled()) {
            continue;
        }
        
        // Check if tag path starts with source pattern
        if (fullPath.startsWith(mapping.getSourcePattern())) {
            // Replace source pattern with topic prefix
            String remainder = fullPath.substring(mapping.getSourcePattern().length());
            if (remainder.startsWith("/")) {
                remainder = remainder.substring(1);
            }
            
            String topic = mapping.getTopicPrefix();
            if (!remainder.isEmpty()) {
                topic = topic + "/" + sanitizeTopicSegment(remainder);
            }
            
            return topic;
        }
    }
    
    // No mapping found, use default mapping
    return mapTagToTopic(tagPath);
}
```

### 7. Tag Subscription Manager

**File:** `mqtt-gateway/src/main/java/.../TagSubscriptionManager.java`

Update `start()` method to set topic mappings:
```java
public void start(TagPublishConfig config) {
    // ... existing code ...
    
    // Set topic mappings on the mapper
    if (config.getTopicMappings() != null) {
        topicMapper.setTopicMappings(config.getTopicMappings());
        logger.info("Loaded {} topic mappings", config.getTopicMappings().size());
    }
    
    // ... rest of existing code ...
}
```

Update tag change handler to use new method (around line 319):
```java
// OLD:
String topic = topicMapper.mapTagToTopic(tagPath);

// NEW:
String topic = topicMapper.mapTagToTopicWithMappings(tagPath);
```

### 8. Active Subscriptions Endpoint (NEW)

**File:** `mqtt-gateway/src/main/java/.../web/MqttDataRoutes.java`

Add new route in `mountRoutes()`:
```java
logger.info("Mounting route: /subscriptions");
routes.newRoute("/subscriptions")
    .type(RouteGroup.TYPE_JSON)
    .handler(MqttDataRoutes::handleSubscriptions)
    .method(HttpMethod.GET)
    .accessControl(AccessControlStrategy.OPEN_ROUTE)
    .mount();
logger.info("Successfully mounted: /subscriptions");
```

Add handler method:
```java
private static Object handleSubscriptions(RequestContext req, HttpServletResponse res) {
    try {
        res.setContentType("application/json");
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        if (hook != null && hook.getTagSubscriptionManager() != null) {
            // Get active subscriptions from TagSubscriptionManager
            // This requires adding a getActiveSubscriptions() method to TagSubscriptionManager
            JsonObject data = new JsonObject();
            data.addProperty("totalTags", 0);  // TODO: implement
            data.add("activeSubscriptions", new JsonArray());  // TODO: implement
            
            response.add("data", data);
        } else {
            JsonObject data = new JsonObject();
            data.addProperty("totalTags", 0);
            data.add("activeSubscriptions", new JsonArray());
            response.add("data", data);
        }
        
        return response;
    } catch (Exception e) {
        logger.error("Error getting subscriptions", e);
        return errorJson("Error: " + e.getMessage());
    }
}
```

### 9. Frontend API

**File:** `mqtt-gateway/web-ui/src/api.ts`

Add:
```typescript
export async function getActiveSubscriptions(): Promise<ApiResponse<SubscriptionStatus>> {
    return apiFetch<SubscriptionStatus>(`${API_BASE}/subscriptions`);
}
```

## Testing Steps

1. **Build and install module**
2. **Test topic mappings UI:**
   - Add mapping: `[default]TestTags` → `enterprise/site1/area2`
   - Save configuration
   - Verify saved in database
3. **Test tag publishing with mappings:**
   - Create tag: `[default]TestTags/Temperature`
   - Change value
   - Verify MQTT message published to: `enterprise/site1/area2/temperature`
4. **Test without mappings:**
   - Remove all mappings
   - Change tag value
   - Verify default topic: `default/testtags/temperature`

## Example Topic Mapping Scenarios

### Scenario 1: ISA-95 Hierarchy
```
Mapping: [default]Site1/Area2/Line3 → enterprise/nashville/assembly/line3
Tag: [default]Site1/Area2/Line3/Robot1/Speed
Result: enterprise/nashville/assembly/line3/robot1/speed
```

### Scenario 2: Multiple Sites
```
Mappings:
  [default]Dallas → enterprise/dallas
  [default]Austin → enterprise/austin
  
Tag: [default]Dallas/Warehouse/Temperature
Result: enterprise/dallas/warehouse/temperature
```

### Scenario 3: Provider-Level Mapping
```
Mapping: [edge] → enterprise/edge-devices
Tag: [edge]Sensor123/Temperature  
Result: enterprise/edge-devices/sensor123/temperature
```

## Database Migration Note

When users upgrade to this version, the `TopicMappings` field will be automatically created with default value `[]` (empty array). No migration script needed since SimpleORM handles schema updates automatically.

## Files Modified Summary

### Created:
- `mqtt-common/.../model/TopicMapping.java` (new class)

### Modified:
- ✅ `mqtt-gateway/web-ui/src/types.ts`
- ✅ `mqtt-gateway/web-ui/src/components/TagSelection.tsx`
- ✅ `mqtt-gateway/web-ui/src/styles.css`
- ✅ `mqtt-gateway/.../records/MqttTagConfigRecord.java`
- ⏳ `mqtt-common/.../model/TagPublishConfig.java`
- ⏳ `mqtt-gateway/.../records/RecordMapper.java`
- ⏳ `mqtt-gateway/.../web/MqttDataRoutes.java`
- ⏳ `mqtt-gateway/.../MqttTopicMapper.java`
- ⏳ `mqtt-gateway/.../TagSubscriptionManager.java`
- ⏳ `mqtt-gateway/web-ui/src/api.ts`
