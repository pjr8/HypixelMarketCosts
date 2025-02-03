plugins {
    id("java")
    id("io.freefair.lombok") version "8.6"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.hypixel.net/repository/Hypixel/") }
    maven { url = uri("https://jitpack.io/") }
}

dependencies {
    implementation("net.hypixel:HypixelAPI:3.0.0")
    implementation("me.nullicorn:Nedit:2.2.0")
    implementation ("commons-io:commons-io:2.15.1")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}