plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "com.safehill"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("at.favre.lib:hkdf:2.0.0")
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}