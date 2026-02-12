group = "com.inductiveautomation.ignition.examples"
version = "1.1.2"

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
