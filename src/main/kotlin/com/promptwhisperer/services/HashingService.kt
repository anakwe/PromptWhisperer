package com.promptwhisperer.services

import java.security.MessageDigest

/**
 * Provides SHA-256 hashing used for artefact integrity. Kept small and testable.
 */
interface HashingService {
    fun sha256(input: ByteArray): String
}

class HashingServiceImpl : HashingService {
    override fun sha256(input: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
