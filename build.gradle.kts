plugins {
    kotlin("jvm") version "2.2.0"
    id("io.github.goooler.shadow") version "8.1.8"   // ← use this Shadow fork
}

group = "hut.dev"
version = "0.1.0"

kotlin { jvmToolchain(21) }
java   { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.devs.beer/")
}

dependencies {
    // provided by server
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("dev.lone:api-itemsadder:4.0.10")

    // shaded
    implementation(kotlin("stdlib"))
    implementation("dev.triumphteam:triumph-gui:3.1.11")
}

tasks {
    jar { enabled = false }
    shadowJar {
        archiveClassifier.set("")
        // relocate Triumph GUI into your namespace
        relocate("dev.triumphteam.gui", "hut.dev.cubePowered.shaded.gui")
    }
    build { dependsOn(shadowJar) }
}