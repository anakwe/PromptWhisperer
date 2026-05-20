package com.promptwhisperer

import com.promptwhisperer.services.isWhitelistedSmallFile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContextScannerServiceTests {
    @Test
    fun `scanner whitelist allows known safe metadata files`() {
        val safeFiles =
            listOf(
                "README.md",
                "build.gradle.kts",
                "package.json",
            )

        safeFiles.forEach { file ->
            assertTrue(
                isWhitelistedSmallFile(file),
                "Expected scanner to allow small-file preview for: $file",
            )
        }
    }

    @Test
    fun `scanner whitelist denies secret-like files from small content scanning`() {
        // If these ever return true, scanner may accidentally load sensitive content into prompt context.
        val secretLikeFiles =
            listOf(
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

        secretLikeFiles.forEach { file ->
            assertFalse(
                isWhitelistedSmallFile(file),
                "Expected scanner to skip small-file preview for secret-like file: $file",
            )
        }
    }
}

