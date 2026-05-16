package com.lagradost.cloudstream3.utils.serializers

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Replicates Jackson's @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) behaviour for Int.
 * The annotated property is deserialized normally but omitted from serialized output.
 *
 *   @Serializable
 *   data class MyData(
 *       val fieldA: String = "",
 *       @Serializable(with = WriteOnlyIntSerializer::class)
 *       val rating: Int = 0
 *   )
 */
object WriteOnlyIntSerializer : JsonTransformingSerializer<Int?>(Int.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement = JsonNull
}
