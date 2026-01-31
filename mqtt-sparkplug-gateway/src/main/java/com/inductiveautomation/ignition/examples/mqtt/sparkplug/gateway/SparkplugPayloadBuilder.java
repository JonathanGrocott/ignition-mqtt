package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway;

import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.eclipse.tahu.message.model.SparkplugBPayload.SparkplugBPayloadBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class SparkplugPayloadBuilder {

    private final SparkplugBPayloadEncoder encoder = new SparkplugBPayloadEncoder();

    public byte[] buildPayload(long sequence, List<Metric> metrics) {
        SparkplugBPayloadBuilder builder = new SparkplugBPayloadBuilder(sequence)
            .setTimestamp(new Date());

        if (metrics != null && !metrics.isEmpty()) {
            builder.addMetrics(metrics);
        }

        SparkplugBPayload payload = builder.createPayload();
        try {
            return encoder.getBytes(payload, false);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode Sparkplug payload", e);
        }
    }
}
