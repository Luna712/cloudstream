package com.lagradost.cloudstream3.utils.serializers

import com.lagradost.cloudstream3.json
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    @Serializable(with = WriteOnlyIntSerializer::class)
    val rating: Int? = null
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

    // region WriteOnlyIntSerializer

    @Test
    fun writeOnlyIntSerializerOmitsFieldOnSerialize() {
        val data = WriteOnlyData(fieldA = "hello", rating = 5)
        val result = json.encodeToString(WriteOnlyData.serializer(), data)
        assertTrue(result.contains("fieldA"))
        assertFalse(result.contains("rating"))
    }

    @Test
    fun writeOnlyIntSerializerDeserializesNormally() {
        val input = """{"fieldA":"hello","rating":5}"""
        val result = json.decodeFromString(WriteOnlyData.serializer(), input)
        assertEquals("hello", result.fieldA)
        assertEquals(5, result.rating)
    }

    @Test
    fun writeOnlyIntSerializerDeserializesNull() {
        val input = """{"fieldA":"hello","rating":null}"""
        val result = json.decodeFromString(WriteOnlyData.serializer(), input)
        assertEquals("hello", result.fieldA)
        assertNull(result.rating)
    }

    @Test
    fun writeOnlyIntSerializerDeserializesMissingAsNull() {
        val input = """{"fieldA":"hello"}"""
        val result = json.decodeFromString(WriteOnlyData.serializer(), input)
        assertEquals("hello", result.fieldA)
        assertNull(result.rating)
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
