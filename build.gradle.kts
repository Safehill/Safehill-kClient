plugins {
    kotlin("jvm") version "1.6.20"
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

application {
    mainClass.set("MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.safehill.kcrypto.Main.kt"
    }
}