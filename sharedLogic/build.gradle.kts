import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
}

val javaTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())

abstract class GenerateGitHashTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val headFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val headsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val head = headFile.get().asFile

        val hash = try {
            if (head.exists()) {
                // Read the commit hash from .git/HEAD
                val headContent = head.readText().trim()
                if (headContent.startsWith("ref:")) {
                    val refPath = headContent.substring(5) // e.g., refs/heads/main
                    val commitFile = File(head.parentFile, refPath)
                    if (commitFile.exists()) commitFile.readText().trim() else ""
                } else headContent // If it's a detached HEAD (commit hash directly)
            } else "" // If .git/HEAD doesn't exist
        } catch (_: Throwable) {
            "" // Just set to an empty string if any exception occurs
        }.take(7) // Get the short commit hash

        val outFile = outputDir.file("git-hash.txt").get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(hash)
    }
}

val generateGitHash = tasks.register<GenerateGitHashTask>("generateGitHash") {
    val gitDir = layout.projectDirectory.dir("../.git")

    headFile.set(gitDir.file("HEAD"))
    headsDir.set(gitDir.dir("refs/heads"))

    outputDir.set(layout.buildDirectory.dir("generated/git-resources"))
}

kotlin {
    android {
        namespace = "com.lagradost.cloudstream3.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(javaTarget)
        }
    }

    jvm()

    sourceSets {
        commonMain {
            resources.srcDir(generateGitHash.flatMap { it.outputDir })
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(project(":library"))
            }
        }

        androidMain.dependencies {
            implementation(libs.preference.ktx)
        }
    }
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        jvmTarget.set(javaTarget)
    }
}
