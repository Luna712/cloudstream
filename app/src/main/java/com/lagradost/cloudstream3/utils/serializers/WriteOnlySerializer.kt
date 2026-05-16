package com.lagradost.cloudstream3.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Replicates Jackson's @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) behaviour.
 * The annotated property is deserialized normally but omitted from serialized output.
 *
 * Usage:
 *
 *   @Serializable
 *   data class MyData(
 *       val fieldA: String = "",
 *       @Serializable(with = WriteOnlySerializer::class)
 *       val fieldB: String = ""
 *   )
 */
class WriteOnlySerializer<T>(
    private val serializer: KSerializer<T>
) : KSerializer<T> by serializer {

    override fun serialize(encoder: Encoder, value: T) {
        // Do nothing, this property is intentionally omitted from serialization.
    }
}
