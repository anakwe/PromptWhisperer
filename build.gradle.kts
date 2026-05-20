plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.6.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("intellijVersion").get())
        bundledPlugin("com.intellij.java")
    }
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/build/**")
        exclude("**/*.kts")
        // Legacy troubleshooting files contain comment-placement patterns that are not auto-correctable.
        exclude("**/TroubleshootingService.kt")
        exclude("**/TroubleshootingModeTests.kt")
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    runIde {
        // Increase sandbox IDE heap to avoid slow startup / OOM during local testing
        jvmArgs("-Xmx2g", "-Xms256m")
        // Suppress JetBrains consent dialog in sandbox
        systemProperty("idea.gdpr.accepted", "true")
    }
}
