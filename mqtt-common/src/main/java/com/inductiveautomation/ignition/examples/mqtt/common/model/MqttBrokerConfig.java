package com.inductiveautomation.ignition.examples.mqtt.common.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.UUID;

import static com.inductiveautomation.ignition.examples.mqtt.common.MqttModuleConstants.*;

/**
 * Configuration for MQTT broker connection settings.
 * This class holds all the necessary parameters to connect to an MQTT broker.
 */
public class MqttBrokerConfig {
    
    @SerializedName("id")
    private Long id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("brokerUrl")
    private String brokerUrl;
    
    @SerializedName("clientId")
    private String clientId;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("password")
    private String password;  // Will be encrypted when stored
    
    @SerializedName("useTls")
    private boolean useTls;
    
    @SerializedName("qos")
    private int qos;
    
    @SerializedName("retained")
    private boolean retained;
    
    @SerializedName("keepAlive")
    private int keepAlive;
    
    @SerializedName("connectionTimeout")
    private int connectionTimeout;
    
    @SerializedName("cleanSession")
    private boolean cleanSession;
    
    /**
     * Default constructor with sensible defaults
     */
    public MqttBrokerConfig() {
        this.id = null;
        this.name = "MQTT Broker";
        this.brokerUrl = DEFAULT_BROKER_URL;
        this.clientId = DEFAULT_CLIENT_ID_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        this.username = "";
        this.password = "";
        this.useTls = false;
        this.qos = DEFAULT_QOS;
        this.retained = false;
        this.keepAlive = DEFAULT_KEEP_ALIVE;
        this.connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        this.cleanSession = true;
    }
    
    /**
     * Constructor with all parameters
     */
    public MqttBrokerConfig(String brokerUrl, String clientId, String username, String password,
                           boolean useTls, int qos, boolean retained, int keepAlive,
                           int connectionTimeout, boolean cleanSession) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.useTls = useTls;
        this.qos = qos;
        this.retained = retained;
        this.keepAlive = keepAlive;
        this.connectionTimeout = connectionTimeout;
        this.cleanSession = cleanSession;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBrokerUrl() {
        return brokerUrl;
    }
    
    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isUseTls() {
        return useTls;
    }
    
    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }
    
    public int getQos() {
        return qos;
    }
    
    public void setQos(int qos) {
        if (qos < 0 || qos > 2) {
            throw new IllegalArgumentException("QoS must be 0, 1, or 2");
        }
        this.qos = qos;
    }
    
    public boolean isRetained() {
        return retained;
    }
    
    public void setRetained(boolean retained) {
        this.retained = retained;
    }
    
    public int getKeepAlive() {
        return keepAlive;
    }
    
    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public boolean isCleanSession() {
        return cleanSession;
    }
    
    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }
    
    /**
     * Validates the configuration
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (brokerUrl == null || brokerUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Broker URL cannot be empty");
        }
        if (!brokerUrl.startsWith("tcp://") && !brokerUrl.startsWith("ssl://")) {
            throw new IllegalArgumentException("Broker URL must start with tcp:// or ssl://");
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be empty");
        }
        if (qos < 0 || qos > 2) {
            throw new IllegalArgumentException("QoS must be 0, 1, or 2");
        }
        if (keepAlive < 0) {
            throw new IllegalArgumentException("Keep alive must be >= 0");
        }
        if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Connection timeout must be >= 0");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MqttBrokerConfig that = (MqttBrokerConfig) o;
        return useTls == that.useTls &&
               qos == that.qos &&
               retained == that.retained &&
               keepAlive == that.keepAlive &&
               connectionTimeout == that.connectionTimeout &&
               cleanSession == that.cleanSession &&
               Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               Objects.equals(brokerUrl, that.brokerUrl) &&
               Objects.equals(clientId, that.clientId) &&
               Objects.equals(username, that.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, brokerUrl, clientId, username, useTls, qos, retained, 
                          keepAlive, connectionTimeout, cleanSession);
    }
    
    @Override
    public String toString() {
        return "MqttBrokerConfig{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", brokerUrl='" + brokerUrl + '\'' +
               ", clientId='" + clientId + '\'' +
               ", username='" + username + '\'' +
               ", useTls=" + useTls +
               ", qos=" + qos +
               ", retained=" + retained +
               ", keepAlive=" + keepAlive +
               ", connectionTimeout=" + connectionTimeout +
               ", cleanSession=" + cleanSession +
               '}';
    }
    
    /**
     * Creates a copy of this configuration
     */
    public MqttBrokerConfig copy() {
        MqttBrokerConfig copy = new MqttBrokerConfig();
        copy.id = this.id;
        copy.name = this.name;
        copy.brokerUrl = this.brokerUrl;
        copy.clientId = this.clientId;
        copy.username = this.username;
        copy.password = this.password;
        copy.useTls = this.useTls;
        copy.qos = this.qos;
        copy.retained = this.retained;
        copy.keepAlive = this.keepAlive;
        copy.connectionTimeout = this.connectionTimeout;
        copy.cleanSession = this.cleanSession;
        return copy;
    }
}
