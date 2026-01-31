package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.config.Property;
import com.inductiveautomation.ignition.common.sqltags.model.TagProp;
import com.inductiveautomation.ignition.common.tags.config.properties.WellKnownTagProps;
import com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps tag property keys to Ignition TagProp identifiers.
 */
public final class TagPropertyResolver {

    private static final Map<String, Property<?>> PROPERTY_MAP;

    static {
        Map<String, Property<?>> map = new LinkedHashMap<>();
        // Basic properties
        map.put("name", WellKnownTagProps.Name);
        map.put("tagGroup", WellKnownTagProps.TagGroup);
        map.put("enabled", WellKnownTagProps.Enabled);

        // Value properties
        map.put("tagType", WellKnownTagProps.TagType);
        map.put("typeId", WellKnownTagProps.TypeId);
        map.put("valueSource", WellKnownTagProps.ValueSource);
        map.put("dataType", WellKnownTagProps.DataType);
        map.put("defaultValue", WellKnownTagProps.DefaultValue);
        map.put("value", WellKnownTagProps.Value);
        map.put("valuePersistence", WellKnownTagProps.ValuePersistence);
        map.put("opcServer", TagProp.OPCServer);
        map.put("opcItemPath", TagProp.OPCItemPath);
        map.put("sourceTagPath", TagProp.SourceTagPath);
        map.put("executionMode", WellKnownTagProps.ExecutionMode);
        map.put("expression", TagProp.Expression);
        map.put("deriveExpressionGetter", TagProp.DeriveExpressionGetter);
        map.put("deriveExpressionSetter", TagProp.DeriveExpressionSetter);
        map.put("datasource", TagProp.SQLBindingDatasource);
        map.put("queryType", TagProp.QueryType);

        // Numeric properties
        map.put("deadband", WellKnownTagProps.Deadband);
        map.put("deadbandMode", WellKnownTagProps.DeadbandMode);
        map.put("scaleMode", WellKnownTagProps.ScaleMode);
        map.put("rawLow", WellKnownTagProps.RawLow);
        map.put("rawHigh", WellKnownTagProps.RawHigh);
        map.put("scaledLow", WellKnownTagProps.ScaledLow);
        map.put("scaledHigh", WellKnownTagProps.ScaledHigh);
        map.put("clampMode", WellKnownTagProps.ClampMode);
        map.put("scaleFactor", WellKnownTagProps.ScaleFactor);
        map.put("engUnit", WellKnownTagProps.EngUnit);
        map.put("engLow", WellKnownTagProps.EngLow);
        map.put("engHigh", WellKnownTagProps.EngHigh);
        map.put("engLimitMode", WellKnownTagProps.EngLimitMode);
        map.put("formatString", WellKnownTagProps.FormatString);

        // Meta properties
        map.put("tooltip", WellKnownTagProps.Tooltip);
        map.put("documentation", WellKnownTagProps.Documentation);

        // Alarm properties
        map.put("alarms", WellKnownTagProps.Alarms);
        map.put("alarmEvalEnabled", WellKnownTagProps.AlarmEvalEnabled);

        PROPERTY_MAP = Collections.unmodifiableMap(map);
    }

    private TagPropertyResolver() {
    }

    public static Map<String, Property<?>> getPropertyMap() {
        return PROPERTY_MAP;
    }

    public static Set<Property<?>> getSelectedTagProps(PayloadFieldConfig fields) {
        if (fields == null || fields.getProperties() == null) {
            return Collections.emptySet();
        }
        Set<Property<?>> selected = new LinkedHashSet<>();
        for (Map.Entry<String, Boolean> entry : fields.getProperties().entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                Property<?> prop = PROPERTY_MAP.get(entry.getKey());
                if (prop != null) {
                    selected.add(prop);
                }
            }
        }
        return selected;
    }
}
