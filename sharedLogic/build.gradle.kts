import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
}

val javaTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())

abstract class GenerateBuildConfigTask : DefaultTask() {

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
                val headContent = head.readText().trim()
                if (headContent.startsWith("ref:")) {
                    val refPath = headContent.substring(5)
                    val commitFile = File(head.parentFile, refPath)
                    if (commitFile.exists()) commitFile.readText().trim() else ""
                } else headContent
            } else ""
        } catch (_: Throwable) {
            ""
        }.take(7)

        val outFile = outputDir.file("BuildConfig.kt").get().asFile
        outFile.parentFile.mkdirs()
        
        outFile.writeText("""
            package com.lagradost.cloudstream3.shared

            object BuildConfig {
                const val GIT_HASH: String = "$hash"
                const val BUILD_DATE: Long = ${System.currentTimeMillis()}L
            }
        """.trimIndent())
    }
}

val generateBuildConfig = tasks.register<GenerateBuildConfigTask>("generateBuildConfig") {
    val gitDir = layout.projectDirectory.dir("../.git")

    headFile.set(gitDir.file("HEAD"))
    headsDir.set(gitDir.dir("refs/heads"))

    outputDir.set(layout.buildDirectory.dir("generated/buildconfig-sources"))
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

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildConfig.flatMap { it.outputDir })
            
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
