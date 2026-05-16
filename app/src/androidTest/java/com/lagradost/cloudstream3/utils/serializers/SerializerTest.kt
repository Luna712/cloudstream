package com.lagradost.cloudstream3.utils.serializers

import android.net.Uri
import com.lagradost.cloudstream3.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
@KeepGeneratedSerializer
@Serializable(with = NonEmptyData.Serializer::class)
data class NonEmptyData(
    val title: String = "",
    val tags: List<String> = emptyList(),
    val meta: Map<String, String> = emptyMap(),
    val name: String = "hello",
) {
    object Serializer : NonEmptySerializer<NonEmptyData>(NonEmptyData.generatedSerializer())
}

@OptIn(ExperimentalSerializationApi::class)
@KeepGeneratedSerializer
@Serializable(with = WriteOnlyData.Serializer::class)
data class WriteOnlyData(
    val fieldA: String = "",
    val fieldB: String = "",
) {
    object Serializer : WriteOnlySerializer<WriteOnlyData>(
        WriteOnlyData.generatedSerializer(),
        setOf("fieldB"),
    )
}

@OptIn(ExperimentalSerializationApi::class)
@KeepGeneratedSerializer
@Serializable(with = MultiWriteOnly.Serializer::class)
data class MultiWriteOnly(
    val fieldA: String = "",
    val fieldB: String = "",
    val fieldC: String = "",
) {
    object Serializer : WriteOnlySerializer<MultiWriteOnly>(
        MultiWriteOnly.generatedSerializer(),
        setOf("fieldB", "fieldC"),
    )
}

@Serializable
data class UriData(
    @Serializable(with = UriSerializer::class)
    val uri: Uri = Uri.EMPTY,
)

class SerializerTest {

    // region NonEmptySerializer

    @Test
    fun nonEmptySerializerOmitsEmptyStrings() {
        val data = NonEmptyData(title = "", name = "hello")
        val result = json.encodeToString(NonEmptyData.serializer(), data)
        assertFalse(result.contains("title"))
        assertTrue(result.contains("name"))
    }

    @Test
    fun nonEmptySerializerOmitsEmptyLists() {
        val data = NonEmptyData(tags = emptyList(), name = "hello")
        val result = json.encodeToString(NonEmptyData.serializer(), data)
        assertFalse(result.contains("tags"))
    }

    @Test
    fun nonEmptySerializerOmitsEmptyMaps() {
        val data = NonEmptyData(meta = emptyMap(), name = "hello")
        val result = json.encodeToString(NonEmptyData.serializer(), data)
        assertFalse(result.contains("meta"))
    }

    @Test
    fun nonEmptySerializerKeepsNonEmptyFields() {
        val data = NonEmptyData(title = "hello", tags = listOf("a"), meta = mapOf("k" to "v"))
        val result = json.encodeToString(NonEmptyData.serializer(), data)
        assertTrue(result.contains("title"))
        assertTrue(result.contains("tags"))
        assertTrue(result.contains("meta"))
    }

    @Test
    fun nonEmptySerializerDoesNotAffectDeserialization() {
        val input = """{"title":"hello","tags":["a"],"meta":{"k":"v"},"name":"world"}"""
        val result = json.decodeFromString(NonEmptyData.serializer(), input)
        assertEquals("hello", result.title)
        assertEquals(listOf("a"), result.tags)
        assertEquals(mapOf("k" to "v"), result.meta)
        assertEquals("world", result.name)
    }

    // endregion

    // region WriteOnlySerializer

    @Test
    fun writeOnlySerializerOmitsFieldOnSerialize() {
        val data = WriteOnlyData(fieldA = "hello", fieldB = "secret")
        val result = json.encodeToString(WriteOnlyData.serializer(), data)
        assertTrue(result.contains("fieldA"))
        assertFalse(result.contains("fieldB"))
    }

    @Test
    fun writeOnlySerializerDeserializesNormally() {
        val input = """{"fieldA":"hello","fieldB":"secret"}"""
        val result = json.decodeFromString(WriteOnlyData.serializer(), input)
        assertEquals("hello", result.fieldA)
        assertEquals("secret", result.fieldB)
    }

    @Test
    fun writeOnlySerializerDeserializesMissingAsDefault() {
        val input = """{"fieldA":"hello"}"""
        val result = json.decodeFromString(WriteOnlyData.serializer(), input)
        assertEquals("hello", result.fieldA)
        assertEquals("", result.fieldB)
    }

    @Test
    fun writeOnlySerializerHandlesMultipleKeys() {
        val data = MultiWriteOnly(fieldA = "hello", fieldB = "secret1", fieldC = "secret2")
        val result = json.encodeToString(MultiWriteOnly.serializer(), data)
        assertTrue(result.contains("fieldA"))
        assertFalse(result.contains("fieldB"))
        assertFalse(result.contains("fieldC"))
    }

    // endregion

    // region UriSerializer

    @Test
    fun uriSerializerSerializesUriToString() {
        val data = UriData(uri = Uri.parse("https://example.com/path?query=1"))
        val result = json.encodeToString(UriData.serializer(), data)
        assertTrue(result.contains("https://example.com/path?query=1"))
    }

    @Test
    fun uriSerializerDeserializesStringToUri() {
        val input = """{"uri":"https://example.com/path?query=1"}"""
        val result = json.decodeFromString(UriData.serializer(), input)
        assertEquals(Uri.parse("https://example.com/path?query=1"), result.uri)
    }

    @Test
    fun uriSerializerRoundtripsCorrectly() {
        val data = UriData(uri = Uri.parse("https://example.com/path?query=1"))
        val encoded = json.encodeToString(UriData.serializer(), data)
        val decoded = json.decodeFromString(UriData.serializer(), encoded)
        assertEquals(data.uri, decoded.uri)
    }

    // endregion
}
