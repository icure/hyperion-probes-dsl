pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "hyperion-probes-dsl"