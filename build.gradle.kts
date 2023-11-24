plugins {
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

group = "com.safehill"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("at.favre.lib:hkdf:2.0.0")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("dev.turingcomplete:kotlin-onetimepassword:2.4.0")
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.safehill.kcrypto.Main.kt"
    }
}