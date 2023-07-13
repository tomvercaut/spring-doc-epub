plugins {
    id("spring.doc.epub.kotlin-library-conventions")
}

dependencies {
    implementation(project(":model"))
    implementation(libs.jsoup)
    implementation(libs.apache.commons.lang3)
    implementation(libs.uuid.generator)
    implementation(libs.kotlin.logging.jvm)
}