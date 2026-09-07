group = "com.monkey.ktplus"
version = rootProject.version

repositories {
    maven {
        name = "ktplus-vendor"
        url = uri(rootProject.rootDir.resolve("build-logic/repo"))
    }
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.spongepowered.org/maven")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://maven.elmakers.com/repository")
    maven("https://jitpack.io")
}

val modulePath = path
val moduleBuildStep =
    tasks.register("moduleBuildStep") {
        group = "verification"
        description = "Builds $modulePath and prints a module success marker."
        doLast {
            logger.lifecycle("SUCCESS $modulePath")
        }
    }

pluginManager.withPlugin("java") {
    moduleBuildStep.configure {
        dependsOn(tasks.named("build"))
    }
}
