package com.inductiveautomation.ignition.examples.mqtt.common.tls;

public enum TlsTrustMode {
    SYSTEM_DEFAULT,
    UPLOADED_CA;

    public static TlsTrustMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return SYSTEM_DEFAULT;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported TLS trust mode: " + value, e);
        }
    }
}
