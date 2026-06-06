package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream3.json
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

    /** Sometimes we want to encode as JSON even if it is already a String. */
    @InternalAPI
    fun Any.toJsonLiteral(): String {
        val serializer =
            this::class.serializerOrNull() ?: json.serializersModule.getContextual(this::class)
        return if (serializer != null) {
            @Suppress("UNCHECKED_CAST")
            json.encodeToString(serializer as KSerializer<Any>, this)
        } else {
            json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), json.encodeToJsonElement(kotlinx.serialization.json.JsonElement.serializer(), this as kotlinx.serialization.json.JsonElement))
        }
    }

    @InternalAPI
    fun <T : Any> parseJson(value: String, kClass: KClass<T>): T {
        val serializer = kClass.serializerOrNull() ?: json.serializersModule.getContextual(kClass)
            ?: error("No serializer found for ${kClass.simpleName}")
        return json.decodeFromString(serializer, value)
    }

    inline fun <reified T : Any> parseJson(value: String): T {
        val serializer = runCatching { serializer<T>() }.getOrNull()
            ?: json.serializersModule.getContextual(T::class)
            ?: error("No serializer found for ${T::class.simpleName}")
        return json.decodeFromString(serializer, value)
    }

    @Deprecated(
        "This overload was only ever used for BasePlugin.Manifest which has since been migrated. " +
                "No other code should be using this. Use reader.readText() and call parseJson(String) instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("parseJson<T>(reader.readText())")
    )
    inline fun <reified T> parseJson(reader: java.io.Reader, valueType: Class<T>): T {
        throw UnsupportedOperationException("Jackson removed. Use parseJson<T>(reader.readText()) instead.")
    }

    inline fun <reified T> tryParseJson(value: String?): T? {
        return try {
            parseJson(value ?: return null)
        } catch (e: Exception) {
            logError(e)
            null
        }
    }
}
