import com.github.jk1.license.render.CsvReportRenderer
import com.github.jk1.license.render.ReportRenderer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "2.2.3"

plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.serialization") version "1.7.20"
    id("maven-publish")
    id("com.taktik.gradle.maven-repository") version "1.0.7"
    id("com.taktik.gradle.git-version") version "2.0.13-gd2de85485"
    id("com.github.jk1.dependency-license-report") version "2.0"
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(CsvReportRenderer())
}

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

    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.9.2")
    testImplementation(group = "io.mockk", name = "mockk", version = "1.13.4")
    testImplementation(group = "io.kotest", name = "kotest-assertions-core-jvm", version = "5.5.5")
    testImplementation(group = "io.kotest", name = "kotest-runner-junit5", version = "5.5.5")
    testImplementation(group = "org.apache.commons", name="commons-rng-simple", version="1.5")
    testImplementation(group = "org.apache.commons", name="commons-rng-sampling", version="1.5")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<PublishToMavenRepository> {
    doFirst {
        println("Artifact >>> ${project.group}:${project.name}:${project.version} <<< published to Maven repository")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}
