pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven {
            name = "ktplus-paperweight-patch"
            url = uri("build-logic/repo")
        }
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "KTPlus"

include("common")
include("nms-bridge")
include("core")
include("dist")

include("NMS:v1_21_1")
include("NMS:v1_21_3")
include("NMS:v1_21_4")
include("NMS:v1_21_5")
include("NMS:v1_21_6")
include("NMS:v1_21_7")
include("NMS:v1_21_8")
include("NMS:v1_21_9")
include("NMS:v1_21_10")
include("NMS:v1_21_11")
include("NMS:v26_1")
include("NMS:v26_2")
