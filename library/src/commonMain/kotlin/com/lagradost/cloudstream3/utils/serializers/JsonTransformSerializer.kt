package com.lagradost.cloudstream3.utils.serializers

import com.lagradost.cloudstream3.Prerelease
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Composable deserialize transformer that applies multiple Jackson-equivalent
 * behaviours to specific fields by key. Pass only the sets you need; all
 * default to empty (no-op).
 *
 * Behaviours (applied in order per field):
 *   singleValueAsListKeys ACCEPT_SINGLE_VALUE_AS_ARRAY
 *   emptyArrayAsNullKeys ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT
 *   emptyStringAsNullKeys ACCEPT_EMPTY_STRING_AS_NULL_OBJECT
 *
 * Usage:
 *
 *   @OptIn(ExperimentalSerializationApi::class)
 *   @KeepGeneratedSerializer
 *   @Serializable(with = MyData.Serializer::class)
 *   data class MyData(
 *       val title: String? = null,
 *       val tags: List<String>? = null,
 *       val meta: MyMeta? = null,
 *   ) {
 *       object Serializer : JsonTransformSerializer<MyData>(
 *           MyData.generatedSerializer(),
 *           singleValueAsListKeys = setOf("tags"),
 *           emptyArrayAsNullKeys = setOf("tags", "meta"),
 *           emptyStringAsNullKeys = setOf("title", "tags"),
 *       )
 *   }
 */
@Prerelease
abstract class JsonTransformSerializer<T : Any>(
    tSerializer: KSerializer<T>,
    private val singleValueAsListKeys: Set<String> = emptySet(),
    private val emptyArrayAsNullKeys: Set<String> = emptySet(),
    private val emptyStringAsNullKeys: Set<String> = emptySet(),
) : JsonTransformingSerializer<T>(tSerializer) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        return JsonObject(element.mapValues { (key, value) ->
            var result = value
            if (key in emptyStringAsNullKeys) {
                if (result is JsonPrimitive && result.isString && result.content.isEmpty()) {
                    result = JsonNull
                }
            }

            if (key in emptyArrayAsNullKeys) {
                if (result is JsonArray && result.isEmpty()) {
                    result = JsonNull
                }
            }

            if (key in singleValueAsListKeys) {
                result = when (result) {
                    is JsonArray -> result
                    JsonNull -> JsonArray(emptyList())
                    else -> JsonArray(listOf(result))
                }
            }

            result
        })
    }
}
