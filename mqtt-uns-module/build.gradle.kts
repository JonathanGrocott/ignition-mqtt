plugins {
    id("io.ia.sdk.modl") version "0.3.0"
}


ignitionModule {
    name.set("MQTT UNS Publisher")
    fileName.set("MQTT-UNS-Publisher.modl")
    id.set("com.inductiveautomation.mqtt.uns")
    moduleVersion.set(project.version.toString())
    moduleDescription.set("Publishes Ignition tags to MQTT broker in Unified Namespace structure with configurable JSON payloads")
    requiredIgnitionVersion.set("8.3.0")
    freeModule.set(true)

    projectScopes.putAll(
        mapOf(
            ":mqtt-common" to "G",
            ":mqtt-gateway" to "G"
        )
    )

    hooks.putAll(
        mapOf(
            "com.inductiveautomation.ignition.examples.mqtt.gateway.MqttGatewayHook" to "G"
        )
    )

    skipModlSigning.set(true)
}

dependencies {
    modlImplementation(project(":mqtt-common"))
    modlImplementation(project(":mqtt-gateway"))
}

// NOTE: Module signing with self-signed certificates is not supported by Ignition.
// Vendor names only appear for modules signed with certificates from recognized Certificate Authorities.
// For development, use unsigned modules. For production with vendor name display, obtain a proper code signing certificate.

// Add vendor information to module.xml
tasks.named("writeModuleXml") {
    doLast {
        val moduleXmlFile = file("build/moduleContent/module.xml")
        if (moduleXmlFile.exists()) {
            val content = moduleXmlFile.readText()
            val updatedContent = content.replace(
                "</requiredIgnitionVersion>",
                "</requiredIgnitionVersion>\n\t\t<vendor>J.Grocott</vendor>"
            )
            moduleXmlFile.writeText(updatedContent)
        }
    }
}
