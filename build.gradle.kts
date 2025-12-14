plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = "com.codecontext"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin Standard Library
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    
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
    
    // ===== Testing =====
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
}

// ===== Application Configuration =====
application {
    mainClass.set("com.codecontext.MainKt")
}

// ===== Test Configuration =====
tasks.test {
    useJUnitPlatform()
}

// ===== Kotlin Configuration =====
kotlin {
    jvmToolchain(21)
}

// ===== Executable JAR Configuration =====
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.codecontext.MainKt"
    }
}
