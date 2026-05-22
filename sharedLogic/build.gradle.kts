import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.buildkonfig)
}

val javaTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())

val headFileProvider = layout.projectDirectory.dir("../.git").file("HEAD")
val headContentProvider = providers.fileContents(headFileProvider).asText.map { it.trim() }

val gitHashProvider = headContentProvider.map { headContent ->
    try {
        if (headContent.startsWith("ref:")) {
            val refPath = headContent.substring(5)
            val commitFile = File(layout.projectDirectory.dir("../.git").asFile, refPath)
            if (commitFile.exists()) commitFile.readText().trim() else ""
        } else {
            headContent
        }
    } catch (_: Throwable) {
        ""
    }.take(7)
}.orElse("")

val buildDateProvider = providers.provider { System.currentTimeMillis() }

buildKonfig {
    packageName = "com.lagradost.cloudstream3.shared"
    exposeObjectWithName = "BuildConfig"
    
    defaultConfigs {
        buildConfigField("String", "GIT_HASH", gitHashProvider)
        buildConfigField("Long", "BUILD_DATE", buildDateProvider)
    }
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
