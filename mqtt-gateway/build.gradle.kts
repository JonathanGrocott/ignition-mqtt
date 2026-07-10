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
    // Depend on common module
    api(project(":mqtt-common"))
    
    // Ignition SDK dependencies
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:8.3.0")
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:8.3.0")
    
    // MQTT Client - this will be bundled with the module
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    modlImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    
    // Logging (provided by Ignition)
    compileOnly("ch.qos.logback:logback-classic:1.2.13")
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    
    // Gson for JSON (provided by Ignition)
    compileOnly("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.inductiveautomation.ignitionsdk:ignition-common:8.3.0")
}

tasks.test {
    useJUnitPlatform()
}
