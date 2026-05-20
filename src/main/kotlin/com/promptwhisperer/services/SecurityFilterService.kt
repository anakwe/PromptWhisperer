package com.promptwhisperer.services

import java.util.regex.Pattern

/**
 * SecurityFilterService filters file lists and contents according to strict policies.
 * NEVER allow secret files to be read or included.
 */
data class FilterResult(
    val safeFiles: List<String>,
    val smallFileContents: Map<String, String>,
    val excluded: List<String>,
    val projectName: String?,
)

interface SecurityFilterService {
    fun filterContext(raw: SafeProjectContext): FilterResult
}

class SecurityFilterServiceImpl : SecurityFilterService {
    // Blocked filename patterns and globs
    private val blockedPatterns =
        listOf(
            Pattern.compile(".*\\.pem$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.key$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.p12$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.jks$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("id_rsa", Pattern.CASE_INSENSITIVE),
            Pattern.compile("id_ed25519", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.env.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*secrets.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*credentials.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.tfvars$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("terraform.tfstate.*", Pattern.CASE_INSENSITIVE),
        )

    override fun filterContext(raw: SafeProjectContext): FilterResult {
        val safeFiles = mutableListOf<String>()
        val safeContents = mutableMapOf<String, String>()
        val excluded = mutableListOf<String>()
        for (f in raw.safeFiles) {
            if (isBlocked(f)) {
                excluded.add(f)
            } else {
                safeFiles.add(f)
            }
        }
        for ((k, v) in raw.smallFileContents) {
            if (isBlocked(k)) {
                excluded.add(k)
            } else {
                safeContents[k] = v
            }
        }
        return FilterResult(safeFiles.sorted(), safeContents, excluded.sorted(), raw.projectName)
    }

    private fun isBlocked(path: String): Boolean {
        return blockedPatterns.any { it.matcher(path).find() }
    }
}
