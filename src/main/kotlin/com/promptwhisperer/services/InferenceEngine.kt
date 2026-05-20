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
    fun analyze(
        task: String,
        config: PromptSessionConfig,
    ): PromptInference {
        return PromptInference(
            voiceFrame = inferVoiceFrame(config.behaviourProfile),
            voiceInstructions = inferVoiceInstructions(config.behaviourProfile),
            conflicts = detectConflicts(task, config),
            clarificationGuidance = inferClarificationGuidance(task, config),
            architectureConcerns = inferArchitectureConcerns(task, config),
            operationalConcerns = inferOperationalConcerns(task, config),
            securityConcerns = inferSecurityConcerns(task, config),
            tradeoffs = inferTradeoffs(task, config),
            deliveryPriorities = inferDeliveryPriorities(task, config),
            recommendedArchitecture = inferRecommendedArchitecture(task, config),
        )
    }

    /**
     * Detects tensions between original request intent and clarification/session constraints.
     */
    fun detectConflicts(
        task: String,
        config: PromptSessionConfig,
    ): List<ConflictInsight> {
        val conflicts = mutableListOf<ConflictInsight>()
        val profile = config.behaviourProfile
        val depth = config.promptDepth
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)

        val scoreTracking = answers["q_score_tracking"].orEmpty()
        if (containsAny(lowerTask, "persisted", "persistent", "leaderboard", "backend", "database") &&
            containsAny(scoreTracking.lowercase(), "local", "local-only")
        ) {
            conflicts.add(
                ConflictInsight(
                    summary =
                        "The initial request points to persisted/backend leaderboard functionality, but clarifications specify local-only score tracking.",
                    recommendations =
                        listOf(
                            "Implement local browser persistence for the MVP.",
                            "Do not add backend leaderboard persistence unless explicitly reconfirmed.",
                            "Document backend persistence as a future enhancement.",
                        ),
                ),
            )
        }

        val scopeAnswer = answers["q_scope"].orEmpty().lowercase()
        if (containsAny(lowerTask, "production", "production-ready", "hardened", "enterprise-grade") &&
            containsAny(scopeAnswer, "prototype", "rapid", "mvp")
        ) {
            conflicts.add(
                ConflictInsight(
                    summary =
                        "The request asks for production-ready delivery, while clarification scope prefers prototype/MVP speed.",
                    recommendations =
                        listOf(
                            "Deliver a narrow MVP slice first with explicit production gaps.",
                            "Treat security hardening, resilience, and observability as a planned follow-up phase.",
                            "Capture production-readiness criteria that must be met before release.",
                        ),
                ),
            )
        }

        val frontendApproach = answers["q_frontend_approach"].orEmpty().lowercase()
        val suggestedFramework = config.requestAnalysis?.suggestedFramework.orEmpty().lowercase()
        val noFrameworkRequested =
            containsAny(frontendApproach, "plain html", "no framework", "vanilla", "none") ||
                containsAny(lowerTask, "no framework", "vanilla js", "plain javascript")
        val frameworkRecommended = suggestedFramework.isNotBlank() && !containsAny(suggestedFramework, "vanilla")
        if (noFrameworkRequested && frameworkRecommended) {
            conflicts.add(
                ConflictInsight(
                    summary =
                        "Clarifications prefer no framework, but analysis recommends ${config.requestAnalysis?.suggestedFramework}.",
                    recommendations =
                        listOf(
                            "Follow the no-framework preference for the initial implementation.",
                            "Only introduce a framework if concrete requirements justify the added complexity.",
                            "Note the recommended framework as an optional future refactor path.",
                        ),
                ),
            )
        }

        if (profile == BehaviourProfile.RAPID_PROTOTYPE && depth == PromptDepth.ENTERPRISE) {
            conflicts.add(
                ConflictInsight(
                    summary =
                        "The selected Behaviour Profile prioritises rapid MVP delivery, while Enterprise depth introduces governance and operational rigor.",
                    recommendations =
                        listOf(
                            "Keep first delivery focused on a working vertical slice.",
                            "Add lightweight documentation and governance checkpoints without blocking implementation.",
                            "Separate immediate build tasks from enterprise hardening backlog.",
                        ),
                ),
            )
        }
        if (profile == BehaviourProfile.MINIMALIST && depth == PromptDepth.ENTERPRISE) {
            conflicts.add(
                ConflictInsight(
                    summary =
                        "Minimalist profile favors lean implementation, but Enterprise depth expects expanded architecture, controls, and documentation.",
                    recommendations =
                        listOf(
                            "Implement the smallest enterprise-safe increment first.",
                            "Prioritize mandatory controls and defer optional process overhead.",
                            "Document what is intentionally deferred to maintain momentum.",
                        ),
                ),
            )
        }
        if (profile == BehaviourProfile.SECURITY_ENGINEER && depth == PromptDepth.MINIMAL) {
            conflicts.add(
                ConflictInsight(
                    summary =
                        "Security Engineer profile expects strong safeguards, while Minimal depth may under-specify validation and threat controls.",
                    recommendations =
                        listOf(
                            "Preserve minimal output size but keep critical threat and validation guidance explicit.",
                            "Include must-have security checks even if broader architecture detail is reduced.",
                        ),
                ),
            )
        }

        val authRequested = containsAny(lowerTask, "auth", "authentication", "login", "oauth", "facebook")
        val threatModelAnswer = answers["q_threat_model"]
        if (authRequested && (threatModelAnswer == null || threatModelAnswer.equals("Not specified", ignoreCase = true))) {
            conflicts.add(
                ConflictInsight(
                    summary =
                        "Authentication is requested, but no threat model is specified in clarifications.",
                    recommendations =
                        listOf(
                            "Assume a conservative baseline threat model for initial implementation.",
                            "Limit auth scope and permissions to least privilege by default.",
                            "Flag threat model definition as a required follow-up before production rollout.",
                        ),
                ),
            )
        }

        return conflicts
    }

    /**
     * Converts concrete clarification answers into implementation planning guidance.
     */
    fun inferClarificationGuidance(
        task: String,
        config: PromptSessionConfig,
    ): List<String> {
        val guidance = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)

        val scoreTracking = answers["q_score_tracking"].orEmpty().lowercase()
        if (containsAny(scoreTracking, "local", "local-only")) {
            guidance.add("Treat leaderboard persistence as local-only for MVP and avoid backend database/storage services.")
            guidance.add("Use browser-local persistence (for example localStorage or IndexedDB) behind a small persistence adapter.")
            guidance.add("Document that local-only scores do not provide cross-device persistence or strong competitive integrity in MVP.")
        }

        val controls = answers["q_controls"].orEmpty().lowercase()
        if (containsAny(controls, "both") || (containsAny(controls, "keyboard") && containsAny(controls, "touch"))) {
            guidance.add("Implement an input abstraction layer so gameplay actions map consistently across keyboard and touch events.")
            guidance.add("Define explicit acceptance tests for desktop keyboard flows and mobile touch flows, including pause/restart interactions.")
        }

        val facebookAuthRequested =
            containsAny(lowerTask, "facebook auth", "facebook login") ||
                (containsAny(lowerTask, "facebook") && containsAny(lowerTask, "auth", "authentication", "login"))
        if (facebookAuthRequested) {
            guidance.add("Integrate with Facebook Login SDK and define callback/redirect handling before wiring app-specific session state.")
            guidance.add("Minimize requested Facebook scopes to least privilege and justify any non-basic permission in documentation.")
        }

        val scope = answers["q_scope"].orEmpty().lowercase()
        if (containsAny(scope, "production-ready") || containsAny(lowerTask, "production-ready", "production")) {
            guidance.add("Include deployment notes for target environments with explicit configuration and rollout assumptions.")
            guidance.add("Treat robust error handling and user-visible failure recovery paths as required, not optional polish.")
            guidance.add("Set verification expectations: run automated checks, capture manual smoke tests, and document release-readiness criteria.")
        }

        val docs = answers["q_docs"].orEmpty().lowercase()
        if (containsAny(docs, "readme + deployment guide", "deployment guide")) {
            guidance.add("Documentation deliverables must include an updated README and a deployment guide with prerequisites, run steps, and rollback notes.")
        }

        return guidance.distinct()
    }

    /**
     * Infers architecture implications from task semantics and clarification answers.
     */
    fun inferArchitectureConcerns(
        task: String,
        config: PromptSessionConfig,
    ): List<String> {
        val concerns = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)
        val profile = config.behaviourProfile

        val frontendApproach = answers["q_frontend_approach"]
        if (frontendApproach != null && frontendApproach.contains("plain html", ignoreCase = true)) {
            concerns.add(
                "Prefer lightweight browser-native implementation and avoid introducing frontend frameworks unless technically justified.",
            )
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
                concerns.add(
                    0,
                    "Given the Rapid Prototype profile, prioritize a shippable vertical slice before adding optional abstractions.",
                )
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
    fun inferOperationalConcerns(
        task: String,
        config: PromptSessionConfig,
    ): List<String> {
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
    fun inferSecurityConcerns(
        task: String,
        config: PromptSessionConfig,
    ): List<String> {
        val concerns = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)

        if (config.behaviourProfile == BehaviourProfile.SECURITY_ENGINEER || lowerTask.contains("auth") || lowerTask.contains("facebook")) {
            concerns.add("Model trust boundaries before coding and identify where tokens, sessions, and identities can be abused.")
            concerns.add("Apply least-privilege principles for any requested scopes or API permissions.")
            concerns.add("Treat all client-supplied values as untrusted and validate before use.")
        }

        if (answers["q_compliance"]?.isNotBlank() == true && answers["q_compliance"] != "None") {
            concerns.add(
                "Compliance expectations include ${answers["q_compliance"]}; preserve evidence-oriented logging and documentation decisions.",
            )
        }
        return concerns.distinct()
    }

    /**
     * Produces explicit trade-off statements for major implementation choices.
     */
    fun inferTradeoffs(
        task: String,
        config: PromptSessionConfig,
    ): List<String> {
        val tradeoffs = mutableListOf<String>()
        val lowerTask = task.lowercase()
        val answers = answers(config.clarificationQuestions)

        if (answers["q_score_tracking"]?.contains("local", ignoreCase = true) == true) {
            tradeoffs.add(
                "A local-only leaderboard keeps implementation and deployment simple, but does not provide cross-device persistence or competitive integrity.",
            )
        }
        if (answers["q_frontend_approach"]?.contains("plain html", ignoreCase = true) == true) {
            tradeoffs.add(
                "A plain HTML/CSS/JavaScript approach minimizes framework overhead, but may require more manual structure for long-term maintainability.",
            )
        }
        if (config.behaviourProfile == BehaviourProfile.RAPID_PROTOTYPE) {
            tradeoffs.add(
                "Prioritizing rapid iteration accelerates delivery, but hardening and architectural refinement should be scheduled explicitly.",
            )
        }
        if (config.behaviourProfile == BehaviourProfile.SECURITY_ENGINEER) {
            tradeoffs.add(
                "Security-first controls reduce attack surface early, but can increase implementation effort in the first milestone.",
            )
        }
        if (config.promptDepth == PromptDepth.ENTERPRISE && (lowerTask.contains("mvp") || config.behaviourProfile == BehaviourProfile.RAPID_PROTOTYPE)) {
            tradeoffs.add(
                "Enterprise-level documentation and controls improve handover quality, but can slow MVP delivery if applied too early.",
            )
        }
        return tradeoffs.distinct()
    }

    /**
     * Produces an ordered implementation sequence weighted by profile and depth.
     */
    fun inferDeliveryPriorities(
        task: String,
        config: PromptSessionConfig,
    ): List<String> {
        val lowerTask = task.lowercase()
        val base =
            if (lowerTask.contains("flappy") || lowerTask.contains("game")) {
                mutableListOf(
                    "Core gameplay loop",
                    "Input handling (keyboard/touch)",
                    "Collision and scoring system",
                    "Score persistence strategy",
                    "Mobile responsiveness and performance",
                    "Authentication/integration flow",
                    "UI polish and accessibility",
                    "Sound and optional enhancements",
                )
            } else {
                mutableListOf(
                    "Core feature implementation",
                    "Validation and error handling",
                    "Testing coverage",
                    "Operational and deployment readiness",
                    "Documentation updates",
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

    private fun inferRecommendedArchitecture(
        task: String,
        config: PromptSessionConfig,
    ): List<String> {
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

    private fun inferVoiceFrame(profile: BehaviourProfile): String =
        when (profile) {
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

    private fun inferVoiceInstructions(profile: BehaviourProfile): List<String> =
        when (profile) {
            BehaviourProfile.SECURITY_ENGINEER ->
                listOf(
                    "Lead with threat boundaries, abuse cases, and trust assumptions.",
                    "Prefer secure defaults before convenience.",
                    "Demand explicit validation and permission scope decisions.",
                )
            BehaviourProfile.RAPID_PROTOTYPE ->
                listOf(
                    "Prioritize a playable, testable MVP slice first.",
                    "Avoid abstractions that do not accelerate first delivery.",
                    "Defer hardening work explicitly rather than silently ignoring it.",
                )
            BehaviourProfile.ENTERPRISE_CONSULTANT ->
                listOf(
                    "Make governance and maintainability visible in the plan.",
                    "Treat rollout and operational controls as design inputs.",
                    "Document decisions for teams beyond the original implementer.",
                )
            BehaviourProfile.DEVOPS_SRE ->
                listOf(
                    "Bias towards operability, reliability, and safe rollback.",
                    "Make observability an explicit design requirement.",
                )
            else -> listOf("Keep reasoning explicit and actionable.")
        }

    private fun answers(questions: List<ClarificationQuestion>): Map<String, String> {
        return questions.associate { it.id to it.resolvedAnswer() }
    }

    private fun containsAny(
        text: String,
        vararg needles: String,
    ): Boolean = needles.any { needle -> text.contains(needle, ignoreCase = true) }
}

/**
 * Aggregated inference payload consumed by prompt blocks.
 */
data class PromptInference(
    val voiceFrame: String,
    val voiceInstructions: List<String>,
    val conflicts: List<ConflictInsight>,
    val clarificationGuidance: List<String>,
    val architectureConcerns: List<String>,
    val operationalConcerns: List<String>,
    val securityConcerns: List<String>,
    val tradeoffs: List<String>,
    val deliveryPriorities: List<String>,
    val recommendedArchitecture: List<String>,
)

data class ConflictInsight(
    val summary: String,
    val recommendations: List<String>,
)

