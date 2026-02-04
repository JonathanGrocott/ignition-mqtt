package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.config.SparkplugConfigurationManager;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records.SparkplugBrokerConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.records.SparkplugPublishConfigRecord;
import com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.web.SparkplugDataRoutes;
import com.inductiveautomation.ignition.gateway.web.systemjs.SystemJsModule;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Gateway hook for the MQTT SparkplugB Publisher module.
 *
 * This is a placeholder implementation that will be expanded with SparkplugB
 * configuration, state management, and publishing logic.
 */
public class SparkplugGatewayHook extends AbstractGatewayModuleHook {

    private static final Logger logger = LoggerFactory.getLogger(SparkplugGatewayHook.class);
    private SparkplugConfigurationManager configManager;
    private final java.util.List<SparkplugPublisherManager> publishers = new java.util.ArrayList<>();
    private final java.util.List<SparkplugTagSubscriptionManager> subscriptions = new java.util.ArrayList<>();
    private GatewayContext gatewayContext;

    @Override
    public void setup(GatewayContext context) {
        logger.info("Setting up {} module (ID: {})", SparkplugModuleConstants.MODULE_NAME, SparkplugModuleConstants.MODULE_ID);
        this.gatewayContext = context;

        try {
            context.getSchemaUpdater().updatePersistentRecords(
                SparkplugBrokerConfigRecord.META,
                SparkplugPublishConfigRecord.META
            );
        } catch (SQLException e) {
            logger.error("Error registering Sparkplug persistent records", e);
        }

        this.configManager = new SparkplugConfigurationManager(context);
        configManager.ensureDefaultDatabaseConfig();
    }

    @Override
    public void startup(LicenseState activationState) {
        logger.info("Starting up {} module", SparkplugModuleConstants.MODULE_NAME);

        try {
            registerWebUI();
            reloadConfigurations();
        } catch (Exception e) {
            logger.error("Error starting SparkplugB module", e);
        }
    }

    public synchronized void reloadConfigurations() {
        try {
            stopPublishing();
            startPublishing();
        } catch (Exception e) {
            logger.error("Failed to reload SparkplugB configuration", e);
        }
    }

    private void startPublishing() {
        if (configManager == null) {
            return;
        }

        java.util.List<com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig> brokerConfigs =
            configManager.loadAllBrokerConfigs();

        java.util.List<com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugPublishConfig> publishConfigs =
            configManager.loadAllPublishConfigs();

        for (com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugPublishConfig publishConfig : publishConfigs) {
            try {
                if (publishConfig == null || !publishConfig.isEnabled() || publishConfig.getBrokerId() == null) {
                    continue;
                }
                com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig brokerConfig =
                    brokerConfigs.stream().filter(cfg -> publishConfig.getBrokerId().equals(cfg.getId())).findFirst().orElse(null);
                if (brokerConfig == null) {
                    logger.warn("Missing broker config for Sparkplug publish config {}", publishConfig.getName());
                    continue;
                }

                com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig sessionConfig =
                    copyBrokerConfig(brokerConfig, resolveEdgeNodeId(publishConfig));
                SparkplugPublisherManager publisher = new SparkplugPublisherManager();
                publishers.add(publisher);
                SparkplugTagSubscriptionManager subscriptionManager =
                    new SparkplugTagSubscriptionManager(gatewayContext, publisher, publishConfig, sessionConfig);
                subscriptionManager.start();
                subscriptions.add(subscriptionManager);
            } catch (Exception e) {
                logger.error("Failed to start Sparkplug publish config", e);
            }
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down {} module", SparkplugModuleConstants.MODULE_NAME);
        stopPublishing();
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }

    private void stopPublishing() {
        for (SparkplugTagSubscriptionManager subscription : subscriptions) {
            subscription.shutdown();
        }
        subscriptions.clear();
        for (SparkplugPublisherManager publisher : publishers) {
            publisher.disconnect();
        }
        publishers.clear();
    }

    @Override
    public void mountRouteHandlers(RouteGroup routes) {
        logger.info("Mounting REST API routes for MQTT SparkplugB Publisher");
        SparkplugDataRoutes.mountRoutes(routes, this);
    }

    @Override
    public java.util.Optional<String> getMountedResourceFolder() {
        return Optional.of("mounted");
    }

    @Override
    public java.util.Optional<String> getMountPathAlias() {
        return Optional.of(SparkplugModuleConstants.RESOURCE_ALIAS);
    }

    public GatewayContext getGatewayContext() {
        return gatewayContext;
    }

    private void registerWebUI() {
        try {
            String cacheBust = Long.toString(System.currentTimeMillis());
            SystemJsModule jsModule = new SystemJsModule(
                SparkplugModuleConstants.SYSTEMJS_MODULE_ID,
                "/res/" + SparkplugModuleConstants.RESOURCE_ALIAS + "/sparkplug-config.js?v=" + cacheBust
            );

            gatewayContext.getWebResourceManager()
                .getNavigationModel()
                .getConnections()
                .addCategory("mqtt-sparkplug-publisher", cat -> cat
                    .label("MQTT SparkplugB Publisher")
                    .position(110)
                    .addPage("Configuration", page -> page
                        .position(10)
                        .mount("/mqtt-sparkplug-publisher", "Configuration", jsModule)
                    )
                );
            logger.info(
                "Registered SparkplugB web UI at /res/{}/sparkplug-config.js?v={}",
                SparkplugModuleConstants.RESOURCE_ALIAS,
                cacheBust
            );
        } catch (Exception e) {
            logger.error("Error registering SparkplugB web UI", e);
        }
    }

    private com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig copyBrokerConfig(
        com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig source,
        String edgeNodeId
    ) {
        com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig copy =
            new com.inductiveautomation.ignition.examples.mqtt.common.model.MqttBrokerConfig();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setBrokerUrl(source.getBrokerUrl());
        copy.setClientId(buildClientId(source.getClientId(), edgeNodeId));
        copy.setUsername(source.getUsername());
        copy.setPassword(source.getPassword());
        copy.setUseTls(source.isUseTls());
        copy.setQos(source.getQos());
        copy.setRetained(source.isRetained());
        copy.setKeepAlive(source.getKeepAlive());
        copy.setConnectionTimeout(source.getConnectionTimeout());
        copy.setCleanSession(source.isCleanSession());
        return copy;
    }

    private String buildClientId(String baseClientId, String edgeNodeId) {
        String base = baseClientId != null && !baseClientId.isEmpty()
            ? baseClientId
            : "sparkplug";
        String suffix = edgeNodeId != null && !edgeNodeId.isEmpty()
            ? edgeNodeId
            : "edge";
        return base + "-" + suffix;
    }

    private String resolveEdgeNodeId(com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugPublishConfig config) {
        if (config.getEdgeNodeId() != null && !config.getEdgeNodeId().isEmpty()) {
            return config.getEdgeNodeId();
        }
        if (config.getDeviceMappings() != null && !config.getDeviceMappings().isEmpty()) {
            for (com.inductiveautomation.ignition.examples.mqtt.common.sparkplug.SparkplugDeviceMapping mapping : config.getDeviceMappings()) {
                if (mapping != null && mapping.getEdgeNodeId() != null && !mapping.getEdgeNodeId().isEmpty()) {
                    return mapping.getEdgeNodeId();
                }
            }
        }
        return "edge";
    }
}
