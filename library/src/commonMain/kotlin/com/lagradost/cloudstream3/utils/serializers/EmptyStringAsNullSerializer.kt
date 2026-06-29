package com.lagradost.cloudstream3.utils.serializers

import com.lagradost.cloudstream3.Prerelease
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Replicates Jackson's ACCEPT_EMPTY_STRING_AS_NULL_OBJECT behaviour.
 * An empty JSON string (`""`) on any field is replaced with JsonNull so that
 * nullable fields decode as null. Requires the enclosing Json instance to
 * have encodeDefaults = true, which is already in our default global Json instance.
 *
 * Usage:
 *
 *   @OptIn(ExperimentalSerializationApi::class)
 *   @KeepGeneratedSerializer
 *   @Serializable(with = MyData.Serializer::class)
 *   data class MyData(
 *       val title: String? = null,
 *       val episode: Episode? = null,
 *   ) {
 *       object Serializer : EmptyStringAsNullSerializer<MyData>(MyData.generatedSerializer())
 *   }
 */
@Prerelease
abstract class EmptyStringAsNullSerializer<T : Any>(tSerializer: KSerializer<T>) :
    JsonTransformingSerializer<T>(tSerializer) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        return JsonObject(element.mapValues { (_, value) ->
            when {
                value is JsonPrimitive && value.isString && value.content.isEmpty() -> JsonNull
                else -> value
            }
        })
    }
}
