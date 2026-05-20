package com.promptwhisperer

import com.promptwhisperer.services.SafeProjectContext
import com.promptwhisperer.services.SecurityFilterServiceImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityFilterTests {
    private val service = SecurityFilterServiceImpl()

    @Test
    fun `secret-like files are excluded from safe files and file contents`() {
        // These files are commonly committed by mistake and must never be fed to prompt context.
        val blockedFiles = listOf(
            ".env",
            ".env.local",
            ".env.production",
            "secrets.yaml",
            "config/secrets.yaml",
            "credentials.json",
            "aws/credentials",
            "prod.tfvars",
            "terraform.tfstate",
            "terraform.tfstate.backup",
            "private.key",
            "server.pem",
            "keystore.jks",
            "cert.p12",
            "id_rsa",
            "id_ed25519",
        )

        val raw =
            SafeProjectContext(
                projectName = "prompt-whisperer",
                safeFiles = blockedFiles + listOf("src/Main.kt"),
                smallFileContents = blockedFiles.associateWith { "sensitive" } + mapOf("README.md" to "safe"),
                excluded = emptyList(),
            )

        val result = service.filterContext(raw)

        blockedFiles.forEach { path ->
            assertFalse(result.safeFiles.contains(path), "Blocked file should not be in safe file list: $path")
            assertFalse(result.smallFileContents.containsKey(path), "Blocked file content should not be included: $path")
            assertTrue(result.excluded.contains(path), "Blocked file should be tracked in excluded list: $path")
        }

        assertTrue(result.safeFiles.contains("src/Main.kt"))
        assertTrue(result.smallFileContents.containsKey("README.md"))
    }

    @Test
    fun `safe files stay available for prompt context`() {
        val safeFiles =
            listOf(
                "README.md",
                "build.gradle.kts",
                "package.json",
                "docs/ARCHITECTURE.md",
            )

        val raw =
            SafeProjectContext(
                projectName = "prompt-whisperer",
                safeFiles = safeFiles,
                smallFileContents = safeFiles.associateWith { "safe" },
                excluded = emptyList(),
            )

        val result = service.filterContext(raw)

        assertEquals(safeFiles.sorted(), result.safeFiles)
        safeFiles.forEach { path ->
            assertTrue(result.smallFileContents.containsKey(path), "Safe file should remain in context: $path")
        }
        assertTrue(result.excluded.isEmpty())
    }
}
