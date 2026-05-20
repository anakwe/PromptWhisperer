package com.promptwhisperer.services

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.models.PromptDepth
import com.promptwhisperer.models.PromptSessionConfig

/**
 * Rule-based inference engine that transforms request+clarifications into planning guidance.
 */
class InferenceEngine {
    /**
     * Computes all reasoning outputs used by synthesis-focused prompt blocks.
     */
    fun analyze(task: String, config: PromptSessionConfig): PromptInference {
        return PromptInference(
            voiceFrame = inferVoiceFrame(config.behaviourProfile),
            voiceInstructions = inferVoiceInstructions(config.behaviourProfile),
            conflicts = detectConflicts(config),
            architectureConcerns = inferArchitectureConcerns(task, config),
            operationalConcerns = inferOperationalConcerns(task, config),
            securityConcerns = inferSecurityConcerns(task, config),
            tradeoffs = inferTradeoffs(task, config),
            deliveryPriorities = inferDeliveryPriorities(task, config),
            recommendedArchitecture = inferRecommendedArchitecture(task, config)
        )
    }

    /**
     * Detects tensions between selected profile and prompt depth.
     */
    fun detectConflicts(config: PromptSessionConfig): List<String> {
        val conflicts = mutableListOf<String>()
        val profile = config.behaviourProfile
        val depth = config.promptDepth

        if (profile == BehaviourProfile.RAPID_PROTOTYPE && depth == PromptDepth.ENTERPRISE) {
            conflicts.add(
                "The selected Behaviour Profile prioritises rapid MVP delivery, while Enterprise depth introduces governance and operational rigor."
            )
        }
        if (profile == BehaviourProfile.MINIMALIST && depth == PromptDepth.ENTERPRISE) {
            conflicts.add(
                "Minimalist profile favors lean implementation, but Enterprise depth expects expanded architecture, controls, and documentation."
            )
        }
        if (profile == BehaviourProfile.SECURITY_ENGINEER && depth == PromptDepth.MINIMAL) {
            conflicts.add(
                "Security Engineer profile expects strong safeguards, while Minimal depth may under-specify validation and threat controls."
            )
        }
        return conflicts
    }

    /**
     * Infers architecture implications from task semantics and clarification answers.
     */
    fun inferArchitectureConcerns(task: String, config: PromptSessionConfig): List<String> {
        val concerns = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)
        val profile = config.behaviourProfile

        val frontendApproach = answers["q_frontend_approach"]
        if (frontendApproach != null && frontendApproach.contains("plain html", ignoreCase = true)) {
            concerns.add("Prefer lightweight browser-native implementation and avoid introducing frontend frameworks unless technically justified.")
            concerns.add("Keep deployment compatible with static hosting by minimizing runtime dependencies.")
        }

        if (lowerTask.contains("flappy") || lowerTask.contains("game")) {
            concerns.add("Use a deterministic game loop and stable collision timing to keep gameplay consistent across devices.")
            concerns.add("Account for touch + keyboard input abstractions so control handling remains coherent.")
            concerns.add("Consider canvas rendering performance and frame budget on mobile browsers.")
        }

        if (lowerTask.contains("facebook")) {
            concerns.add("Plan Facebook App registration and callback configuration early to avoid blocked auth integration later.")
            concerns.add("Verify compatibility with in-app browsers where Facebook sessions may behave differently.")
        }

        if (answers["q_score_tracking"]?.contains("local", ignoreCase = true) == true) {
            concerns.add("Use browser-local persistence only and avoid backend database infrastructure in MVP scope.")
        }

        when (profile) {
            BehaviourProfile.RAPID_PROTOTYPE -> {
                concerns.add(0, "Given the Rapid Prototype profile, prioritize a shippable vertical slice before adding optional abstractions.")
            }
            BehaviourProfile.SENIOR_ARCHITECT -> {
                concerns.add(0, "Define clear module boundaries first so future growth does not force broad refactors.")
            }
            BehaviourProfile.ENTERPRISE_CONSULTANT -> {
                concerns.add(0, "Capture architecture decisions in a concise ADR-style note to support governance and handover.")
            }
            else -> Unit
        }

