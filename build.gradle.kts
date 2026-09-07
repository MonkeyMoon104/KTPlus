plugins {
    base
    alias(libs.plugins.gradle.shadow) apply false
}

group = "com.monkey.ktplus"
version = providers.gradleProperty("version").get()

tasks.build {
    dependsOn(":dist:build")
}

tasks.register("verifyPluginJar") {
    group = "verification"
    description = "Verifies the shaded KTPlus plugin jar produced by :dist."
    dependsOn(":dist:verifyPluginJar")
}

tasks.register("test") {
    group = "verification"
    description = "Runs :core tests."
    dependsOn(":core:test")
}

gradle.projectsEvaluated {
    val moduleBuildSteps = tasks.register("moduleBuildSteps") {
        group = "verification"
        description = "Runs module build steps for all KTPlus modules."
    }
    subprojects.forEach { sub ->
        sub.tasks.findByName("moduleBuildStep")?.let { step ->
            moduleBuildSteps.configure {
                dependsOn(step)
            }
        }
    }
}
