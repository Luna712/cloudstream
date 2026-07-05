plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
}

kotlin {
    android {
        // Must be unique
        namespace = "com.lagradost.cloudstream4"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        lint {
            checkTestSources = true
            checkDependencies = true
        }
    }

    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        all {
            languageSettings {
                optIn("com.lagradost.cloudstream3.InternalAPI")
                optIn("com.lagradost.cloudstream3.Prerelease")
            }
        }

        commonMain.dependencies {
            implementation(libs.coil.compose)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(project(":library"))
        }

        androidMain.dependencies {
            implementation(libs.activity.compose)
            implementation(libs.preference.ktx)
        }

        commonTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(libs.kotlin.test)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlin.reflect)
            implementation(libs.kotlin.test)
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    source.setFrom(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/jvmMain/kotlin",
        "src/commonTest/kotlin",
        "src/jvmTest/kotlin",
    )
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)

    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.compose.rules.detekt)
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.lagradost.cloudstream4.generated.resources"
    generateResClass = auto
}