        return concerns.distinct()
    }

    /**
     * Infers operability and delivery-environment concerns.
     */
    fun inferOperationalConcerns(task: String, config: PromptSessionConfig): List<String> {
        val concerns = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)

        if (answers["q_docs"]?.contains("deployment", ignoreCase = true) == true) {
            concerns.add("Include deployment and runbook instructions so implementation can be reproduced reliably.")
        }
        if (answers["q_environment"]?.isNotBlank() == true && answers["q_environment"] != "Not specified") {
            concerns.add("Target environment is ${answers["q_environment"]}; keep configuration and assumptions explicit for that stage.")
        }
        if (lowerTask.contains("game") || lowerTask.contains("web")) {
            concerns.add("Define fallback behavior for low-performance devices to keep core interactions responsive.")
        }
        if (config.behaviourProfile == BehaviourProfile.DEVOPS_SRE) {
            concerns.add(0, "Treat observability and rollback readiness as first-class acceptance criteria, not post-release tasks.")
        }
        return concerns.distinct()
    }

    /**
     * Infers security boundaries and safeguards relevant to the request.
     */
    fun inferSecurityConcerns(task: String, config: PromptSessionConfig): List<String> {
        val concerns = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)

        if (config.behaviourProfile == BehaviourProfile.SECURITY_ENGINEER || lowerTask.contains("auth") || lowerTask.contains("facebook")) {
            concerns.add("Model trust boundaries before coding and identify where tokens, sessions, and identities can be abused.")
            concerns.add("Apply least-privilege principles for any requested scopes or API permissions.")
            concerns.add("Treat all client-supplied values as untrusted and validate before use.")
        }

        if (answers["q_compliance"]?.isNotBlank() == true && answers["q_compliance"] != "None") {
            concerns.add("Compliance expectations include ${answers["q_compliance"]}; preserve evidence-oriented logging and documentation decisions.")
        }
        return concerns.distinct()
    }

    /**
     * Produces explicit trade-off statements for major implementation choices.
     */
    fun inferTradeoffs(task: String, config: PromptSessionConfig): List<String> {
        val tradeoffs = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)

        if (answers["q_score_tracking"]?.contains("local", ignoreCase = true) == true) {
            tradeoffs.add("A local-only leaderboard keeps implementation and deployment simple, but does not provide cross-device persistence or competitive integrity.")
        }
        if (answers["q_frontend_approach"]?.contains("plain html", ignoreCase = true) == true) {
            tradeoffs.add("A plain HTML/CSS/JavaScript approach minimizes framework overhead, but may require more manual structure for long-term maintainability.")
        }
        if (config.behaviourProfile == BehaviourProfile.RAPID_PROTOTYPE) {
            tradeoffs.add("Prioritizing rapid iteration accelerates delivery, but hardening and architectural refinement should be scheduled explicitly.")
        }
        if (config.behaviourProfile == BehaviourProfile.SECURITY_ENGINEER) {
            tradeoffs.add("Security-first controls reduce attack surface early, but can increase implementation effort in the first milestone.")
        }
        if (config.promptDepth == PromptDepth.ENTERPRISE && (lowerTask.contains("mvp") || config.behaviourProfile == BehaviourProfile.RAPID_PROTOTYPE)) {
            tradeoffs.add("Enterprise-level documentation and controls improve handover quality, but can slow MVP delivery if applied too early.")
        }
        return tradeoffs.distinct()
    }

    /**
     * Produces an ordered implementation sequence weighted by profile and depth.
     */
    fun inferDeliveryPriorities(task: String, config: PromptSessionConfig): List<String> {
        val lowerTask = task.lowercase()
        val base = if (lowerTask.contains("flappy") || lowerTask.contains("game")) {
            mutableListOf(
                "Core gameplay loop",
                "Input handling (keyboard/touch)",
                "Collision and scoring system",
                "Score persistence strategy",
                "Mobile responsiveness and performance",
                "Authentication/integration flow",
                "UI polish and accessibility",
                "Sound and optional enhancements"
            )
        } else {
            mutableListOf(
                "Core feature implementation",
                "Validation and error handling",
                "Testing coverage",
                "Operational and deployment readiness",
                "Documentation updates"
            )
        }

        when (config.behaviourProfile) {
            BehaviourProfile.RAPID_PROTOTYPE -> {
                base.remove("Core gameplay loop")
                base.add(0, "Core gameplay loop")
                base.remove("Input handling (keyboard/touch)")
                base.add(1, "Input handling (keyboard/touch)")
                base.add(2, "Deliver playable MVP slice before optional architecture hardening")
            }
            BehaviourProfile.SECURITY_ENGINEER -> {
                base.add(1, "Threat boundary review and least-privilege checks")
                base.add(3, "Session/auth validation and misuse-case tests")
            }
            BehaviourProfile.ENTERPRISE_CONSULTANT -> {
                base.add(2, "Architecture decision records and governance checkpoints")
                base.add(4, "Operational readiness and rollout plan")
            }
            BehaviourProfile.DEVOPS_SRE -> {
                base.add(2, "Observability and rollback path")
            }
            else -> Unit
        }

        if (config.promptDepth == PromptDepth.MINIMAL) {
            return base.take(5).distinct()
        }

        return base.distinct()
    }

    private fun inferRecommendedArchitecture(task: String, config: PromptSessionConfig): List<String> {
        val architecture = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)

        val frontendApproach = answers["q_frontend_approach"]
        val scoreTracking = answers["q_score_tracking"]

        if (frontendApproach != null && frontendApproach.isNotBlank() && frontendApproach != "Not specified") {
            architecture.add("Frontend: $frontendApproach")
        } else if (lowerTask.contains("game")) {
            architecture.add("Frontend: lightweight Canvas-based game loop")
        } else {
            architecture.add("Frontend: align with existing project conventions")
        }

        if (lowerTask.contains("facebook")) {
            architecture.add("Authentication: Facebook Login SDK with explicit callback handling")
        }

        if (scoreTracking != null && scoreTracking.contains("local", ignoreCase = true)) {
            architecture.add("Persistence: browser local storage for MVP scoreboard")
            architecture.add("Leaderboard: local-only implementation, no backend dependency in initial delivery")
        }

        architecture.add("Hosting: static-compatible deployment unless server capabilities are explicitly requested")

        return architecture.distinct()
    }

    private fun inferVoiceFrame(profile: BehaviourProfile): String = when (profile) {
        BehaviourProfile.SECURITY_ENGINEER -> "Threat-model-first architect"
        BehaviourProfile.RAPID_PROTOTYPE -> "MVP delivery coach"
        BehaviourProfile.ENTERPRISE_CONSULTANT -> "Governance-aware architecture consultant"
        BehaviourProfile.DEVOPS_SRE -> "Reliability-first delivery engineer"
        BehaviourProfile.SENIOR_ARCHITECT -> "Long-horizon system designer"
        BehaviourProfile.TROUBLESHOOTER -> "Evidence-first root cause investigator"
        BehaviourProfile.TEACHING_MODE -> "Mentor engineer explaining trade-offs"
        BehaviourProfile.MINIMALIST -> "Lean implementation strategist"
        BehaviourProfile.BALANCED_ENGINEER -> "Pragmatic senior engineer"
    }

    private fun inferVoiceInstructions(profile: BehaviourProfile): List<String> = when (profile) {
        BehaviourProfile.SECURITY_ENGINEER -> listOf(
            "Lead with threat boundaries, abuse cases, and trust assumptions.",
            "Prefer secure defaults before convenience.",
            "Demand explicit validation and permission scope decisions."
        )
        BehaviourProfile.RAPID_PROTOTYPE -> listOf(
            "Prioritize a playable, testable MVP slice first.",
            "Avoid abstractions that do not accelerate first delivery.",
            "Defer hardening work explicitly rather than silently ignoring it."
        )
        BehaviourProfile.ENTERPRISE_CONSULTANT -> listOf(
            "Make governance and maintainability visible in the plan.",
            "Treat rollout and operational controls as design inputs.",
            "Document decisions for teams beyond the original implementer."
        )
        BehaviourProfile.DEVOPS_SRE -> listOf(
            "Bias towards operability, reliability, and safe rollback.",
            "Make observability an explicit design requirement."
        )
        else -> listOf("Keep reasoning explicit and actionable.")
    }

    private fun answers(questions: List<ClarificationQuestion>): Map<String, String> {
        return questions.associate { it.id to it.resolvedAnswer() }
    }
}

/**
 * Aggregated inference payload consumed by prompt blocks.
 */
data class PromptInference(
    val voiceFrame: String,
    val voiceInstructions: List<String>,
    val conflicts: List<String>,
    val architectureConcerns: List<String>,
    val operationalConcerns: List<String>,
    val securityConcerns: List<String>,
    val tradeoffs: List<String>,
    val deliveryPriorities: List<String>,
    val recommendedArchitecture: List<String>
)

