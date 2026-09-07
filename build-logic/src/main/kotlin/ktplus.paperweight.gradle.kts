import com.monkey.ktplus.gradle.KtPaperweightExtension
import io.papermc.paperweight.tasks.JavaLauncherTask
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

plugins {
    id("ktplus.java")
    id("io.papermc.paperweight.userdev")
}

val ktPaperweight = extensions.create<KtPaperweightExtension>("ktPaperweight")
ktPaperweight.reobfuscate.convention(true)
ktPaperweight.launcherVersion.convention(21)
ktPaperweight.paperBundle.convention(
    provider { KtPaperweightExtension.defaultPaperBundleVersion(project) },
)

paperweight {
    reobfArtifactConfiguration = ReobfArtifactConfiguration.REOBF_PRODUCTION
}

dependencies {
    paperweight.paperDevBundle(ktPaperweight.paperBundle)
}

val toolchains = extensions.getByType<JavaToolchainService>()

tasks.withType<JavaLauncherTask>().configureEach {
    launcher.set(
        toolchains.launcherFor {
            languageVersion.set(ktPaperweight.launcherVersion.map { JavaLanguageVersion.of(it) })
        },
    )
}

afterEvaluate {
    if (!ktPaperweight.reobfuscate.get()) {
        tasks.named("reobfJar") {
            enabled = false
        }
    }
}
