package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream3.json
import com.lagradost.cloudstream3.mvvm.logError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
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
        if (serializer != null) {
            @Suppress("UNCHECKED_CAST")
            return json.encodeToString(serializer as KSerializer<Any>, this)
        }
        return when (this) {
            is Set<*> -> json.encodeToString(SetSerializer(elementSerializer()), this as Set<Any?>)
            is List<*> -> json.encodeToString(ListSerializer(elementSerializer()), this as List<Any?>)
            is Collection<*> -> json.encodeToString(ListSerializer(elementSerializer()), this.toList() as List<Any?>)
            is Map<*, *> -> json.encodeToString(MapSerializer(String.serializer(), valueSerializer()), this as Map<String, Any?>)
            else -> error("No serializer found for ${this::class.simpleName}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun elementSerializerForClass(kClass: KClass<*>): KSerializer<Any?> {
        val serializer = kClass.serializerOrNull()
            ?: json.serializersModule.getContextual(kClass)
            ?: error("No serializer found for element type ${kClass.simpleName}")
        return serializer as KSerializer<Any?>
    }

    private fun Collection<*>.elementSerializer(): KSerializer<Any?> {
        val elementClass = this.firstOrNull()?.let { it::class } ?: String::class
        return elementSerializerForClass(elementClass)
    }

    private fun Map<*, *>.valueSerializer(): KSerializer<Any?> {
        val elementClass = this.values.firstOrNull()?.let { it::class } ?: String::class
        return elementSerializerForClass(elementClass)
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
