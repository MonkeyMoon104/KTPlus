plugins {
    `kotlin-dsl`
}

group = "com.monkey.ktplus.build-logic"

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(libs.gradle.paperweight.userdev)
    implementation(libs.gradle.shadow)
}

kotlin {
    jvmToolchain(21)
}
