package com.inductiveautomation.ignition.examples.mqtt.common.tls;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MqttTlsSupport {
    public static final int MAX_CA_PEM_BYTES = 256 * 1024;
    public static final int MAX_CA_CERTIFICATES = 16;

    private MqttTlsSupport() {
    }

    public static List<X509Certificate> parseCaCertificates(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("A CA certificate is required when uploaded CA trust is selected");
        }

        byte[] pemBytes = pem.getBytes(StandardCharsets.UTF_8);
        if (pemBytes.length > MAX_CA_PEM_BYTES) {
            throw new IllegalArgumentException("CA certificate bundle exceeds the 256 KB limit");
        }

        String upper = pem.toUpperCase(Locale.ROOT);
        if (upper.contains("PRIVATE KEY")) {
            throw new IllegalArgumentException("Private keys are not accepted; upload CA certificates only");
        }

        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> parsed = certificateFactory.generateCertificates(
                new ByteArrayInputStream(pemBytes)
            );
            if (parsed.isEmpty()) {
                throw new IllegalArgumentException("No X.509 certificates were found in the uploaded file");
            }
            if (parsed.size() > MAX_CA_CERTIFICATES) {
                throw new IllegalArgumentException("CA certificate bundle contains more than 16 certificates");
            }

            List<X509Certificate> certificates = new ArrayList<>(parsed.size());
            for (Certificate certificate : parsed) {
                if (!(certificate instanceof X509Certificate x509Certificate)) {
                    throw new IllegalArgumentException("The uploaded file contains a non-X.509 certificate");
                }
                certificates.add(x509Certificate);
            }
            return certificates;
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Unable to parse the uploaded CA certificate bundle", e);
        }
    }

    public static SSLContext buildSslContext(String pem) {
        try {
            List<X509Certificate> certificates = parseCaCertificates(pem);
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            for (int index = 0; index < certificates.size(); index++) {
                trustStore.setCertificateEntry("uploaded-ca-" + index, certificates.get(index));
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to initialize TLS using the uploaded CA certificate", e);
        }
    }

    public static List<TlsCertificateInfo> describeCertificates(String pem) {
        List<TlsCertificateInfo> result = new ArrayList<>();
        Date now = new Date();
        for (X509Certificate certificate : parseCaCertificates(pem)) {
            result.add(new TlsCertificateInfo(
                certificate.getSubjectX500Principal().getName(),
                certificate.getIssuerX500Principal().getName(),
                certificate.getSerialNumber().toString(16).toUpperCase(Locale.ROOT),
                sha256Fingerprint(certificate),
                certificate.getNotBefore().toInstant().toString(),
                certificate.getNotAfter().toInstant().toString(),
                !now.before(certificate.getNotBefore()) && !now.after(certificate.getNotAfter())
            ));
        }
        return result;
    }

    private static String sha256Fingerprint(X509Certificate certificate) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            StringBuilder fingerprint = new StringBuilder(digest.length * 3 - 1);
            for (int i = 0; i < digest.length; i++) {
                if (i > 0) {
                    fingerprint.append(':');
                }
                fingerprint.append(String.format("%02X", digest[i]));
            }
            return fingerprint.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to calculate certificate fingerprint", e);
        }
    }
}
