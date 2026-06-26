package com.lagradost.cloudstream3.utils

import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream3.json
import com.lagradost.cloudstream3.mapper
import com.lagradost.cloudstream3.mvvm.debugPrint
import com.lagradost.cloudstream3.mvvm.logError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
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
        var fallbackTrace: String? = null
        if (serializer != null) {
            try {
                debugPrint { "AppUtils/toJsonLiteral: using kotlinx serialization for ${this::class.qualifiedName}" }
                return json.encodeToString(serializer, this)
            } catch (e: SerializationException) {
                logError(e)
                fallbackTrace = e.stackTraceToString()
                debugPrint { "AppUtils/toJsonLiteral: kotlinx failed, falling back to Jackson for ${this::class.qualifiedName}" }
            }
        } else {
            fallbackTrace = Exception().stackTraceToString()
        }
        debugPrint { "AppUtils/toJsonLiteral: using Jackson for ${this::class.qualifiedName}\n$fallbackTrace" }
        return mapper.writeValueAsString(this)
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
        var fallbackTrace: String? = null
        if (serializer != null) {
            try {
                debugPrint { "AppUtils/parseJson(kClass): using kotlinx serialization for ${kClass.qualifiedName}" }
                return json.decodeFromString(serializer, value)
            } catch (e: SerializationException) {
                logError(e)
                fallbackTrace = e.stackTraceToString()
                debugPrint { "AppUtils/parseJson(kClass): kotlinx failed, falling back to Jackson for ${kClass.qualifiedName}" }
            }
        } else {
            fallbackTrace = Exception().stackTraceToString()
        }
        debugPrint { "AppUtils/parseJson(kClass): using Jackson for ${kClass.qualifiedName}\n$fallbackTrace" }
        return mapper.readValue(value, kClass.java)
    }

    // This is inlined code and can easily cause breakage in extensions!
    // Watch out when editing this to make sure stable also supports all inlined code!
    inline fun <reified T : Any> parseJson(value: String): T {
        val serializer = runCatching { serializer<T>() }
            .recoverCatching { json.serializersModule.getContextual(T::class) }
            .getOrNull()

        var fallbackTrace: String? = null
        if (serializer != null) {
            try {
                debugPrint { "AppUtils/parseJson<reified>: using kotlinx serialization for ${T::class.qualifiedName}" }
                return json.decodeFromString(serializer, value)
            } catch (e: SerializationException) {
                logError(e)
                fallbackTrace = e.stackTraceToString()
                debugPrint { "AppUtils/parseJson<reified>: kotlinx failed, falling back to Jackson for ${T::class.qualifiedName}" }
            } catch (e: Throwable) {
                fallbackTrace = e.stackTraceToString()
                debugPrint { "AppUtils/parseJson<reified>: unexpected error for ${T::class.qualifiedName}, falling back to Jackson" }
            }
        } else {
            fallbackTrace = Exception().stackTraceToString()
        }
        debugPrint { "AppUtils/parseJson<reified>: using Jackson for ${T::class.qualifiedName}\n$fallbackTrace" }
        return mapper.readValue(value)
    }

    @Deprecated(
        "This overload was only ever used for BasePlugin.Manifest which has since been migrated. " +
                "No other code should be using this. Use reader.readText() and call parseJson(String) instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("parseJson<T>(reader.readText())")
    )
    inline fun <reified T> parseJson(reader: java.io.Reader, valueType: Class<T>): T {
        return mapper.readValue(reader, valueType)
    }

    inline fun <reified T> tryParseJson(value: String?): T? {
        return try {
            parseJson(value ?: return null)
        } catch (_: Exception) {
            null
        }
    }
}
