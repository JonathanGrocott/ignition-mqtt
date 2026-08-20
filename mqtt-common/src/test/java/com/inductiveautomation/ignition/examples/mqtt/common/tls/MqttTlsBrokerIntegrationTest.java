package com.inductiveautomation.ignition.examples.mqtt.common.tls;

import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "MQTT_INTEGRATION_ENABLED", matches = "(?i)true")
class MqttTlsBrokerIntegrationTest {
    @Test
    void connectsAuthenticatesAndPublishesUsingUploadedCa() throws Exception {
        MqttBrokerConfig config = integrationConfig(
            requiredSetting("MQTT_INTEGRATION_BROKER_URL"),
            requiredSetting("MQTT_INTEGRATION_PASSWORD"),
            TlsTrustMode.UPLOADED_CA
        );

        MqttConnectOptions options = MqttConnectionOptionsFactory.build(config);
        CountDownLatch messageReceived = new CountDownLatch(1);
        AtomicReference<byte[]> receivedPayload = new AtomicReference<>();
        byte[] expectedPayload = "mqtt-tls-local-integration-test".getBytes(StandardCharsets.UTF_8);
        String topic = "integration/tls/" + UUID.randomUUID();

        try (MqttClient client = new MqttClient(
            config.getEffectiveBrokerUrl(),
            config.getClientId(),
            new MemoryPersistence()
        )) {
            client.connect(options);
            assertTrue(client.isConnected());
            client.subscribe(topic, 1, (receivedTopic, message) -> {
                receivedPayload.set(message.getPayload());
                messageReceived.countDown();
            });
            client.publish(topic, new MqttMessage(expectedPayload));

            assertTrue(messageReceived.await(10, TimeUnit.SECONDS), "Published message was not received");
            assertArrayEquals(expectedPayload, receivedPayload.get());
            client.disconnect();
        }
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {
        MqttBrokerConfig config = integrationConfig(
            requiredSetting("MQTT_INTEGRATION_BROKER_URL"),
            "incorrect-local-test-password",
            TlsTrustMode.UPLOADED_CA
        );

        assertThrows(MqttException.class, () -> connect(config));
    }

    @Test
    void rejectsBrokerCertificateWhenCaIsNotTrusted() throws Exception {
        MqttBrokerConfig config = integrationConfig(
            requiredSetting("MQTT_INTEGRATION_BROKER_URL"),
            requiredSetting("MQTT_INTEGRATION_PASSWORD"),
            TlsTrustMode.SYSTEM_DEFAULT
        );

        assertThrows(MqttException.class, () -> connect(config));
    }

    @Test
    void rejectsCertificateHostnameMismatch() throws Exception {
        String mismatchedUrl = requiredSetting("MQTT_INTEGRATION_BROKER_URL").replace("localhost", "127.0.0.1");
        MqttBrokerConfig config = integrationConfig(
            mismatchedUrl,
            requiredSetting("MQTT_INTEGRATION_PASSWORD"),
            TlsTrustMode.UPLOADED_CA
        );

        assertThrows(MqttException.class, () -> connect(config));
    }

    private static MqttBrokerConfig integrationConfig(
        String brokerUrl,
        String password,
        TlsTrustMode trustMode
    ) throws Exception {
        MqttBrokerConfig config = new MqttBrokerConfig();
        config.setBrokerUrl(brokerUrl);
        config.setClientId("mqtt-tls-integration-" + UUID.randomUUID());
        config.setUsername(requiredSetting("MQTT_INTEGRATION_USERNAME"));
        config.setPassword(password);
        config.setUseTls(true);
        config.setTlsTrustMode(trustMode);
        if (trustMode == TlsTrustMode.UPLOADED_CA) {
            config.setCaCertificatePem(Files.readString(
                Path.of(requiredSetting("MQTT_INTEGRATION_CA_CERTIFICATE_PATH")),
                StandardCharsets.UTF_8
            ));
        }
        config.setConnectionTimeout((int) Duration.ofSeconds(10).toSeconds());
        config.validate();
        return config;
    }

    private static void connect(MqttBrokerConfig config) throws Exception {
        try (MqttClient client = new MqttClient(
            config.getEffectiveBrokerUrl(),
            config.getClientId(),
            new MemoryPersistence()
        )) {
            client.connect(MqttConnectionOptionsFactory.build(config));
        }
    }

    private static String requiredSetting(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value;
    }
}
