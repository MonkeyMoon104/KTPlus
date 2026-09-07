plugins {
    `kotlin-dsl`
}

group = "com.monkey.ktplus.build-logic"

repositories {
    maven {
        name = "ktplus-paperweight-patch"
        url = uri(layout.projectDirectory.dir("repo"))
    }
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
