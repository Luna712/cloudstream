// https://developer.android.com/build#settings-file
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")

        // https://youtrack.jetbrains.com/issue/KT-55620/
        exclusiveContent {
            forRepository {
                ivy("https://nodejs.org/dist/") {
                    name = "Node Distributions at $url"
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                    }
                    metadataSources { artifact() }
                    content { includeModule("org.nodejs", "node") }
                }
            }
            filter { includeGroup("org.nodejs") }
        }

        exclusiveContent {
            forRepository {
                ivy("https://github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn Distributions at $url"
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]).[ext]")
                    }
                    metadataSources { artifact() }
                    content { includeModule("com.yarnpkg", "yarn") }
                }
            }
            filter { includeGroup("com.yarnpkg") }
        }
    }
}

rootProject.name = "CloudStream"
include(":app", ":library", ":docs")
