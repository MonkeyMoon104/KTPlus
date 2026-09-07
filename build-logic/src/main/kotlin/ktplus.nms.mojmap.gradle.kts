import com.monkey.ktplus.gradle.KtPaperweightExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("ktplus.paperweight")
}

configure<KtPaperweightExtension> {
    reobfuscate.set(false)
}

dependencies {
    compileOnly(project(":nms-bridge"))
}
