plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val modlImplementation by configurations.creating

dependencies {
    api(project(":mqtt-common"))

    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:8.3.0")
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:8.3.0")

    // MQTT client for SparkplugB publishers
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    modlImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // SparkplugB payloads (Tahu)
    implementation("org.eclipse.tahu:tahu-core:1.0.14")
    modlImplementation("org.eclipse.tahu:tahu-core:1.0.14")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    modlImplementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")

    compileOnly("ch.qos.logback:logback-classic:1.2.13")
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    compileOnly("com.google.code.gson:gson:2.10.1")
}
