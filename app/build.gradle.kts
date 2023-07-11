plugins {
    id("spring.doc.epub.kotlin-application-conventions")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(testLibs.junit)

    implementation(project(":model"))
    implementation(project(":common"))
    implementation(project(":service"))

    implementation(libs.kotlin.logging.jvm)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)

    implementation(libs.apache.commons.lang3)
    implementation(libs.jetbrains.annotations)
    implementation(libs.jsoup)

}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

application {
    // Define the main class for the application.
    mainClass.set("tv.spring.doc.epub.AppKt")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
