import org.gradle.jvm.application.tasks.CreateStartScripts
import org.gradle.api.tasks.Sync

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = "com.vericore"
version = "0.7.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.28.2")
    implementation("com.squareup:kotlinpoet:1.16.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
    implementation("org.jgrapht:jgrapht-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.12")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
}

application {
    mainClass.set("com.vericore.MainKt")
}

tasks.test {
    useJUnitPlatform()
    workingDir(project.projectDir)
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.vericore.MainKt"
    }
}

// Temporary compatibility launcher: existing `codecontext` scripts continue to work,
// but they invoke the canonical Vericore implementation and emit a deprecation warning.
val legacyStartScripts = tasks.register<CreateStartScripts>("legacyStartScripts") {
    applicationName = "codecontext"
    mainClass.set("com.vericore.LegacyCodeContextMainKt")
    outputDir = layout.buildDirectory.dir("legacy-start-scripts").get().asFile
    classpath = files(tasks.jar, configurations.runtimeClasspath)
}

val installLegacyCli = tasks.register<Sync>("installLegacyCli") {
    from(legacyStartScripts)
    into(layout.buildDirectory.dir("install/vericore/bin"))
    dependsOn(legacyStartScripts)
}

tasks.named("installDist") {
    finalizedBy(installLegacyCli)
}
