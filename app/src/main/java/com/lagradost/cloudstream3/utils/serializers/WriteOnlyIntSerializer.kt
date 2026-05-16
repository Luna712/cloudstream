package com.lagradost.cloudstream3.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Replicates Jackson's @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) behaviour for Int?.
 * The annotated property is deserialized normally but omitted from serialized output.
 *
 * Usage:
 *
 *   @Serializable
 *   data class MyData(
 *       val fieldA: String = "",
 *       @Serializable(with = WriteOnlyIntSerializer::class)
 *       val rating: Int? = null
 *   )
 */
object WriteOnlyIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("WriteOnlyInt", PrimitiveKind.INT) {}.nullable

    override fun serialize(encoder: Encoder, value: Int?) {
        // Do nothing, this property is intentionally omitted from serialization.
    }

    override fun deserialize(decoder: Decoder): Int? {
        if (decoder.decodeNotNullMark()) {
            return decoder.decodeInt()
        }
        return decoder.decodeNull()
    }
}
