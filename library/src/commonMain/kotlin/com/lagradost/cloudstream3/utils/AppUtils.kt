package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream3.json
import com.lagradost.cloudstream3.mvvm.debugPrint
import com.lagradost.cloudstream3.mvvm.logError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
object AppUtils {
    /** Any object as a JSON string */
    fun Any.toJson(): String {
        if (this is String) return this
        return toJsonLiteral()
    }

    @InternalAPI
    fun Any.toJsonLiteralImpl(serializer: KSerializer<Any>?): String {
        if (serializer == null) {
            val e = Exception("No serializer found for ${this::class.simpleName}")
            logError(e)
            throw e
        }
        debugPrint { "AppUtils/toJsonLiteral: using kotlinx serialization for ${this::class.simpleName}" }
        return json.encodeToString(serializer, this)
    }

    /** Runtime lookup version, subject to type erasure for generic types. */
    @InternalAPI
    fun Any.toJsonLiteral(): String {
        val serializer = this::class.serializerOrNull()
            ?: json.serializersModule.getContextual(this::class)
        @Suppress("UNCHECKED_CAST")
        return toJsonLiteralImpl(serializer as KSerializer<Any>?)
    }

    /** Reified version, preserves full generic type info at call site. */
    @InternalAPI
    @JvmName("toJsonLiteralReified")
    inline fun <reified T : Any> T.toJsonLiteral(): String {
        val serializer = runCatching { serializer<T>() }
            .recoverCatching { json.serializersModule.getContextual(T::class) }
            .getOrNull()
        @Suppress("UNCHECKED_CAST")
        return toJsonLiteralImpl(serializer as KSerializer<Any>?)
    }

    @InternalAPI
    fun <T : Any> parseJson(value: String, kClass: KClass<T>): T {
        val serializer = kClass.serializerOrNull() ?: json.serializersModule.getContextual(kClass)
        if (serializer == null) {
            val e = Exception("No serializer found for ${kClass.simpleName}")
            logError(e)
            throw e
        }
        debugPrint { "AppUtils/parseJson(kClass): using kotlinx serialization for ${kClass.simpleName}" }
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromString(serializer, value) as T
    }

    // This is inlined code and can easily cause breakage in extensions!
    // Watch out when editing this to make sure stable also supports all inlined code!
    inline fun <reified T : Any> parseJson(value: String): T {
        val serializer = runCatching { serializer<T>() }
            .recoverCatching { json.serializersModule.getContextual(T::class) }
            .getOrNull()

        if (serializer == null) {
            val e = Exception("No serializer found for ${T::class.simpleName}")
            logError(e)
            throw e
        }

        debugPrint { "AppUtils/parseJson<reified>: using kotlinx serialization for ${T::class.simpleName}" }
        return json.decodeFromString(serializer, value)
    }

    inline fun <reified T> tryParseJson(value: String?): T? {
        return try {
            parseJson(value ?: return null)
        } catch (_: Exception) {
            null
        }
    }
}
