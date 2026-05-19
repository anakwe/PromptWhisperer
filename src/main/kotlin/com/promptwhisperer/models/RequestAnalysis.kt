package com.promptwhisperer.models

/**
 * Clarification question for request analysis phase.
 */
data class ClarificationQuestion(
    val id: String,
    val question: String,
    val category: String,
    val answers: List<String>,
    val defaultAnswer: String? = null,
    val userAnswer: String? = null
) {
    fun isAnswered(): Boolean = userAnswer != null
}

/**
 * Request analysis result after clarifications are gathered.
 */
data class RequestAnalysis(
    val originalRequest: String,
    val clarificationQuestions: List<ClarificationQuestion>,
    val inferredConcerns: List<String> = emptyList(),
    val suggestedFramework: String? = null,
    val suggestedArchitecture: String? = null
)

/**
 * Configuration state for a prompt generation session.
 */
data class PromptSessionConfig(
    val behaviourProfile: BehaviourProfile = BehaviourProfile.BALANCED_ENGINEER,
    val promptDepth: PromptDepth = PromptDepth.STANDARD,
    val enabledGuardrails: List<Guardrail> = Guardrail.all().filter { it.enabled },
    val requestAnalysis: RequestAnalysis? = null,
    val clarificationAnswers: Map<String, String> = emptyMap()
)

