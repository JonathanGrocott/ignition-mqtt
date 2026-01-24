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
    
    // Gson for JSON serialization (provided by Ignition)
    compileOnly("com.google.code.gson:gson:2.10.1")
}
