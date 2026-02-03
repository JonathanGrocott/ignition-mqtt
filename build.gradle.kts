group = "com.inductiveautomation.ignition.examples"
version = "1.0.9"

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
