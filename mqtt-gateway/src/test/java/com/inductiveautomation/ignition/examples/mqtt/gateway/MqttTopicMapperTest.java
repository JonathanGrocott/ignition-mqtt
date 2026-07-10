package com.inductiveautomation.ignition.examples.mqtt.gateway;

import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.examples.mqtt.common.model.PayloadFieldConfig;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TopicMapping;
import com.inductiveautomation.ignition.examples.mqtt.common.model.TopicPublishMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MqttTopicMapperTest {

    private final MqttTopicMapper mapper = new MqttTopicMapper();

    @Test
    void findsEveryMatchingRouteForSameSourceAndBroker() {
        TopicMapping fortyTwenty = mapping(
            "route-40-20",
            "[default]enterprise/renton/hbl",
            "enterprise/renton_40_20/hbl",
            1L
        );
        TopicMapping fourTwenty = mapping(
            "route-04-20",
            "[default]enterprise/renton/hbl",
            "enterprise/renton_04_20/hbl",
            1L
        );

        mapper.setTopicMappings(List.of(fortyTwenty, fourTwenty));

        List<TopicMapping> matches = mapper.findMatchingMappings("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        assertEquals(List.of(fortyTwenty, fourTwenty), matches);
    }

    @Test
    void findsEveryMatchingRouteForSameSourceAcrossBrokers() {
        TopicMapping brokerOne = mapping("broker-1", "[default]enterprise/renton/hbl", "enterprise/renton/hbl", 1L);
        TopicMapping brokerTwo = mapping("broker-2", "[default]enterprise/renton/hbl", "enterprise/renton/hbl", 2L);

        mapper.setTopicMappings(List.of(brokerOne, brokerTwo));

        List<TopicMapping> matches = mapper.findMatchingMappings("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        assertEquals(List.of(brokerOne, brokerTwo), matches);
    }

    @Test
    void returnsMoreSpecificRoutesBeforeBroadRoutes() {
        TopicMapping broad = mapping("enterprise", "[default]enterprise", "enterprise", 1L);
        TopicMapping specific = mapping("hbl", "[default]enterprise/renton/hbl", "enterprise/renton_40_20/hbl", 1L);

        mapper.setTopicMappings(List.of(broad, specific));

        List<TopicMapping> matches = mapper.findMatchingMappings("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        assertEquals(List.of(specific, broad), matches);
        assertSame(specific, mapper.findBestMapping("[default]enterprise/renton/hbl/Line1/Asset1/Temp"));
    }

    @Test
    void preservesConfigurationOrderForEquallySpecificRoutes() {
        TopicMapping first = mapping("first", "[default]enterprise/renton/hbl", "enterprise/first/hbl", 1L);
        TopicMapping second = mapping("second", "[default]enterprise/renton/hbl", "enterprise/second/hbl", 1L);
        TopicMapping third = mapping("third", "[default]enterprise/renton/hbl", "enterprise/third/hbl", 1L);

        mapper.setTopicMappings(List.of(first, second, third));

        List<TopicMapping> matches = mapper.findMatchingMappings("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        assertEquals(List.of(first, second, third), matches);
    }

    @Test
    void excludesDisabledRoutes() {
        TopicMapping enabled = mapping("enabled", "[default]enterprise/renton/hbl", "enterprise/enabled/hbl", 1L);
        TopicMapping disabled = mapping("disabled", "[default]enterprise/renton/hbl", "enterprise/disabled/hbl", 1L);
        disabled.setEnabled(false);

        mapper.setTopicMappings(List.of(disabled, enabled));

        List<TopicMapping> matches = mapper.findMatchingMappings("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        assertEquals(List.of(enabled), matches);
    }

    @Test
    void collapsesExactDuplicateRouteDefinitions() {
        TopicMapping first = mapping("first-id", "[default]enterprise/renton/hbl", "enterprise/renton/hbl", 1L);
        TopicMapping duplicate = mapping("second-id", "[default]enterprise/renton/hbl", "enterprise/renton/hbl", 1L);

        mapper.setTopicMappings(List.of(first, duplicate));

        List<TopicMapping> matches = mapper.findMatchingMappings("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        assertEquals(List.of(first), matches);
    }

    @Test
    void collapsesExactDuplicateRoutesWithEquivalentCustomPayloadFields() {
        TopicMapping first = mapping("first-id", "[default]enterprise/renton/hbl", "enterprise/renton/hbl", 1L);
        first.setUseDefaultPayloadFields(false);
        first.setPayloadFields(payloadFields(false, true));
        TopicMapping duplicate = mapping("second-id", "[default]enterprise/renton/hbl", "enterprise/renton/hbl", 1L);
        duplicate.setUseDefaultPayloadFields(false);
        duplicate.setPayloadFields(payloadFields(false, true));

        mapper.setTopicMappings(List.of(first, duplicate));

        List<TopicMapping> matches = mapper.findMatchingMappings("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        assertEquals(List.of(first), matches);
    }

    @Test
    void mapsFanOutRoutesToIndependentTopics() throws Exception {
        TopicMapping fortyTwenty = mapping(
            "route-40-20",
            "[default]enterprise/renton/hbl",
            "enterprise/renton_40_20/hbl",
            1L
        );
        TopicMapping fourTwenty = mapping(
            "route-04-20",
            "[default]enterprise/renton/hbl",
            "enterprise/renton_04_20/hbl",
            1L
        );
        TagPath tagPath = TagPathParser.parse("[default]enterprise/renton/hbl/Line 1/Asset 1/Temp");

        assertEquals(
            "enterprise/renton_40_20/hbl/line_1/asset_1/temp",
            mapper.mapTagToTopicWithMapping(tagPath, fortyTwenty)
        );
        assertEquals(
            "enterprise/renton_04_20/hbl/line_1/asset_1/temp",
            mapper.mapTagToTopicWithMapping(tagPath, fourTwenty)
        );
    }

    @Test
    void singleTopicRoutesStillPublishToTheirConfiguredTopic() throws Exception {
        TopicMapping route = mapping("single", "[default]enterprise/renton/hbl", "enterprise/renton_40_20/hbl", 1L);
        route.setPublishMode(TopicPublishMode.SINGLE_TOPIC);
        TagPath tagPath = TagPathParser.parse("[default]enterprise/renton/hbl/Line 1/Asset 1/Temp");

        assertEquals("enterprise/renton_40_20/hbl", mapper.mapTagToTopicWithMapping(tagPath, route));
    }

    @Test
    void effectiveRouteKeySeparatesDifferentFanOutTopics() throws Exception {
        TopicMapping fortyTwenty = mapping(
            "route-40-20",
            "[default]enterprise/renton/hbl",
            "enterprise/renton_40_20/hbl",
            1L
        );
        TopicMapping fourTwenty = mapping(
            "route-04-20",
            "[default]enterprise/renton/hbl",
            "enterprise/renton_04_20/hbl",
            1L
        );
        TagPath tagPath = TagPathParser.parse("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        String fortyTwentyTopic = mapper.mapTagToTopicWithMapping(tagPath, fortyTwenty);
        String fourTwentyTopic = mapper.mapTagToTopicWithMapping(tagPath, fourTwenty);

        assertNotEquals(
            mapper.buildEffectiveRouteKey(1L, fortyTwentyTopic, fortyTwenty),
            mapper.buildEffectiveRouteKey(1L, fourTwentyTopic, fourTwenty)
        );
    }

    @Test
    void effectiveRouteKeyCollapsesBroadAndSpecificMappingsThatResolveToSameTopic() throws Exception {
        TopicMapping broad = mapping("enterprise", "[default]enterprise", "enterprise", 1L);
        TopicMapping specific = mapping("hbl", "[default]enterprise/renton/hbl", "enterprise/renton/hbl", 1L);
        TagPath tagPath = TagPathParser.parse("[default]enterprise/renton/hbl/Line1/Asset1/Temp");

        String broadTopic = mapper.mapTagToTopicWithMapping(tagPath, broad);
        String specificTopic = mapper.mapTagToTopicWithMapping(tagPath, specific);

        assertEquals("enterprise/renton/hbl/line1/asset1/temp", broadTopic);
        assertEquals(broadTopic, specificTopic);
        assertEquals(
            mapper.buildEffectiveRouteKey(1L, broadTopic, broad),
            mapper.buildEffectiveRouteKey(1L, specificTopic, specific)
        );
    }

    private TopicMapping mapping(String id, String sourcePattern, String topicPrefix, Long brokerId) {
        return new TopicMapping(id, sourcePattern, topicPrefix, true, brokerId);
    }

    private PayloadFieldConfig payloadFields(boolean includeQuality, boolean includeDataType) {
        PayloadFieldConfig fields = new PayloadFieldConfig();
        fields.setIncludeQuality(includeQuality);
        fields.getProperties().put("dataType", includeDataType);
        return fields;
    }
}
