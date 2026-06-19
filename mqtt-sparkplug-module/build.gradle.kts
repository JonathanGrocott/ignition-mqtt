plugins {
    id("io.ia.sdk.modl") version "0.3.0"
}


ignitionModule {
    name.set("MQTT SparkplugB Publisher")
    fileName.set("MQTT-SparkplugB-Publisher.modl")
    id.set("com.inductiveautomation.mqtt.sparkplugb")
    moduleVersion.set(project.version.toString())
    moduleDescription.set("Publishes Ignition tags to MQTT broker using SparkplugB payloads")
    requiredIgnitionVersion.set("8.3.0")
    freeModule.set(true)

    projectScopes.putAll(
        mapOf(
            ":mqtt-common" to "G",
            ":mqtt-sparkplug-gateway" to "G"
        )
    )

    hooks.putAll(
        mapOf(
            "com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.SparkplugGatewayHook" to "G"
        )
    )

    skipModlSigning.set(true)
}

dependencies {
    modlImplementation(project(":mqtt-common"))
    modlImplementation(project(":mqtt-sparkplug-gateway"))
}

// The build creates an unsigned intermediate .modl; build.sh signs it with the IA
// module-signer using the self-signed certificate generated under certs/.

tasks.named("writeModuleXml") {
    doLast {
        val moduleXmlFile = file("build/moduleContent/module.xml")
        if (!moduleXmlFile.exists()) {
            return@doLast
        }
        val version = project.version.toString()
        val jarFiles = file("build/moduleContent")
            .listFiles { file -> file.isFile && file.name.endsWith(".jar") }
            ?.map { it.name }
            ?.filterNot { it.startsWith("logback-") || it.startsWith("slf4j-") }
            ?.sorted()
            ?: emptyList()

        val moduleXml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<modules>\n")
            append("\t<module>\n")
            append("\t\t<name>MQTT SparkplugB Publisher</name>\n")
            append("\t\t<id>com.inductiveautomation.mqtt.sparkplugb</id>\n")
            append("\t\t<version>").append(version).append("</version>\n")
            append("\t\t<description>Publishes Ignition tags to MQTT broker using SparkplugB payloads</description>\n")
            append("\t\t<requiredIgnitionVersion>8.3.0</requiredIgnitionVersion>\n")
            append("\t\t<vendor>J.Grocott</vendor>\n")
            append("\t\t<freeModule>true</freeModule>\n")
            append("\t\t<hook scope=\"G\">com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway.SparkplugGatewayHook</hook>\n")
            append("\t\t<requiredFrameworkVersion>8</requiredFrameworkVersion>\n")

            jarFiles.forEach { jarName ->
                append("\t\t<jar scope=\"G\">").append(jarName).append("</jar>\n")
            }

            append("\t</module>\n")
            append("</modules>\n")
        }

        moduleXmlFile.writeText(moduleXml)
    }
}
