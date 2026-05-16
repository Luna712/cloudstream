package com.lagradost.cloudstream3.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Replicates Jackson's @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) behaviour.
 * Properties in [keysToIgnore] are deserialized normally but omitted from serialized output.
 *
 * Cannot be used directly with @Serializable(with = ...) since annotations don't support
 * constructor arguments. Instead, create a named object in the target class that extends
 * this and passes the keys to ignore:
 *
 *   @Serializable(with = MyData.Serializer::class)
 *   data class MyData(
 *       val fieldA: String,
 *       val fieldB: String,
 *   ) {
 *       object Serializer : WriteOnlySerializer<MyData>(
 *           MyData.serializer(),
 *           setOf("fieldB"),
 *       )
 *   }
 */
abstract class WriteOnlySerializer<T : Any>(
    tSerializerProducer: () -> KSerializer<T>,
    private val keysToIgnore: Set<String>,
) : JsonTransformingSerializer<T>(tSerializerProducer()) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        return JsonObject(element.filterKeys { it !in keysToIgnore })
    }
}
