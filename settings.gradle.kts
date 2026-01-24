pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public")
        }
    }
}

rootProject.name = "mqtt-module"

include(
    ":mqtt-common",
    ":mqtt-gateway"
)
