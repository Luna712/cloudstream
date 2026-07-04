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
enum class AppFlavor {
    Stable,
    Prerelease,
    Debug;

    val isDebug: Boolean get() = this == Debug
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
    /* FLAVOR */
    @Volatile
    private var flavor: AppFlavor = AppFlavor.Stable

    fun setFlavor(value: AppFlavor) {
        flavor = value
    }

    fun setFlavor(value: String?) {
        flavor = AppFlavor.fromString(value)
    }

    val isDebug: Boolean get() = flavor.isDebug
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
