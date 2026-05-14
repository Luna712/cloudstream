package com.lagradost.cloudstream3.utils

import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.mapper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import java.io.Reader

val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
object AppUtils {
    /** Any object as json string */
    fun Any.toJson(): String {
        if (this is String) return this
        // @Serializable generates a serializer at compile time; contextual serializers are
        // registered manually in serializersModule, we need both to support all cases
        val serializer = this::class.serializerOrNull() ?: json.serializersModule.getContextual(this::class)
        return if (serializer != null) {
            try {
                json.encodeToString(JsonElement.serializer(), json.parseToJsonElement(this.toString()))
            } catch (_: Exception) {
                mapper.writeValueAsString(this)
            }
        } else {
            mapper.writeValueAsString(this)
        }
    }

    inline fun <reified T : Any> parseJson(value: String): T {
        // @Serializable generates a serializer at compile time; contextual serializers are
        // registered manually in serializersModule, we need both to support all cases
        val serializer = T::class.serializerOrNull() ?: json.serializersModule.getContextual(T::class)
        return if (serializer != null) {
            try {
                json.decodeFromString(serializer, value)
            } catch (_: Exception) {
                mapper.readValue(value)
            }
        } else {
            mapper.readValue(value)
        }
    }

    inline fun <reified T> parseJson(reader: Reader, valueType: Class<T>): T {
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
