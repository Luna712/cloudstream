package com.lagradost.cloudstream3.utils

import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream3.json
import com.lagradost.cloudstream3.mapper
import com.lagradost.cloudstream3.mvvm.logError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
        // @Serializable generates a serializer at compile time; contextual serializers are
        // registered manually in serializersModule, we need both to support all cases
        val serializer =
            this::class.serializerOrNull() ?: json.serializersModule.getContextual(this::class)
        if (serializer != null) {
            try {
                @Suppress("UNCHECKED_CAST")
                return json.encodeToString(serializer as KSerializer<Any>, this)
            } catch (e: SerializationException) {
                logError(e)
                return mapper.writeValueAsString(this)
            }
        }
        // Handle generic collection/map types where type params are erased at runtime.
        // Convert to JsonElement to support mixed types within collections.
        return try {
            json.encodeToString(JsonElement.serializer(), toJsonElement())
        } catch (e: SerializationException) {
            logError(e)
            mapper.writeValueAsString(this)
        }
    }

    /**
     * Recursively converts any value to a [JsonElement], supporting mixed-type
     * collections, nested maps, nulls, primitives, and @Serializable objects.
     */
    private fun Any?.toJsonElement(): JsonElement {
        if (this == null) return JsonNull
        // Try kotlinx serializer first (handles @Serializable and primitives)
        val serializer = this::class.serializerOrNull()
            ?: json.serializersModule.getContextual(this::class)
        if (serializer != null) {
            try {
                @Suppress("UNCHECKED_CAST")
                return json.encodeToJsonElement(serializer as KSerializer<Any>, this)
            } catch (_: SerializationException) {
                // fall through to manual handling
            }
        }
        return when (this) {
            is Boolean -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            is Enum<*> -> JsonPrimitive(this.name)
            is Array<*> -> JsonArray(this.map { it.toJsonElement() })
            is Collection<*> -> JsonArray(this.map { it.toJsonElement() })
            is Map<*, *> -> JsonObject(this.entries.associate { (k, v) ->
                k.toString() to v.toJsonElement()
            })
            else -> throw SerializationException("No serializer found for ${this::class.simpleName}")
        }
    }

    @InternalAPI
    fun <T : Any> parseJson(value: String, kClass: KClass<T>): T {
        val serializer = kClass.serializerOrNull() ?: json.serializersModule.getContextual(kClass)
        if (serializer != null) {
            try {
                return json.decodeFromString(serializer, value)
            } catch (e: SerializationException) {
                logError(e)
            }
        }

        return mapper.readValue(value, kClass.java)
    }

    // This is inlined code and can easily cause breakage in extensions!
    // Watch out when editing this to make sure stable also supports all inlined code!
    inline fun <reified T : Any> parseJson(value: String): T {
        // @Serializable generates a serializer at compile time; contextual serializers are
        // registered manually in serializersModule, we need both to support all cases
        val serializer = runCatching { serializer<T>() }.getOrNull()
            ?: json.serializersModule.getContextual(T::class)

        // Prefer Kotlin Serialization over Jackson
        if (serializer != null) {
            try {
                return json.decodeFromString(serializer, value)
            } catch (e: SerializationException) {
                logError(e)
            }
        }

        return mapper.readValue(value)
    }

    @Deprecated(
        "This overload was only ever used for BasePlugin.Manifest which has since been migrated. " +
                "No other code should be using this. Use reader.readText() and call parseJson(String) instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("parseJson<T>(reader.readText())")
    )
    inline fun <reified T> parseJson(reader: java.io.Reader, valueType: Class<T>): T {
        // Reader-based parsing has no kotlinx equivalent, fall back to Jackson
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
