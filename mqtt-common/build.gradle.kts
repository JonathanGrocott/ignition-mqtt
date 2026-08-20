plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:8.3.0")
    compileOnly("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    
    // Gson for JSON serialization (provided by Ignition)
    compileOnly("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
}

tasks.test {
    useJUnitPlatform()
}
