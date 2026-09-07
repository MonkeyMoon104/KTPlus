import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URLClassLoader
import java.util.zip.ZipFile
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.file.DuplicatesStrategy

plugins {
    id("ktplus.base")
    java
    alias(libs.plugins.gradle.shadow)
}

configurations.matching { it.isCanBeResolved }.configureEach {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

tasks.withType<ShadowJar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

val reobfNmsProjects = listOf(
    ":NMS:v1_21_1",
    ":NMS:v1_21_3",
    ":NMS:v1_21_4",
    ":NMS:v1_21_5",
    ":NMS:v1_21_6",
    ":NMS:v1_21_7",
    ":NMS:v1_21_8",
    ":NMS:v1_21_9",
    ":NMS:v1_21_10",
    ":NMS:v1_21_11",
)

val runtimeNmsProjects = listOf(
    ":NMS:v26_1",
    ":NMS:v26_2",
)

val pluginJarFile = layout.buildDirectory.file("libs/KTPlus-${project.version}.jar")

dependencies {
    implementation(project(":core"))
    implementation(libs.lib.inventory.framework)

    reobfNmsProjects.forEach { path ->
        runtimeOnly(project(path, configuration = "reobf")) {
            isTransitive = false
        }
    }
    runtimeNmsProjects.forEach { path ->
        runtimeOnly(project(path, configuration = "runtimeElements")) {
            isTransitive = false
        }
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("KTPlus-${project.version}.jar")
    configurations = listOf(project.configurations.runtimeClasspath.get())
    mergeServiceFiles()
    relocate("me.lucko.jarrelocator", "com.monkey.ktplus.libs.relocator")
    relocate("org.objectweb.asm", "com.monkey.ktplus.libs.asm")
    relocate("org.bstats", "com.monkey.ktplus.libs.bstats")
    relocate("com.github.stefvanschie.inventoryframework", "com.monkey.ktplus.libs.inventoryframework")
    relocate("net.querz", "com.monkey.ktplus.libs.nbt")
}

val verifyPluginJarTask = tasks.register("verifyPluginJar") {
    group = "verification"
    description = "Checks relocator shade and library descriptors in KTPlus.jar"
    dependsOn(tasks.shadowJar)

    doLast {
        val pluginJar = pluginJarFile.get().asFile
        check(pluginJar.isFile) {
            "Plugin jar not found: ${pluginJar.absolutePath}"
        }

        ZipFile(pluginJar).use { zip ->
            val requiredDescriptors = listOf(
                "META-INF/ktplus/libs/packetevents.properties",
            )
            requiredDescriptors.forEach { path ->
                check(zip.getEntry(path) != null) { "Missing library descriptor in jar: $path" }
            }
            val unexpectedDescriptors = listOf(
                "META-INF/ktplus/libs/jackson.properties",
                "META-INF/ktplus/libs/lamp.properties",
                "META-INF/ktplus/libs/hikaricp.properties",
                "META-INF/ktplus/libs/bstats.properties",
            )
            unexpectedDescriptors.forEach { path ->
                check(zip.getEntry(path) == null) {
                    "Maven Central lib should use plugin.yml libraries, not LibraryLoader descriptor: $path"
                }
            }
            val hasRelocator = zip.entries().asSequence().any {
                it.name.startsWith("com/monkey/ktplus/libs/relocator/")
            }
            check(hasRelocator) { "Plugin jar must shade jar-relocator under libs.relocator" }
            val leaked = zip.entries().asSequence()
                .map { it.name }
                .filter { name ->
                    name.startsWith("com/github/stefvanschie/inventoryframework/") ||
                        name.startsWith("me/lucko/jarrelocator/") ||
                        name.startsWith("org/bstats/") ||
                        name.startsWith("com/monkey/ktplus/libs/packetevents/") ||
                        name.startsWith("net/querz/")
                }
                .take(5)
                .toList()
            check(leaked.isEmpty()) {
                "Plugin jar still contains non-relocated third-party library classes, e.g. $leaked"
            }
            val hasInventoryFramework = zip.entries().asSequence().any {
                it.name.startsWith("com/monkey/ktplus/libs/inventoryframework/")
            }
            check(hasInventoryFramework) {
                "Plugin jar must shade IF under com.monkey.ktplus.libs.inventoryframework"
            }
            val hasBstats = zip.entries().asSequence().any {
                it.name.startsWith("com/monkey/ktplus/libs/bstats/")
            }
            check(hasBstats) { "Plugin jar must shade bStats under com.monkey.ktplus.libs.bstats" }
            zip.getInputStream(zip.getEntry("plugin.yml")).use { input ->
                val pluginYml = input.readBytes().decodeToString()
                check(pluginYml.contains("libraries:")) { "plugin.yml must declare Spigot libraries" }
                check(pluginYml.contains("com.zaxxer:HikariCP:")) { "plugin.yml missing HikariCP library" }
                check(!pluginYml.contains("org.bstats:bstats-bukkit:")) {
                    "bStats must be shaded, not listed in plugin.yml libraries"
                }
                check(!pluginYml.contains("\${")) { "plugin.yml still contains unresolved placeholders" }
            }
        }

        URLClassLoader(arrayOf(pluginJar.toURI().toURL()), ClassLoader.getPlatformClassLoader()).use { loader ->
            loader.loadClass("com.monkey.ktplus.lib.LibraryLoader")
            loader.loadClass("com.monkey.ktplus.lib.RuntimeLibraryBootstrap")
        }
    }
}

tasks.shadowJar {
    finalizedBy(verifyPluginJarTask)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
