package com.monkey.ktplus.gradle

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.getByType

abstract class KtPaperweightExtension
    @Inject
    constructor(
        private val project: Project,
    ) {
        abstract val paperBundle: Property<String>
        abstract val reobfuscate: Property<Boolean>
        abstract val launcherVersion: Property<Int>

        fun catalog(versionKey: String) {
            paperBundle.set(resolvePaperBundleVersion(versionKey))
        }

        private fun resolvePaperBundleVersion(versionKey: String): String =
            project.extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")
                .findVersion(versionKey)
                .orElseThrow { IllegalArgumentException("Missing version catalog key '$versionKey'") }
                .requiredVersion

        companion object {
            fun paperBundleVersionKey(projectName: String): String =
                "platform-paper-${projectName.replace('_', '-')}"

            fun defaultPaperBundleVersion(project: Project): String {
                val versionKey = paperBundleVersionKey(project.name)
                return project.extensions
                    .getByType<VersionCatalogsExtension>()
                    .named("libs")
                    .findVersion(versionKey)
                    .orElseThrow { IllegalArgumentException("Missing platform paper version '$versionKey'") }
                    .requiredVersion
            }
        }
    }
