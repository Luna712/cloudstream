plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.buildkonfig) apply false // Universal build config
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
}

allprojects {
    plugins.forEach { println(it) }
    // https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#test_task_fails_when_no_tests_are_discovered
    tasks.withType<AbstractTestTask>().configureEach {
        failOnNoDiscoveredTests = false
    }
}
gradle.projectsEvaluated {
println("Scanning for 'archives' usage...")

allprojects.forEach { project ->
    project.configurations.matching { it.name == "archives" }.forEach { config ->
        println("Project: ${project.path} has 'archives' configuration")
        config.allDependencies.forEach { dep ->
            println("  Dependency: $dep")
        }
        config.allArtifacts.forEach { artifact ->
            println("  Artifact: ${artifact.file}")
        }
    }
}

println("Checking tasks attaching to archives...")
allprojects.forEach { project ->
    project.tasks.forEach { task ->
        task.outputs.files.files.forEach { file ->
            if (file.exists()) {
                val artifactsUsingArchives = project.configurations.matching { it.name == "archives" }
                    .flatMap { it.allArtifacts }.map { it.file }
                if (file in artifactsUsingArchives) {
                    println("Task ${task.path} outputs ${file} -> attached to 'archives'")
                }
            }
        }
    }
}
}

