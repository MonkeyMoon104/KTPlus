import com.monkey.ktplus.gradle.KtJavaExtension
import com.monkey.ktplus.gradle.configureKtJava
import org.gradle.kotlin.dsl.create

plugins {
    id("ktplus.base")
    java
}

val ktJava = extensions.create<KtJavaExtension>("ktJava")
ktJava.toolchain.convention(21)
ktJava.release.convention(21)
ktJava.testRelease.convention(21)
ktJava.injectReleaseArg.convention(true)
ktJava.werror.convention(true)

configureKtJava(ktJava)
