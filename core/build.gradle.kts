import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.DuplicatesStrategy
import java.security.MessageDigest

plugins {
    id("ktplus.base")
    id("ktplus.java")
    id("ktplus.relocate")
}

fun libConfiguration(name: String) = configurations.create(name) {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val libPacketEvents = libConfiguration("libPacketEvents")
val libraryDescriptorDir = layout.buildDirectory.dir("generated/libs")

data class LibrarySpec(
    val id: String,
    val track: String,
    val keyClass: String,
    val configuration: Configuration,
)

fun mavenArtifactPath(artifact: ResolvedArtifact): String {
    val module = artifact.moduleVersion.id
    val groupPath = module.group.replace('.', '/')
    val classifier = artifact.classifier
    val fileName = if (classifier.isNullOrBlank()) {
        "${module.name}-${module.version}.jar"
    } else {
        "${module.name}-${module.version}-$classifier.jar"
    }
    return "$groupPath/${module.name}/${module.version}/$fileName"
}

fun repositoryBasesFor(group: String): List<String> {
    val ordered = linkedSetOf<String>()
    when {
        group.startsWith("com.github.retrooper") -> ordered += "https://repo.codemc.io/repository/maven-releases/"
    }
    val preferredMirror = System.getenv("KTPLUS_LIBS_MIRROR")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    if (preferredMirror != null) {
        ordered += preferredMirror.trimEnd('/')
    }
    ordered += "https://maven-central.storage-download.googleapis.com/maven2"
    ordered += "https://repo.maven.apache.org/maven2"
    ordered += "https://repo1.maven.org/maven2"
    ordered += "https://repo.papermc.io/repository/maven-public/"
    ordered += "https://repo.codemc.io/repository/maven-releases/"
    return ordered.toList()
}

fun mavenDownloadUrls(artifact: ResolvedArtifact): List<String> {
    val path = mavenArtifactPath(artifact)
    return repositoryBasesFor(artifact.moduleVersion.id.group).map { base ->
        val normalized = base.trimEnd('/')
        "$normalized/$path"
    }
}

fun sha256Hex(file: java.io.File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(16_384)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

fun writeLibraryDescriptor(spec: LibrarySpec, outputDir: java.io.File) {
    val resolved = spec.configuration.resolvedConfiguration.resolvedArtifacts
        .filter { it.type == "jar" || it.extension == "jar" }
        .filterNot { it.name.endsWith("-sources") || it.name.endsWith("-javadoc") }
        .filter { it.file.isFile && it.file.length() > 0L }
    val artifacts = resolved.sortedBy { it.file.name }
    check(artifacts.isNotEmpty()) { "Library ${spec.id} resolved zero jars" }

    val builder = StringBuilder()
    builder.append("track=").append(spec.track).append('\n')
    builder.append("key-class=").append(spec.keyClass).append('\n')
    builder.append("jar-count=").append(artifacts.size).append('\n')
    artifacts.forEachIndexed { index, artifact ->
        val file = artifact.file
        val urls = mavenDownloadUrls(artifact)
        builder.append("jar.").append(index).append(".file=").append(file.name).append('\n')
        builder.append("jar.").append(index).append(".url-count=").append(urls.size).append('\n')
        urls.forEachIndexed { urlIndex, url ->
            builder.append("jar.").append(index).append(".url.").append(urlIndex).append('=').append(url).append('\n')
        }
        builder.append("jar.").append(index).append(".sha256=").append(sha256Hex(file)).append('\n')
        builder.append("jar.").append(index).append(".size=").append(file.length()).append('\n')
    }
    outputDir.mkdirs()
    outputDir.resolve("${spec.id}.properties").writeText(builder.toString())
}

fun relocateCompileLib(
    taskName: String,
    configuration: Configuration,
    archiveName: String,
    vararg relocations: Pair<String, String>,
): TaskProvider<ShadowJar> =
    tasks.register<ShadowJar>(taskName) {
        archiveFileName.set(archiveName)
        destinationDirectory.set(layout.buildDirectory.dir("relocated-compile"))
        configurations = listOf(configuration)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        filesMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        mergeServiceFiles()
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
        exclude("META-INF/versions/**/module-info.class")
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        relocations.forEach { (from, to) -> relocate(from, to) }
    }

val relocatePacketEventsCompile = relocateCompileLib(
    "relocatePacketEventsCompile",
    libPacketEvents,
    "packetevents-relocated-compile.jar",
    "com.github.retrooper" to "com.monkey.ktplus.libs.packetevents",
    "io.github.retrooper" to "com.monkey.ktplus.libs.packetevents",
)

val generateLibraryDescriptorsTask = tasks.register("generateLibraryDescriptors") {
    inputs.files(libPacketEvents)
    outputs.dir(libraryDescriptorDir)

    doLast {
        val out = libraryDescriptorDir.get().asFile
        if (out.exists()) {
            out.listFiles()?.forEach { it.delete() }
        }
        out.mkdirs()
        writeLibraryDescriptor(
            LibrarySpec(
                "packetevents",
                "modern",
                "com.monkey.ktplus.libs.packetevents.packetevents.PacketEvents",
                libPacketEvents,
            ),
            out,
        )
    }
}

dependencies {
    implementation(project(":nms-bridge"))
    compileOnly(libs.platform.paper.api)
    compileOnly(libs.lib.jspecify)
    compileOnly(libs.lib.placeholderapi)
    compileOnly(libs.lib.inventory.framework)
    testImplementation(libs.platform.paper.api)
    implementation(libs.lib.jar.relocator)
    implementation(libs.lib.asm)
    implementation(libs.lib.asm.commons)
    implementation(libs.lib.asm.tree)
    implementation(libs.lib.asm.analysis)

    compileOnly(libs.lib.jackson.databind)
    compileOnly(libs.lib.jackson.jsr310)
    compileOnly(libs.lib.lamp.common)
    compileOnly(libs.lib.lamp.bukkit)
    compileOnly(libs.lib.configurate.yaml)
    implementation(libs.lib.bstats.bukkit)
    compileOnly(libs.lib.caffeine)
    compileOnly(libs.lib.hikari)
    compileOnly(libs.lib.slf4j.api)
    compileOnly(libs.lib.sqlite)
    compileOnly(libs.lib.mysql.connector)
    compileOnly(libs.lib.mariadb.connector)
    compileOnly(libs.lib.postgresql)
    compileOnly(libs.lib.flyway.core)
    compileOnly(libs.lib.flyway.mysql)
    compileOnly(libs.lib.flyway.database.postgresql)
    compileOnly(libs.lib.okhttp)

    add(libPacketEvents.name, libs.lib.packetevents.spigot)
    compileOnly(files(relocatePacketEventsCompile.map { it.archiveFile }))

    testImplementation(libs.test.junit.jupiter)
    testImplementation(libs.lib.sqlite)
    testImplementation(libs.lib.nbt)
    testImplementation(libs.lib.hikari)
    testImplementation(libs.lib.jackson.databind)
    testImplementation(libs.lib.flyway.core)
    testImplementation(libs.lib.flyway.mysql)
    testImplementation(libs.lib.flyway.database.postgresql)
    testImplementation(libs.lib.okhttp)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    implementation(libs.lib.nbt)
}

tasks.named("compileJava").configure {
    dependsOn(relocatePacketEventsCompile)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf(
        "version" to project.version,
        "jackson" to libs.versions.lib.jackson.get(),
        "lamp" to libs.versions.lib.lamp.get(),
        "configurate" to libs.versions.lib.configurate.get(),
        "caffeine" to libs.versions.lib.caffeine.get(),
        "hikaricp" to libs.versions.lib.hikari.get(),
        "slf4j" to libs.versions.lib.slf4j.get(),
        "sqlite" to libs.versions.lib.sqlite.get(),
        "mysql" to libs.versions.lib.mysql.get(),
        "mariadb" to libs.versions.lib.mariadb.get(),
        "postgresql" to libs.versions.lib.postgresql.get(),
        "flyway" to libs.versions.lib.flyway.get(),
        "okhttp" to libs.versions.lib.okhttp.get(),
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
    from(libraryDescriptorDir) {
        into("META-INF/ktplus/libs")
    }
    dependsOn(generateLibraryDescriptorsTask)
}
