package com.promptwhisperer

import com.promptwhisperer.services.SafeProjectContext
import com.promptwhisperer.services.SecurityFilterServiceImpl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityFilterTests {
    @Test
    fun `blocked patterns removed`() {
        val raw =
            SafeProjectContext(
                projectName = "p",
                safeFiles = listOf("src/Main.kt", "secrets.env", "id_rsa"),
                smallFileContents = mapOf("README.md" to "ok"),
                excluded = emptyList(),
            )
        val result = SecurityFilterServiceImpl().filterContext(raw)
        assertTrue(result.safeFiles.contains("src/Main.kt"))
        assertFalse(result.safeFiles.contains("secrets.env"))
        assertFalse(result.safeFiles.contains("id_rsa"))
    }
}
