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
 * constructor arguments, and the nested object pattern causes class initialization ordering
 * issues on Android. Instead, create a top-level anonymous object:
 *
 *   @Serializable
 *   data class MyData(
 *       val fieldA: String = "",
 *       val fieldB: String = ""
 *   )
 *
 *   val myDataSerializer = object : WriteOnlySerializer<MyData>(
 *       MyData.serializer(),
 *       setOf("fieldB")
 *   ) {}
 *
 *   val encoded = json.encodeToString(myDataSerializer, MyData(fieldA = "hello", fieldB = "secret"))
 *   val decoded = json.decodeFromString(myDataSerializer, encoded)
 */
abstract class WriteOnlySerializer<T : Any>(
    tSerializer: KSerializer<T>,
    private val keysToIgnore: Set<String>
) : JsonTransformingSerializer<T>(tSerializer) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        return JsonObject(element.filterKeys { it !in keysToIgnore })
    }
}
