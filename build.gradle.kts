plugins {
    id("io.ia.sdk.modl") version "0.3.0"
}

group = "com.inductiveautomation.ignition.examples"
version = "1.0.0-SNAPSHOT"

ignitionModule {
    name.set("MQTT UNS Publisher")
    fileName.set("MQTT-UNS-Publisher.modl")
    id.set("com.inductiveautomation.mqtt.uns")
    moduleVersion.set(version.toString())
    moduleDescription.set("Publishes Ignition tags to MQTT broker in Unified Namespace structure with configurable JSON payloads")
    requiredIgnitionVersion.set("8.3.0")
    
    projectScopes.putAll(
        mapOf(
            ":mqtt-gateway" to "G"
        )
    )

    moduleDependencies.put("com.inductiveautomation.ignition.common", "G")
    
    hooks.putAll(
        mapOf(
            "com.inductiveautomation.ignition.examples.mqtt.gateway.MqttGatewayHook" to "G"
        )
    )
    
    skipModlSigning.set(true)
}

allprojects {
    version = rootProject.version
}

subprojects {
    apply(plugin = "java-library")
    
    repositories {
        mavenCentral()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public")
        }
    }
}
