plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.safehill"
version = "0.1"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.hkdf)
    implementation(libs.bcprov.jdk15on)


    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.onetimepassword)

    api(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.content.negotiation)

    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation(libs.mockito.core)
    testImplementation(libs.junit.jupiter)
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