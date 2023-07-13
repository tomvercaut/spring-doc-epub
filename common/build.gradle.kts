plugins {
    id("spring.doc.epub.kotlin-library-conventions")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":service"))

    implementation(libs.kotlin.logging.jvm)
    implementation(libs.jsoup)
    implementation(libs.apache.commons.lang3)

    testImplementation(testLibs.junit)
    testImplementation(libs.logback.core)
    testImplementation(libs.logback.classic)
}