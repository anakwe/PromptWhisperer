package com.promptwhisperer.services

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.ClarificationCategory
import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.models.PromptMode
import com.promptwhisperer.models.RequestAnalysis

/**
 * Generates context-aware clarification questions based on a raw request.
 */
interface ClarificationService {
    fun generateClarifications(
        request: String,
        profile: BehaviourProfile = BehaviourProfile.default(),
        mode: PromptMode = PromptMode.STANDARD,
    ): List<ClarificationQuestion>

    fun analyzeRequest(
        request: String,
        answers: Map<String, String>,
    ): RequestAnalysis
}

class ClarificationServiceImpl : ClarificationService {
    override fun generateClarifications(
        request: String,
        profile: BehaviourProfile,
        mode: PromptMode,
    ): List<ClarificationQuestion> {
        val lower = request.lowercase()
        val questions = mutableListOf<ClarificationQuestion>()

        questions.add(
            ClarificationQuestion(
                id = "q_scope",
                question = "What delivery scope do you want?",
                category = ClarificationCategory.REQUIREMENTS,
                suggestedAnswers = listOf("Prototype", "Production-ready", "MVP then harden"),
                defaultAnswer = "Production-ready",
            ),
        )

        if (isWebOrGameRequest(lower)) {
            questions.add(
                ClarificationQuestion(
                    id = "q_frontend_approach",
                    question = "Which frontend framework or approach should be used?",
                    category = ClarificationCategory.ARCHITECTURE,
                    suggestedAnswers = listOf("Plain HTML/CSS/JS", "React", "Vue", "Svelte", "No preference"),
                    defaultAnswer = "No preference",
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_responsive",
                    question = "Should the app be mobile responsive?",
                    category = ClarificationCategory.UX,
                    suggestedAnswers = listOf("Yes", "No", "Not specified"),
                    defaultAnswer = "Yes",
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_controls",
                    question = "Should keyboard controls, touch controls, or both be supported?",
                    category = ClarificationCategory.UX,
                    suggestedAnswers = listOf("Keyboard", "Touch", "Both"),
                    defaultAnswer = "Both",
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_score_tracking",
                    question = "Should score tracking be local-only or persisted?",
                    category = ClarificationCategory.REQUIREMENTS,
                    suggestedAnswers = listOf("Local-only", "Persisted leaderboard", "Not needed"),
                    defaultAnswer = "Local-only",
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_sound",
                    question = "Should sound effects be included?",
                    category = ClarificationCategory.UX,
                    suggestedAnswers = listOf("Yes", "No", "Optional"),
                    defaultAnswer = "Optional",
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_accessibility",
                    question = "Should accessibility considerations be included?",
                    category = ClarificationCategory.UX,
                    suggestedAnswers = listOf("Yes", "No", "Basic support"),
                    defaultAnswer = "Basic support",
                ),
            )
        }

        if (mode == PromptMode.SECURITY_REVIEW || isSecuritySensitive(lower) || profile == BehaviourProfile.SECURITY_ENGINEER) {
            questions.add(
                ClarificationQuestion(
                    id = "q_security_asset",
                    question = "What asset or behaviour is being protected?",
                    category = ClarificationCategory.SECURITY,
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_threat_model",
                    question = "What threat model should be considered?",
                    category = ClarificationCategory.SECURITY,
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_compliance",
                    question = "Are there compliance or audit requirements?",
                    category = ClarificationCategory.SECURITY,
                    suggestedAnswers = listOf("None", "SOC2", "ISO 27001", "GDPR", "Other"),
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_least_privilege",
                    question = "Should least-privilege access be enforced?",
                    category = ClarificationCategory.SECURITY,
                    suggestedAnswers = listOf("Yes", "No", "Not specified"),
                    defaultAnswer = "Yes",
                ),
            )
        }

        if (isInfrastructureRequest(lower) || mode == PromptMode.ARCHITECTURE) {
            questions.add(
                ClarificationQuestion(
                    id = "q_platform",
                    question = "Which cloud/platform is targeted?",
                    category = ClarificationCategory.DEPLOYMENT,
                    suggestedAnswers = listOf("Azure", "AWS", "GCP", "On-prem", "Not specified"),
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_environment",
                    question = "Which environment is this for?",
                    category = ClarificationCategory.OPERATIONS,
                    suggestedAnswers = listOf("Development", "Staging", "Production"),
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_standards",
                    question = "Are there naming/tagging standards or existing modules to follow?",
                    category = ClarificationCategory.ARCHITECTURE,
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_rollback",
                    question = "Should rollback be considered in the plan?",
                    category = ClarificationCategory.OPERATIONS,
                    suggestedAnswers = listOf("Yes", "No", "Not specified"),
                    defaultAnswer = "Yes",
                ),
            )
        }

        if (mode == PromptMode.DEBUGGING || isDebugRequest(lower)) {
            questions.add(
                ClarificationQuestion(
                    id = "q_error",
                    question = "What error message or stack trace is seen?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_recent_change",
                    question = "What changed recently?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_expected",
                    question = "What behaviour was expected?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_actual",
                    question = "What behaviour actually happened?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_repro",
                    question = "Are there reliable reproduction steps?",
                    category = ClarificationCategory.TESTING,
                    suggestedAnswers = listOf("Yes", "No", "Partially"),
                ),
            )
        }

        questions.add(
            ClarificationQuestion(
                id = "q_testing",
                question = "Should tests be added?",
                category = ClarificationCategory.TESTING,
                suggestedAnswers = listOf("Unit tests", "Unit + integration", "No tests", "If framework exists"),
                defaultAnswer = "If framework exists",
            ),
        )
        questions.add(
            ClarificationQuestion(
                id = "q_docs",
                question = "Should deployment or usage instructions be included?",
                category = ClarificationCategory.DOCUMENTATION,
                suggestedAnswers = listOf("README only", "README + deployment guide", "No docs"),
                defaultAnswer = "README + deployment guide",
            ),
        )

        return questions.distinctBy { it.id }
    }

