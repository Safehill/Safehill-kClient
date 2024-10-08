plugins {
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

group = "com.safehill"
version = "0.1"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("at.favre.lib:hkdf:2.0.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-kotlinx-serialization:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("dev.turingcomplete:kotlin-onetimepassword:2.4.0")

    val ktor_version = "2.3.11"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")

    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:4.8.1")
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.safehill.kclient.Main.kt"
    }
}

java {
    withSourcesJar()
}