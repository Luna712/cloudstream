package com.lagradost.cloudstream3.utils.serializers

import com.lagradost.cloudstream3.Prerelease
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Replicates Jackson's ACCEPT_SINGLE_VALUE_AS_ARRAY behaviour.
 * A bare non-array JSON value is wrapped in a single-element list; a JSON
 * array is decoded normally; JSON null becomes an empty list.
 * Requires the enclosing Json instance to have encodeDefaults = true,
 * which is already in our default global Json instance.
 *
 * Usage:
 *
 *   @OptIn(ExperimentalSerializationApi::class)
 *   @KeepGeneratedSerializer
 *   @Serializable(with = MyData.Serializer::class)
 *   data class MyData(
 *       val tags: List<String> = emptyList(),
 *       val episodes: List<Episode> = emptyList(),
 *   ) {
 *       object Serializer : SingleValueAsListSerializer<MyData>(MyData.generatedSerializer())
 *   }
 */
@Prerelease
abstract class SingleValueAsListSerializer<T : Any>(tSerializer: KSerializer<T>) :
    JsonTransformingSerializer<T>(tSerializer) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        return JsonObject(element.mapValues { (_, value) ->
            when (value) {
                is JsonArray -> value
                JsonNull -> JsonArray(emptyList())
                else -> JsonArray(listOf(value))
            }
        })
    }
}
