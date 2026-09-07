plugins {
    id("ktplus.base")
    id("ktplus.java")
    `java-library`
}

dependencies {
    api(project(":common"))
    compileOnly(libs.lib.jspecify)
}
