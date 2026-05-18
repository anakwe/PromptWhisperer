package com.promptwhisperer.services

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/**
 * Deterministic slug utility to create filesystem-safe names from task descriptions.
 * Keeps only ASCII letters, digits and hyphens.
 */
object SlugUtil {
    fun slugify(input: String): String {
        val norm = Normalizer.normalize(input, Normalizer.Form.NFD)
        val ascii = norm.replace("[^\\p{ASCII}]".toRegex(), "")
        val cleaned = ascii
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .take(50)
        return if (cleaned.isBlank()) {
            "task-${Random(0).nextInt(10000)}"
        } else cleaned
    }
}
