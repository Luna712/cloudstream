package com.lagradost.cloudstream3.utils.serializers

import com.lagradost.cloudstream3.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
data class NonEmptyData(
    val title: String = "",
    val tags: List<String> = emptyList(),
    val meta: Map<String, String> = emptyMap(),
    val name: String = "hello"
)

private val nonEmptySerializer = NonEmptySerializer(NonEmptyData.serializer())

@Serializable
data class WriteOnlyData(
    val fieldA: String = "",
    @Serializable(with = WriteOnlySerializer::class)
    val fieldB: String = ""
)

@Serializable
data class MultiWriteOnly(
    val fieldA: String = "",
    @Serializable(with = WriteOnlySerializer::class)
    val fieldB: String = "",
    @Serializable(with = WriteOnlySerializer::class)
    val fieldC: String = ""
)

class SerializerTest {

    // region NonEmptySerializer

    @Test
    fun nonEmptySerializerOmitsEmptyStrings() {
        val data = NonEmptyData(title = "", name = "hello")
        val result = json.encodeToString(nonEmptySerializer, data)
        assertFalse(result.contains("title"))
        assertTrue(result.contains("name"))
    }

    @Test
    fun nonEmptySerializerOmitsEmptyLists() {
        val data = NonEmptyData(tags = emptyList(), name = "hello")
        val result = json.encodeToString(nonEmptySerializer, data)
        assertFalse(result.contains("tags"))
    }

    @Test
    fun nonEmptySerializerOmitsEmptyMaps() {
        val data = NonEmptyData(meta = emptyMap(), name = "hello")
        val result = json.encodeToString(nonEmptySerializer, data)
        assertFalse(result.contains("meta"))
    }

    @Test
    fun nonEmptySerializerKeepsNonEmptyFields() {
        val data = NonEmptyData(title = "hello", tags = listOf("a"), meta = mapOf("k" to "v"))
        val result = json.encodeToString(nonEmptySerializer, data)
        assertTrue(result.contains("title"))
        assertTrue(result.contains("tags"))
        assertTrue(result.contains("meta"))
    }

    @Test
    fun nonEmptySerializerDoesNotAffectDeserialization() {
        val input = """{"title":"hello","tags":["a"],"meta":{"k":"v"},"name":"world"}"""
        val result = json.decodeFromString(nonEmptySerializer, input)
        assertEquals("hello", result.title)
        assertEquals(listOf("a"), result.tags)
        assertEquals(mapOf("k" to "v"), result.meta)
        assertEquals("world", result.name)
    }

    // endregion

    // region WriteOnlySerializer

    @Test
    fun writeOnlySerializerOmitsWriteOnlyFieldOnSerialize() {
        val data = WriteOnlyData(fieldA = "hello", fieldB = "secret")
        val result = json.encodeToString(WriteOnlyData.serializer(), data)
        assertTrue(result.contains("fieldA"))
        assertFalse(result.contains("fieldB"))
    }

    @Test
    fun writeOnlySerializerDeserializesWriteOnlyFieldNormally() {
        val input = """{"fieldA":"hello","fieldB":"secret"}"""
        val result = json.decodeFromString(WriteOnlyData.serializer(), input)
        assertEquals("hello", result.fieldA)
        assertEquals("secret", result.fieldB)
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
        val uri = android.net.Uri.parse("https://example.com/path?query=1")
        val result = json.encodeToString(UriSerializer, uri)
        assertEquals("\"https://example.com/path?query=1\"", result)
    }

    @Test
    fun uriSerializerDeserializesStringToUri() {
        val input = "\"https://example.com/path?query=1\""
        val result = json.decodeFromString(UriSerializer, input)
        assertEquals(android.net.Uri.parse("https://example.com/path?query=1"), result)
    }

    @Test
    fun uriSerializerRoundtripsCorrectly() {
        val uri = android.net.Uri.parse("https://example.com/path?query=1")
        val encoded = json.encodeToString(UriSerializer, uri)
        val decoded = json.decodeFromString(UriSerializer, encoded)
        assertEquals(uri, decoded)
    }

    // endregion
}
