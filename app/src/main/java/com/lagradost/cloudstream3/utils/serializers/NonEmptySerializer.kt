package com.lagradost.cloudstream3.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Replicates Jackson's @JsonInclude(JsonInclude.Include.NON_EMPTY) behaviour.
 * Strips null, empty strings, empty arrays, and empty objects from the serialized
 * output. Requires the enclosing Json instance to have encodeDefaults = true,
 * which is already in our default global Json instance.
 *
 * Can be applied to a single property or to the entire class to cover all properties:
 *
 *   @Serializable(with = NonEmptySerializer::class)
 *   data class MyData(
 *       val tags: List<String> = emptyList(),
 *       val title: String = "",
 *       val meta: Map<String, String> = emptyMap()
 *   )
 */
abstract class NonEmptySerializer<T : Any>(tSerializer: KSerializer<T>) :
    JsonTransformingSerializer<T>(tSerializer) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element

        return JsonObject(element.filterValues { value ->
            when (value) {
                is JsonPrimitive -> value.content.isNotEmpty()
                is JsonArray -> value.isNotEmpty()
                is JsonObject -> value.isNotEmpty()
                JsonNull -> false
            }
        })
    }
}
