@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.spring.io/plugins-release") }
    }

    versionCatalogs {
        create("dslLibs") {
            from(files("./gradle/libs.versions.toml"))
        }
    }

}

rootProject.name = "hyperion-probes-dsl"