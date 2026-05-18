plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.6.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25"
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

tasks {
    test {
        useJUnitPlatform()
    }
}