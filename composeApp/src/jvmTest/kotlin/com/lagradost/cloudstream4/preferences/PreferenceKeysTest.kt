package com.lagradost.cloudstream4.preferences

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PreferenceKeysTest {

    @Test
    fun allPreferenceKeysAreUnique() {
        val sourceFile = File("src/commonMain/kotlin/com/lagradost/cloudstream4/preferences/PreferenceKeys.kt")
        val regex = """const val \w+ = "([^"]+)"""".toRegex()
        val keys = regex.findAll(sourceFile.readText()).map { it.groupValues[1] }.toList()

        assertTrue(keys.isNotEmpty(), "No preference keys found in source")
        assertTrue(keys.toSet().size == keys.size, "Preference keys must not collide: $keys")
    }
}
