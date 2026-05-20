package com.promptwhisperer.models

/**
 * User-selected workflow mode.
 */
enum class PromptMode(val displayName: String, val description: String) {
    STANDARD("Standard Mode", "Balanced implementation planning."),
    ARCHITECTURE("Architecture Mode", "Architecture-first decomposition and trade-offs."),
    DEBUGGING("Debugging Mode", "Evidence-driven diagnosis and fix strategy."),
    SECURITY_REVIEW("Security Review Mode", "Threat-aware implementation and secure defaults."),
    TEST_GENERATION("Test Generation Mode", "Implementation prompts with testing emphasis."),
    TROUBLESHOOTING("Troubleshooting Mode", "Failure analysis and remediation guidance"),
    ;

    override fun toString(): String = displayName
}

/**
 * Clarification categories shown in the UI and prompt output.
 */
enum class ClarificationCategory(val displayName: String) {
    REQUIREMENTS("Requirements"),
    ARCHITECTURE("Architecture"),
    SECURITY("Security"),
    TESTING("Testing"),
    OPERATIONS("Operations"),
    DOCUMENTATION("Documentation"),
    UX("UX"),
    DEPLOYMENT("Deployment"),
}

/**
 * Clarification question for request analysis phase.
 */
data class ClarificationQuestion(
    val id: String,
    val question: String,
    val category: ClarificationCategory,
    var answer: String = "",
    val suggestedAnswers: List<String> = emptyList(),
    val defaultAnswer: String? = null,
) {
    fun resolvedAnswer(): String = answer.trim().ifBlank { "Not specified" }
}

/**
 * Request analysis result after clarifications are gathered.
 */
data class RequestAnalysis(
    val originalRequest: String,
    val clarificationQuestions: List<ClarificationQuestion>,
    val inferredConcerns: List<String> = emptyList(),
    val suggestedFramework: String? = null,
    val suggestedArchitecture: String? = null,
)

/**
 * Configuration state for a prompt generation session.
 */
data class PromptSessionConfig(
    val promptMode: PromptMode = PromptMode.STANDARD,
    val behaviourProfile: BehaviourProfile = BehaviourProfile.BALANCED_ENGINEER,
    val promptDepth: PromptDepth = PromptDepth.STANDARD,
    val enabledGuardrails: List<Guardrail> = Guardrail.all().filter { it.enabled },
    val requestAnalysis: RequestAnalysis? = null,
    val clarificationQuestions: List<ClarificationQuestion> = emptyList(),
)
