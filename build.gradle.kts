plugins {
    java
}

group = "com.sl"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Hytale Server API (compile only - provided at runtime)
    compileOnly(fileTree("libs") { include("*.jar") })
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.jar {
    // Set JAR name
    archiveBaseName.set("SLParty")
    archiveVersion.set(version.toString())
}

tasks.test {
    useJUnitPlatform()
}
