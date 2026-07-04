package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.InternalAPI
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
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
object AppConfig : SynchronizedObject() {
    /* FLAVORS */
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
    private val strings = mutableMapOf<String, String>()

    fun setString(key: String, value: String) {
        synchronized(this) { strings[key] = value }
    }

    fun setStrings(values: Map<String, String>) {
        synchronized(this) { strings.putAll(values) }
    }

    fun getString(key: String): String? {
        synchronized(this) { return strings[key] }
    }

    fun getStringOrDefault(key: String, default: String): String {
        synchronized(this) { return strings[key] ?: default }
    }
}
