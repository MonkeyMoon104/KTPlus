package com.monkey.ktplus.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

fun Project.configureKtJava(javaExt: KtJavaExtension) {
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(javaExt.toolchain.map { JavaLanguageVersion.of(it) })
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-Xlint:all",
                "-Xlint:-processing",
                "-Xlint:-options",
                "-Xlint:-classfile",
                "-Werror",
            ),
        )
        doFirst {
            if (!javaExt.injectReleaseArg.get()) {
                return@doFirst
            }
            val releaseTarget =
                if (name == "compileTestJava") {
                    javaExt.testRelease.get()
                } else {
                    javaExt.release.get()
                }
            val args = options.compilerArgs
            val cleaned = ArrayList<String>(args.size)
            var skipNext = false
            for (arg in args) {
                if (skipNext) {
                    skipNext = false
                    continue
                }
                if (arg == "--release") {
                    skipNext = true
                    continue
                }
                cleaned.add(arg)
            }
            args.clear()
            args.addAll(listOf("--release", releaseTarget.toString()))
            args.addAll(cleaned)
            if (releaseTarget == 8 || !javaExt.werror.get()) {
                args.removeAll(listOf("-Werror"))
                args.add("-Xlint:-classfile")
                args.add("-nowarn")
            }
        }
    }

    afterEvaluate {
        tasks.withType<JavaCompile>().configureEach {
            if (javaExt.injectReleaseArg.get()) {
                options.release.unset()
            } else {
                options.release.set(javaExt.release)
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("--enable-native-access=ALL-UNNAMED", "-Xshare:off")
    }
}
