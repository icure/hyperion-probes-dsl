plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinxSerialization)
}

dependencies {
    implementation(libs.micrometer)
    implementation(libs.dynahist)
    implementation(libs.kotlinxSerialization)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotestCore)
    testImplementation(libs.kotestRunner)
    testImplementation(libs.apacheRngSimple)
    testImplementation(libs.apacheRngSampling)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}