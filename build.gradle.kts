plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = "com.codecontext"
version = "0.6.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin Standard Library
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // ===== CLI Framework =====
    implementation("com.github.ajalt.clikt:clikt:4.2.2")

    // ===== Code Parsing (CRITICAL!) =====
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.8")
    implementation("com.squareup:kotlinpoet:1.16.0")

    // ===== Git Analysis (CRITICAL!) =====
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")

    // ===== Graph Algorithms =====
    implementation("org.jgrapht:jgrapht-core:1.5.2")

    // ===== JSON Serialization (Better than Gson for Kotlin) =====
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // ===== Logging (IMPORTANT for debugging) =====
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // ===== HTML Generation (for reports) =====
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    // ===== Ktor Server (local API) =====
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.12")

    // ===== Testing =====
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
}

application {
    mainClass.set("com.codecontext.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.codecontext.MainKt"
    }
}
