plugins {
    alias(dslLibs.plugins.kotlin)
    alias(dslLibs.plugins.kotlinxSerialization)
}

dependencies {
    implementation(dslLibs.bundles.ktorClient)

    implementation(dslLibs.micrometer)
    implementation(dslLibs.hdrHistogram)
    implementation(dslLibs.kotlinxSerialization)
    implementation(dslLibs.guava)

    testImplementation(dslLibs.junit)
    testImplementation(dslLibs.mockk)
    testImplementation(dslLibs.kotestCore)
    testImplementation(dslLibs.kotestRunner)
    testImplementation(dslLibs.apacheRngSimple)
    testImplementation(dslLibs.apacheRngSampling)
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
