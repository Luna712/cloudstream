package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.InternalAPI
import kotlin.concurrent.Volatile

// Remove and update usages of it after next stable
// It is used in inline code so we can only actually
// replace it once the code also exists on stable.
@InternalAPI
object AppDebug {
    @Volatile
    var isDebug: Boolean = false
}

@InternalAPI
enum class AppBuildType {
    Debug,
    Release;

    val isDebug: Boolean get() = this == Debug
    val isRelease: Boolean get() = this == Release

    companion object {
        fun fromString(value: String?): AppBuildType {
            if (value == null) return Release
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Release
        }
    }
}

@InternalAPI
enum class AppFlavor {
    Prerelease,
    Stable;

    val isPrerelease: Boolean get() = this == Prerelease
    val isStable: Boolean get() = this == Stable

    companion object {
        fun fromString(value: String?): AppFlavor {
            if (value == null) return Stable
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Stable
        }
    }
}

@InternalAPI
enum class AppString {
    // TODO
    AppName,
}

@InternalAPI
object AppConfig {
    
    /* BUILD TYPES */
    @Volatile
    private var buildType: AppBuildType = AppBuildType.Release

    fun setBuildType(value: AppBuildType) {
        buildType = value
    }

    fun setBuildType(value: String?) {
        buildType = AppBuildType.fromString(value)
    }

    val isDebug: Boolean get() = buildType.isDebug
    val isRelease: Boolean get() = buildType.isRelease

    /* FLAVORS */
    @Volatile
    private var flavor: AppFlavor = AppFlavor.Stable

    fun setFlavor(value: AppFlavor) {
        flavor = value
    }

    fun setFlavor(value: String?) {
        flavor = AppFlavor.fromString(value)
    }

    val isPrerelease: Boolean get() = flavor.isPrerelease
    val isStable: Boolean get() = flavor.isStable

    /* STRINGS */
    @Volatile
    private var strings: Map<AppString, String> = emptyMap()

    fun setStrings(block: MutableMap<AppString, String>.() -> Unit) {
        strings = buildMap(block)
    }

    fun getString(key: AppString): String? {
        return strings[key]
    }

    fun getStringOrDefault(key: AppString, default: String): String {
        return strings[key] ?: default
    }
}
