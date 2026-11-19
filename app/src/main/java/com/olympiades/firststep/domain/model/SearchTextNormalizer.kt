package com.olympiades.firststep.domain.model

import java.text.Normalizer
import java.util.Locale

object SearchTextNormalizer {
    private val TOKEN_SPLIT_REGEX = Regex("[\\s\\-_/.,;:'’\"()\\[\\]{}|\\\\?؟!]+")
    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val ARABIC_DIACRITICS_AND_TATWEEL_REGEX = Regex("[\\u0640\\u064B-\\u065F\\u0670]")

    fun sanitizeInputQuery(raw: String): String {
        return raw
            .filter { ch -> !ch.isISOControl() }
            .take(160)
    }

    fun normalizeSearchText(input: String): String {
        if (input.isBlank()) return ""
        val preNormalized = input
            .replace("œ", "oe")
            .replace("Œ", "oe")
            .replace("æ", "ae")
            .replace("Æ", "ae")
        val decomposed = Normalizer.normalize(preNormalized, Normalizer.Form.NFD)
        val withoutMarks = DIACRITICS_REGEX.replace(decomposed, "")
        val arabicCleaned = ARABIC_DIACRITICS_AND_TATWEEL_REGEX.replace(withoutMarks, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
        return arabicCleaned.lowercase(Locale.ROOT)
    }

    fun tokenizeQuery(query: String): List<String> {
        val normalized = normalizeSearchText(query).trim()
        if (normalized.isEmpty()) return emptyList()
        return normalized.split(TOKEN_SPLIT_REGEX).filter { it.isNotBlank() }
    }
}
