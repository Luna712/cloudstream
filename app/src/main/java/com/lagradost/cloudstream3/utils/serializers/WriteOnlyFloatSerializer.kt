package com.lagradost.cloudstream3.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Replicates Jackson's @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) behaviour for Float.
 * The annotated property is deserialized normally but omitted from serialized output.
 *
 * Usage:
 *
 *   @Serializable
 *   data class MyData(
 *       val fieldA: String = "",
 *       @Serializable(with = WriteOnlyFloatSerializer::class)
 *       val rating: Float = 0f
 *   )
 */
object WriteOnlyFloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WriteOnlyFloat", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: Float) {
        // Do nothing, this property is intentionally omitted from serialization.
    }

    override fun deserialize(decoder: Decoder): Float {
        return decoder.decodeFloat()
    }
}
