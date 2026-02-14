package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway;

import com.inductiveautomation.ignition.examples.mqtt.common.model.ConnectionState;
import com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.inductiveautomation.ignition.examples.mqtt.common.MqttModuleConstants.*;

public class SparkplugPublisherManager {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugPublisherManager.class);

    private final AtomicReference<MqttBrokerConfig> config = new AtomicReference<>();
    private final AtomicReference<ConnectionState> connectionState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private volatile boolean slowReconnectMode = false;

    private volatile MqttClient mqttClient;
    private ScheduledExecutorService reconnectScheduler;
    private volatile boolean shouldBeConnected = false;
    private volatile String willTopic;
    private volatile byte[] willPayload;
    private volatile int willQos;
    private volatile boolean willRetained;
    private volatile SparkplugCommandListener commandListener;

    public SparkplugPublisherManager() {
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Sparkplug-Reconnect-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void connect(
        MqttBrokerConfig config,
        String willTopic,
        byte[] willPayload,
        int willQos,
        boolean willRetained
    ) {
        if (config == null) {
            logger.warn("Cannot connect: configuration is null");
            return;
        }

        try {
            config.validate();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid MQTT configuration: {}", e.getMessage());
            setConnectionState(ConnectionState.ERROR);
            return;
        }

        this.config.set(config);
        this.shouldBeConnected = true;
        this.willTopic = willTopic;
        this.willPayload = willPayload;
        this.willQos = willQos;
        this.willRetained = willRetained;

        logger.info("Initiating Sparkplug MQTT connection to {}", config.getBrokerUrl());
        doConnect(willTopic, willPayload, willQos, willRetained);
    }

    private synchronized void doConnect(String willTopic, byte[] willPayload, int willQos, boolean willRetained) {
        MqttBrokerConfig cfg = config.get();
        if (cfg == null) {
            logger.warn("Cannot connect: no configuration available");
            return;
        }

        setConnectionState(ConnectionState.CONNECTING);
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }

            mqttClient = new MqttClient(
                cfg.getBrokerUrl(),
                cfg.getClientId(),
                new MemoryPersistence()
            );

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.warn("Sparkplug MQTT connection lost for client '{}' to {}: {}",
                        cfg.getClientId(), cfg.getBrokerUrl(), cause.getMessage());
                    setConnectionState(ConnectionState.RECONNECTING);
                    scheduleReconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    SparkplugCommandListener listener = commandListener;
                    if (listener != null) {
                        listener.onCommand(topic, message.getPayload());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            MqttConnectOptions options = buildConnectOptions(cfg, willTopic, willPayload, willQos, willRetained);
            mqttClient.connect(options);
            setConnectionState(ConnectionState.CONNECTED);
            reconnectAttempts.set(0);
            slowReconnectMode = false;
            logger.info("Connected to Sparkplug MQTT broker: {} (client ID: '{}')",
                cfg.getBrokerUrl(), cfg.getClientId());

        } catch (MqttException e) {
            logger.error("Failed to connect to Sparkplug MQTT broker: {} - {}",
                cfg.getBrokerUrl(), e.getMessage(), e);
            setConnectionState(ConnectionState.RECONNECTING);
            scheduleReconnect();
        }
    }

    private MqttConnectOptions buildConnectOptions(
        MqttBrokerConfig cfg,
        String willTopic,
        byte[] willPayload,
        int willQos,
        boolean willRetained
    ) {
        MqttConnectOptions options = new MqttConnectOptions();

        if (cfg.getUsername() != null && !cfg.getUsername().isEmpty()) {
            options.setUserName(cfg.getUsername());
        }
        if (cfg.getPassword() != null && !cfg.getPassword().isEmpty()) {
            options.setPassword(cfg.getPassword().toCharArray());
        }

        options.setCleanSession(cfg.isCleanSession());
        options.setConnectionTimeout(cfg.getConnectionTimeout());
        options.setKeepAliveInterval(cfg.getKeepAlive());
        options.setAutomaticReconnect(false);

        if (willTopic != null && willPayload != null) {
            options.setWill(willTopic, willPayload, willQos, willRetained);
        }

        return options;
    }

    private void scheduleReconnect() {
        if (!shouldBeConnected) {
            return;
        }

        if (slowReconnectMode) {
            long slowDelayMs = getSlowReconnectDelayMs();
            reconnectScheduler.schedule(() -> {
                if (shouldBeConnected) {
                    doConnect(willTopic, willPayload, willQos, willRetained);
                }
            }, slowDelayMs, TimeUnit.MILLISECONDS);
            return;
        }

        int attempts = reconnectAttempts.incrementAndGet();
        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            slowReconnectMode = true;
            long slowDelayMs = getSlowReconnectDelayMs();
            reconnectScheduler.schedule(() -> {
                if (shouldBeConnected) {
                    doConnect(willTopic, willPayload, willQos, willRetained);
                }
            }, slowDelayMs, TimeUnit.MILLISECONDS);
            return;
        }

        long delay = Math.min(
            MAX_RECONNECT_DELAY,
            INITIAL_RECONNECT_DELAY * (long) Math.pow(2, attempts - 1)
        );

        reconnectScheduler.schedule(() -> {
            if (shouldBeConnected) {
                doConnect(willTopic, willPayload, willQos, willRetained);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private long getSlowReconnectDelayMs() {
        MqttBrokerConfig cfg = config.get();
        int intervalSeconds = cfg != null ? cfg.getSlowReconnectIntervalSeconds() : 0;
        if (intervalSeconds <= 0) {
            intervalSeconds = DEFAULT_SLOW_RECONNECT_INTERVAL_SECONDS;
        }
        return TimeUnit.SECONDS.toMillis(intervalSeconds);
    }

    public boolean publish(String topic, byte[] payload, int qos, boolean retained) {
        if (!isConnected()) {
            return false;
        }

        try {
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            message.setRetained(retained);
            mqttClient.publish(topic, message);
            return true;
        } catch (MqttException e) {
            logger.error("Failed to publish Sparkplug message to '{}': {}", topic, e.getMessage());
            return false;
        }
    }

    public boolean subscribe(String topic, int qos) {
        if (!isConnected()) {
            return false;
        }
        try {
            mqttClient.subscribe(topic, qos);
            return true;
        } catch (MqttException e) {
            logger.warn("Failed to subscribe to {}: {}", topic, e.getMessage());
            return false;
        }
    }

    public void setCommandListener(SparkplugCommandListener listener) {
        this.commandListener = listener;
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public ConnectionState getConnectionState() {
        return connectionState.get();
    }

    public void disconnect() {
        shouldBeConnected = false;
        slowReconnectMode = false;
        try {
            if (mqttClient != null) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            logger.warn("Error disconnecting Sparkplug MQTT client: {}", e.getMessage());
        }
        setConnectionState(ConnectionState.DISCONNECTED);
    }

    private void setConnectionState(ConnectionState state) {
        connectionState.set(state);
    }

    public interface SparkplugCommandListener {
        void onCommand(String topic, byte[] payload);
    }
}
