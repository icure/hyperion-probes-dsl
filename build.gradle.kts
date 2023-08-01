import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "2.2.3"

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://repo.spring.io/plugins-release") }
    }
    dependencies {
        classpath("com.taktik.gradle:gradle-plugin-git-version:2.0.4")
        classpath("com.taktik.gradle:gradle-plugin-helm-repository:0.2.21-99208035f3")
    }
}

plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.serialization") version "1.7.20"
    id("maven-publish")
}

apply(plugin = "git-version")

group = "com.icure"

val gitVersion: String? by project
version = gitVersion ?: "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.micrometer:micrometer-core:1.10.5")
    implementation(group = "com.dynatrace.dynahist", name = "dynahist", version = "1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}