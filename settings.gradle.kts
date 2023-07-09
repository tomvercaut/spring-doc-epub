pluginManagement {
    // Include 'plugins build' to define convention plugins.
    includeBuild("build-logic")
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val log4j = version("log4j", "2.20.0")
            library("log4j.bom", "org.apache.logging.log4j",  "log4j-bom").versionRef(log4j)
            library("jsoup", "org.jsoup", "jsoup").version("1.16.1")
            library("kotlin.logging.jvm", "io.github.microutils", "kotlin-logging-jvm").version("3.0.5")
            library( "apache.commons.lang3", "org.apache.commons", "commons-lang3").version("3.12.0")
            library("jetbrains.annotations", "org.jetbrains", "annotations").version("24.0.1")
            library("lombok", "org.projectlombok", "lombok").version("1.18.28")
        }
        create("testLibs") {
            library("junit", "org.junit.jupiter", "junit-jupiter").version("5.9.1")
            library( "apache.commons.lang3", "org.apache.commons", "commons-lang3").version("3.12.0")
        }
    }
}

rootProject.name = "spring-doc-epub"
include("model", "common", "service")
include("app")
