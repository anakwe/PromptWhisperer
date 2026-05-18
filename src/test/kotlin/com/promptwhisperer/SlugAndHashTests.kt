package com.promptwhisperer

import com.promptwhisperer.services.HashingServiceImpl
import com.promptwhisperer.services.SlugUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SlugAndHashTests {
    @Test
    fun `slugify reduces to expected characters`() {
        val s = "Add OAuth Login (Google/GitHub) -- integrate!"
        val slug = SlugUtil.slugify(s)
        assertTrue(slug.matches(Regex("^[a-z0-9\\-]+$")))
    }

    @Test
    fun `sha256 produces stable hex length`() {
        val hs = HashingServiceImpl().sha256("hello".toByteArray())
        assertEquals(64, hs.length)
    }
}
