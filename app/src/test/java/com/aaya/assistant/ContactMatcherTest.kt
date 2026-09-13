package com.aaya.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactMatcherTest {

    private fun extractTargetName(rawSpokenText: String): String {
        var text = rawSpokenText.lowercase()

        val hindiSuffixes = listOf(
            "ko phone lagao", "ko phone karo", "ko call karo", "ko call kar",
            "ko call lagao", "ko call ghumao", "ko bolo", "ko message karo"
        )
        for (suffix in hindiSuffixes) {
            if (text.contains(suffix)) {
                text = text.replace(suffix, "")
            }
        }

        val englishPrefixes = listOf(
            "call to", "call my", "call", "dial", "phone", "ring", "please call"
        )
        for (prefix in englishPrefixes) {
            if (text.startsWith(prefix)) {
                text = text.removePrefix(prefix)
            }
        }

        return text.trim()
    }

    @Test
    fun testHinglishExtraction() {
        assertEquals("mummy", extractTargetName("Mummy ko call karo"))
        assertEquals("papa", extractTargetName("Papa ko phone lagao"))
        assertEquals("bhai", extractTargetName("Bhai ko call kar"))
    }

    @Test
    fun testEnglishExtraction() {
        assertEquals("mom", extractTargetName("Call Mom"))
        assertEquals("mother", extractTargetName("Call my mother"))
        assertEquals("alex", extractTargetName("Please call Alex"))
    }
}
