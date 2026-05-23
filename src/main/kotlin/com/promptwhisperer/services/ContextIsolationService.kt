package com.promptwhisperer.services

import com.promptwhisperer.models.ContextScope
import com.promptwhisperer.models.DomainProfile

data class EnhancementMemorySnapshot(
    val assumptions: List<String> = emptyList(),
    val taskFingerprint: String? = null,
)

class EnhancementMemoryStore {
    private var sessionMemory = EnhancementMemorySnapshot()
    private val projectMemory = mutableMapOf<String, EnhancementMemorySnapshot>()

    fun read(
        scope: ContextScope,
        projectKey: String,
    ): EnhancementMemorySnapshot =
        when (scope) {
            ContextScope.STATELESS -> EnhancementMemorySnapshot()
            ContextScope.SESSION -> sessionMemory
            ContextScope.PROJECT -> projectMemory[projectKey] ?: EnhancementMemorySnapshot()
            ContextScope.GLOBAL -> globalMemory
        }

    fun write(
        scope: ContextScope,
        projectKey: String,
        snapshot: EnhancementMemorySnapshot,
    ) {
        when (scope) {
            ContextScope.STATELESS -> Unit
            ContextScope.SESSION -> sessionMemory = snapshot
            ContextScope.PROJECT -> projectMemory[projectKey] = snapshot
            ContextScope.GLOBAL -> globalMemory = snapshot
        }
    }

    fun reset(
        scope: ContextScope,
        projectKey: String,
    ) {
        when (scope) {
            ContextScope.STATELESS -> Unit
            ContextScope.SESSION -> sessionMemory = EnhancementMemorySnapshot()
            ContextScope.PROJECT -> projectMemory.remove(projectKey)
            ContextScope.GLOBAL -> globalMemory = EnhancementMemorySnapshot()
        }
    }

    companion object {
        private var globalMemory = EnhancementMemorySnapshot()
    }
}

class DomainProfiler {
    data class DomainInference(
        val profile: DomainProfile,
        val confidence: Double,
        val matchedSignals: List<String>,
    )

    fun infer(task: String): DomainProfile = inferWithConfidence(task).profile

    fun inferWithConfidence(task: String): DomainInference {
        val lowerTask = task.lowercase()
        val domainSignals =
            mapOf(
                DomainProfile.SECURITY_TOOL to listOf("siem", "soc", "waf", "threat", "security", "remediation", "least privilege", "audit"),
                DomainProfile.GAME to listOf("game", "gameplay", "leaderboard", "flappy", "touch controls", "score", "pause", "restart"),
                DomainProfile.PLUGIN to listOf("intellij", "plugin", "jetbrains", "ide"),
                DomainProfile.BACKEND_API to listOf("rest api", "backend", "microservice", "endpoint", "service"),
                DomainProfile.MOBILE_APP to listOf("android", "ios", "mobile app", "mobile"),
                DomainProfile.CLI_TOOL to listOf("cli", "command line", "terminal tool", "shell"),
                DomainProfile.AI_AGENT to listOf("prompt", "assistant", "agent", "copilot", "claude", "cursor"),
            )

        val scoredDomains =
            domainSignals
                .map { (profile, signals) ->
                    val hits = signals.filter { signal -> lowerTask.contains(signal, ignoreCase = true) }
                    Triple(profile, hits.size, hits)
                }
                .sortedByDescending { it.second }

        val best = scoredDomains.firstOrNull()
        if (best == null || best.second == 0) {
            return DomainInference(
                profile = DomainProfile.GENERIC,
                confidence = 0.35,
                matchedSignals = emptyList(),
            )
        }

        val runnerUpHits = scoredDomains.getOrNull(1)?.second ?: 0
        val baseConfidence =
            when (best.second) {
                1 -> 0.66
                2 -> 0.82
                else -> 0.92
            }
        val ambiguityPenalty =
            when {
                runnerUpHits >= best.second -> 0.22
                runnerUpHits == best.second - 1 && best.second > 1 -> 0.1
                else -> 0.0
            }

        return DomainInference(
            profile = best.first,
            confidence = (baseConfidence - ambiguityPenalty).coerceIn(0.35, 0.99),
            matchedSignals = best.third,
        )
    }

    private fun containsAny(
        text: String,
        vararg needles: String,
    ): Boolean = needles.any { needle -> text.contains(needle, ignoreCase = true) }
}

class ContextContaminationDetector(
    private val domainProfiler: DomainProfiler = DomainProfiler(),
) {
    fun detect(
        task: String,
        inheritedAssumptions: List<String>,
    ): List<String> {
        if (inheritedAssumptions.isEmpty()) {
            return emptyList()
        }

        val domain = domainProfiler.infer(task)
        val suspiciousTerms =
            when (domain) {
                DomainProfile.SECURITY_TOOL -> listOf("gameplay", "leaderboard", "touch", "sound", "pause", "restart")
                DomainProfile.GAME -> listOf("soc", "siem", "waf", "compliance evidence")
                DomainProfile.MOBILE_APP -> listOf("leaderboard persistence", "siem", "soc")
                DomainProfile.BACKEND_API -> listOf("gameplay", "touch controls", "sound effects")
                else -> emptyList()
            }

        if (suspiciousTerms.isEmpty()) {
            return emptyList()
        }

        val flattened = inheritedAssumptions.joinToString("\n").lowercase()
        return suspiciousTerms
            .filter { term -> flattened.contains(term.lowercase()) }
            .map { term ->
                "Potential context contamination: inherited term '$term' appears low relevance for inferred domain ${domain.name.lowercase()}."
            }
            .distinct()
    }
}
