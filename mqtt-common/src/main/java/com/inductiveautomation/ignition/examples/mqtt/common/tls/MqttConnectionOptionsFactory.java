package com.inductiveautomation.ignition.examples.mqtt.common.tls;

import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.Locale;

public final class MqttConnectionOptionsFactory {
    private MqttConnectionOptionsFactory() {
    }

    public static String normalizeBrokerUrl(String brokerUrl, boolean useTls) {
        if (brokerUrl == null || brokerUrl.isBlank()) {
            throw new IllegalArgumentException("Broker URL cannot be empty");
        }

        String value = brokerUrl.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("mqtts://")) {
            return "ssl://" + value.substring("mqtts://".length());
        }
        if (lower.startsWith("mqtt://")) {
            value = "tcp://" + value.substring("mqtt://".length());
            lower = value.toLowerCase(Locale.ROOT);
        }
        if (useTls && lower.startsWith("tcp://")) {
            return "ssl://" + value.substring("tcp://".length());
        }
        if (!lower.startsWith("tcp://") && !lower.startsWith("ssl://")) {
            throw new IllegalArgumentException(
                "Broker URL must start with tcp://, ssl://, mqtt://, or mqtts://"
            );
        }
        return value;
    }

    public static boolean isTlsUrl(String brokerUrl, boolean useTls) {
        return normalizeBrokerUrl(brokerUrl, useTls).toLowerCase(Locale.ROOT).startsWith("ssl://");
    }

    public static MqttConnectOptions build(MqttBrokerConfig config) {
        MqttConnectOptions options = new MqttConnectOptions();

        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            options.setUserName(config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            options.setPassword(config.getPassword().toCharArray());
        }

        options.setCleanSession(config.isCleanSession());
        options.setConnectionTimeout(config.getConnectionTimeout());
        options.setKeepAliveInterval(config.getKeepAlive());
        options.setAutomaticReconnect(false);

        if (isTlsUrl(config.getBrokerUrl(), config.isUseTls())) {
            options.setHttpsHostnameVerificationEnabled(true);
            if (config.getTlsTrustMode() == TlsTrustMode.UPLOADED_CA) {
                options.setSocketFactory(
                    MqttTlsSupport.buildSslContext(config.getCaCertificatePem()).getSocketFactory()
                );
            }
        }

        return options;
    }
}