    override fun analyzeRequest(
        request: String,
        answers: Map<String, String>,
    ): RequestAnalysis {
        val lower = request.lowercase()

        val inferredConcerns = mutableListOf<String>()
        if (isSecuritySensitive(lower)) inferredConcerns.add("security")
        if (isPerformanceCritical(lower)) inferredConcerns.add("performance")
        if (isComplexData(lower)) inferredConcerns.add("data-model-design")
        if (isDistributed(lower)) inferredConcerns.add("concurrency")

        val suggestedFramework = inferFramework(answers)
        val suggestedArchitecture = inferArchitecture(answers)

        return RequestAnalysis(
            originalRequest = request,
            clarificationQuestions = generateClarifications(request),
            inferredConcerns = inferredConcerns,
            suggestedFramework = suggestedFramework,
            suggestedArchitecture = suggestedArchitecture,
        )
    }

    private fun inferFramework(answers: Map<String, String>): String? {
        val framework = answers["q_frontend_approach"] ?: answers["q_framework"]
        return when (framework) {
            "React" -> "React (JavaScript/TypeScript)"
            "Vue" -> "Vue 3 (JavaScript/TypeScript)"
            "Svelte" -> "Svelte (JavaScript/TypeScript)"
            "Plain HTML/CSS/JS" -> "Vanilla JavaScript"
            else -> null
        }
    }

    private fun inferArchitecture(answers: Map<String, String>): String? {
        val deployment = answers["q_platform"] ?: answers["q_deployment"]
        val testing = answers["q_testing"]

        return when {
            deployment?.contains("kubernetes", ignoreCase = true) == true -> "Microservices with containerized deployment"
            deployment?.contains("serverless", ignoreCase = true) == true -> "Serverless/FaaS architecture"
            testing?.contains("integration", ignoreCase = true) == true -> "Layered architecture (UI / API / Data)"
            else -> null
        }
    }

    private fun isWebOrGameRequest(lower: String): Boolean =
        lower.contains("web") || lower.contains("website") || lower.contains("frontend") || lower.contains("game") || lower.contains("flappy")

    private fun isInfrastructureRequest(lower: String): Boolean =
        lower.contains("infra") || lower.contains("terraform") || lower.contains("kubernetes") || lower.contains("cloud") || lower.contains("deploy")

    private fun isDebugRequest(lower: String): Boolean =
        lower.contains("debug") || lower.contains("error") || lower.contains("failure") || lower.contains("broken") || lower.contains("stack trace")

    private fun isSecuritySensitive(lower: String): Boolean =
        lower.contains("auth") || lower.contains("secure") || lower.contains("payment") || lower.contains("sensitive")

    private fun isPerformanceCritical(lower: String): Boolean =
        lower.contains("real-time") || lower.contains("fast") || lower.contains("performance") || lower.contains("latency")

    private fun isComplexData(lower: String): Boolean =
        lower.contains("data") || lower.contains("database") || lower.contains("model") || lower.contains("graph")

    private fun isDistributed(lower: String): Boolean =
        lower.contains("distributed") || lower.contains("concurrent") || lower.contains("async") || lower.contains("multi-thread")
}
