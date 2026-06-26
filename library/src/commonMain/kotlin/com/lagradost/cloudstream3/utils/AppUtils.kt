package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.InternalAPI
import com.lagradost.cloudstream3.json
import com.lagradost.cloudstream3.mvvm.debugPrint
import com.lagradost.cloudstream3.mvvm.logError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
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
    inline fun <reified T : Any> T.toJsonLiteral(): String {
        val serializer = serializer<T>()
        return try {
            debugPrint { "AppUtils/toJsonLiteral: using kotlinx serialization for ${T::class.qualifiedName}" }
            json.encodeToString(serializer, this)
        } catch (e: SerializationException) {
            logError(e)
            debugPrint { "AppUtils/toJsonLiteral: kotlinx failed for ${T::class.qualifiedName}" }
            throw e
        }
    }

    @InternalAPI
    fun <T : Any> parseJson(value: String, kClass: KClass<T>): T {
        val serializer = kClass.serializerOrNull() ?: json.serializersModule.getContextual(kClass)
            ?: throw SerializationException("No serializer found for ${kClass.qualifiedName}")
        return try {
            debugPrint { "AppUtils/parseJson(kClass): using kotlinx serialization for ${kClass.qualifiedName}" }
            json.decodeFromString(serializer, value)
        } catch (e: SerializationException) {
            logError(e)
            debugPrint { "AppUtils/parseJson(kClass): kotlinx failed for ${kClass.qualifiedName}" }
            throw e
        }
    }

    // This is inlined code and can easily cause breakage in extensions!
    // Watch out when editing this to make sure stable also supports all inlined code!
    inline fun <reified T : Any> parseJson(value: String): T {
        val serializer = serializer<T>()
        return try {
            debugPrint { "AppUtils/parseJson<reified>: using kotlinx serialization for ${T::class.qualifiedName}" }
            json.decodeFromString(serializer, value)
        } catch (e: SerializationException) {
            logError(e)
            debugPrint { "AppUtils/parseJson<reified>: kotlinx failed for ${T::class.qualifiedName}" }
            throw e
        }
    }

    inline fun <reified T : Any> tryParseJson(value: String?): T? {
        return try {
            parseJson(value ?: return null)
        } catch (_: Exception) {
            null
        }
    }
}
