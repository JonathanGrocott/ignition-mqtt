package com.inductiveautomation.ignition.examples.mqtt.common.tls;

public class TlsCertificateInfo {
    private final String subject;
    private final String issuer;
    private final String serialNumber;
    private final String sha256Fingerprint;
    private final String notBefore;
    private final String notAfter;
    private final boolean currentlyValid;

    public TlsCertificateInfo(
        String subject,
        String issuer,
        String serialNumber,
        String sha256Fingerprint,
        String notBefore,
        String notAfter,
        boolean currentlyValid
    ) {
        this.subject = subject;
        this.issuer = issuer;
        this.serialNumber = serialNumber;
        this.sha256Fingerprint = sha256Fingerprint;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.currentlyValid = currentlyValid;
    }

    public String getSubject() {
        return subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSha256Fingerprint() {
        return sha256Fingerprint;
    }

    public String getNotBefore() {
        return notBefore;
    }

    public String getNotAfter() {
        return notAfter;
    }

    public boolean isCurrentlyValid() {
        return currentlyValid;
    }
}
