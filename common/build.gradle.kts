plugins {
    id("spring.doc.epub.kotlin-library-conventions")
}

dependencies {
    implementation(libs.kotlin.logging.jvm)
    testImplementation(testLibs.junit)
    testImplementation(testLibs.apache.commons.lang3)
}