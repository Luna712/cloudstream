package com.lagradost.cloudstream3.utils.serializers

import com.lagradost.cloudstream3.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializerTest {

    // region NonEmptySerializer

    @Serializable(with = NonEmptySerializer::class)
    data class NonEmptyData(
        val title: String = "",
        val tags: List<String> = emptyList(),
        val meta: Map<String, String> = emptyMap(),
        val name: String = "hello"
    )

    @Test
    fun `NonEmptySerializer omits empty strings`() {
        val data = NonEmptyData(title = "", name = "hello")
        val result = json.encodeToString(NonEmptySerializer(NonEmptyData.serializer()), data)
        assertFalse(result.contains("title"))
        assertTrue(result.contains("name"))
    }

    @Test
    fun `NonEmptySerializer omits empty lists`() {
        val data = NonEmptyData(tags = emptyList(), name = "hello")
        val result = json.encodeToString(NonEmptySerializer(NonEmptyData.serializer()), data)
        assertFalse(result.contains("tags"))
    }

    @Test
    fun `NonEmptySerializer omits empty maps`() {
        val data = NonEmptyData(meta = emptyMap(), name = "hello")
        val result = json.encodeToString(NonEmptySerializer(NonEmptyData.serializer()), data)
        assertFalse(result.contains("meta"))
    }

    @Test
    fun `NonEmptySerializer keeps non-empty fields`() {
        val data = NonEmptyData(title = "hello", tags = listOf("a"), meta = mapOf("k" to "v"))
        val result = json.encodeToString(NonEmptySerializer(NonEmptyData.serializer()), data)
        assertTrue(result.contains("title"))
        assertTrue(result.contains("tags"))
        assertTrue(result.contains("meta"))
    }

    @Test
    fun `NonEmptySerializer does not affect deserialization`() {
        val input = """{"title":"hello","tags":["a"],"meta":{"k":"v"},"name":"world"}"""
        val result = json.decodeFromString(NonEmptySerializer(NonEmptyData.serializer()), input)
        assertEquals("hello", result.title)
        assertEquals(listOf("a"), result.tags)
        assertEquals(mapOf("k" to "v"), result.meta)
        assertEquals("world", result.name)
    }

    // endregion

    // region WriteOnlySerializer

    @Serializable(with = WriteOnlyData.Serializer::class)
    data class WriteOnlyData(
        val fieldA: String = "",
        val fieldB: String = ""
    ) {
        object Serializer : WriteOnlySerializer<WriteOnlyData>(
            WriteOnlyData.serializer(),
            setOf("fieldB")
        )
    }

    @Test
    fun `WriteOnlySerializer omits write-only field on serialize`() {
        val data = WriteOnlyData(fieldA = "hello", fieldB = "secret")
        val result = json.encodeToString(WriteOnlyData.serializer(), data)
        assertTrue(result.contains("fieldA"))
        assertFalse(result.contains("fieldB"))
    }

    @Test
    fun `WriteOnlySerializer deserializes write-only field normally`() {
        val input = """{"fieldA":"hello","fieldB":"secret"}"""
        val result = json.decodeFromString(WriteOnlyData.serializer(), input)
        assertEquals("hello", result.fieldA)
        assertEquals("secret", result.fieldB)
    }

    @Test
    fun `WriteOnlySerializer handles multiple keys`() {
        @Serializable(with = MultiWriteOnly.Serializer::class)
        data class MultiWriteOnly(
            val fieldA: String = "",
            val fieldB: String = "",
            val fieldC: String = ""
        ) {
            object Serializer : WriteOnlySerializer<MultiWriteOnly>(
                MultiWriteOnly.serializer(),
                setOf("fieldB", "fieldC")
            )
        }

        val data = MultiWriteOnly(fieldA = "hello", fieldB = "secret1", fieldC = "secret2")
        val result = json.encodeToString(MultiWriteOnly.serializer(), data)
        assertTrue(result.contains("fieldA"))
        assertFalse(result.contains("fieldB"))
        assertFalse(result.contains("fieldC"))
    }

    // endregion

    // region UriSerializer

    @Test
    fun `UriSerializer serializes uri to string`() {
        val uri = android.net.Uri.parse("https://example.com/path?query=1")
        val result = json.encodeToString(UriSerializer, uri)
        assertEquals("\"https://example.com/path?query=1\"", result)
    }

    @Test
    fun `UriSerializer deserializes string to uri`() {
        val input = "\"https://example.com/path?query=1\""
        val result = json.decodeFromString(UriSerializer, input)
        assertEquals(android.net.Uri.parse("https://example.com/path?query=1"), result)
    }

    @Test
    fun `UriSerializer roundtrips correctly`() {
        val uri = android.net.Uri.parse("https://example.com/path?query=1")
        val encoded = json.encodeToString(UriSerializer, uri)
        val decoded = json.decodeFromString(UriSerializer, encoded)
        assertEquals(uri, decoded)
    }

    // endregion
}
