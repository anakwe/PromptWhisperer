package com.promptwhisperer.services

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.ClarificationCategory
import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.models.DomainProfile
import com.promptwhisperer.models.PromptMode
import com.promptwhisperer.models.RequestAnalysis
import java.util.logging.Level
import java.util.logging.Logger

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
        profile: BehaviourProfile = BehaviourProfile.default(),
        mode: PromptMode = PromptMode.STANDARD,
    ): RequestAnalysis

    fun getLastSelectionDiagnostics(): List<ClarificationSelectionDiagnostic>
}

data class ClarificationSelectionDiagnostic(
    val questionId: String,
    val question: String,
    val reason: String,
)

class ClarificationServiceImpl : ClarificationService {
    private val logger = Logger.getLogger(ClarificationServiceImpl::class.java.name)
    private val domainProfiler = DomainProfiler()

    @Volatile
    private var lastSelectionDiagnostics: List<ClarificationSelectionDiagnostic> = emptyList()

    override fun getLastSelectionDiagnostics(): List<ClarificationSelectionDiagnostic> = lastSelectionDiagnostics

    override fun generateClarifications(
        request: String,
        profile: BehaviourProfile,
        mode: PromptMode,
    ): List<ClarificationQuestion> {
        val lower = request.lowercase()
        val domainInference = domainProfiler.inferWithConfidence(request)
        val questions = mutableListOf<ClarificationQuestion>()
        val diagnostics = mutableListOf<ClarificationSelectionDiagnostic>()

        val domainQuestionThreshold = 0.65
        val gameQuestionThreshold = 0.8
        val hasWebSignals = isWebRequest(lower)
        val shouldUseGameQuestions =
            domainInference.profile == DomainProfile.GAME && domainInference.confidence >= gameQuestionThreshold
        val shouldUseSecurityQuestions =
            mode == PromptMode.SECURITY_REVIEW ||
                profile == BehaviourProfile.SECURITY_ENGINEER ||
                isSecuritySensitive(lower) ||
                (domainInference.profile == DomainProfile.SECURITY_TOOL && domainInference.confidence >= domainQuestionThreshold)

        fun addQuestion(
            question: ClarificationQuestion,
            selectionReason: String,
        ) {
            questions.add(question)
            val reason =
                "$selectionReason | domainProfile=${domainInference.profile.name} | confidence=${"%.2f".format(domainInference.confidence)}"
            diagnostics.add(
                ClarificationSelectionDiagnostic(
                    questionId = question.id,
                    question = question.question,
                    reason = reason,
                ),
            )
            logger.log(Level.FINE, "Question selected: '${question.question}' Reason: $reason")
        }

        addQuestion(
            ClarificationQuestion(
                id = "q_scope",
                question = "What delivery scope do you want?",
                category = ClarificationCategory.REQUIREMENTS,
                suggestedAnswers = listOf("Prototype", "Production-ready", "MVP then harden"),
                defaultAnswer = "Production-ready",
            ),
            selectionReason = "baseline scope clarification",
        )

        if (hasWebSignals || shouldUseGameQuestions) {
            addQuestion(
                ClarificationQuestion(
                    id = "q_frontend_approach",
                    question = "Which frontend framework or approach should be used?",
                    category = ClarificationCategory.ARCHITECTURE,
                    suggestedAnswers = listOf("Plain HTML/CSS/JS", "React", "Vue", "Svelte", "No preference"),
                    defaultAnswer = "No preference",
                ),
                selectionReason = "web or interactive UI signal detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_responsive",
                    question = "Should the app be mobile responsive?",
                    category = ClarificationCategory.UX,
                    suggestedAnswers = listOf("Yes", "No", "Not specified"),
                    defaultAnswer = "Yes",
                ),
                selectionReason = "web or interactive UI signal detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_accessibility",
                    question = "Should accessibility considerations be included?",
                    category = ClarificationCategory.UX,
                    suggestedAnswers = listOf("Yes", "No", "Basic support"),
                    defaultAnswer = "Basic support",
                ),
                selectionReason = "web or interactive UI signal detected",
            )
        }

        if (shouldUseGameQuestions) {
            addQuestion(
                ClarificationQuestion(
                    id = "q_controls",
                    question = "Should keyboard controls, touch controls, or both be supported?",
                    category = ClarificationCategory.UX,
                    suggestedAnswers = listOf("Keyboard", "Touch", "Both"),
                    defaultAnswer = "Both",
                ),
                selectionReason = "game-specific clarification selected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_score_tracking",
                    question = "Should score tracking be local-only or persisted?",
                    category = ClarificationCategory.REQUIREMENTS,
                    suggestedAnswers = listOf("Local-only", "Persisted leaderboard", "Not needed"),
                    defaultAnswer = "Local-only",
                ),
                selectionReason = "game-specific clarification selected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_sound",
                    question = "Should sound effects be included?",
                    category = ClarificationCategory.UX,
                    suggestedAnswers = listOf("Yes", "No", "Optional"),
                    defaultAnswer = "Optional",
                ),
                selectionReason = "game-specific clarification selected",
            )
        } else if (domainInference.profile == DomainProfile.GAME) {
            logger.log(
                Level.INFO,
                "Game-specific clarification questions suppressed because confidence ${"%.2f".format(domainInference.confidence)} is below threshold ${"%.2f".format(gameQuestionThreshold)}.",
            )
        }

        if (shouldUseSecurityQuestions) {
            addQuestion(
                ClarificationQuestion(
                    id = "q_security_asset",
                    question = "What asset or behaviour is being protected?",
                    category = ClarificationCategory.SECURITY,
                ),
                selectionReason = "security signal or security mode/profile detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_threat_model",
                    question = "What threat model should be considered?",
                    category = ClarificationCategory.SECURITY,
                ),
                selectionReason = "security signal or security mode/profile detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_compliance",
                    question = "Are there compliance or audit requirements?",
                    category = ClarificationCategory.SECURITY,
                    suggestedAnswers = listOf("None", "SOC2", "ISO 27001", "GDPR", "Other"),
                ),
                selectionReason = "security signal or security mode/profile detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_least_privilege",
                    question = "Should least-privilege access be enforced?",
                    category = ClarificationCategory.SECURITY,
                    suggestedAnswers = listOf("Yes", "No", "Not specified"),
                    defaultAnswer = "Yes",
                ),
                selectionReason = "security signal or security mode/profile detected",
            )
        }

        if (isInfrastructureRequest(lower) || mode == PromptMode.ARCHITECTURE) {
            addQuestion(
                ClarificationQuestion(
                    id = "q_platform",
                    question = "Which cloud/platform is targeted?",
                    category = ClarificationCategory.DEPLOYMENT,
                    suggestedAnswers = listOf("Azure", "AWS", "GCP", "On-prem", "Not specified"),
                ),
                selectionReason = "infrastructure or architecture mode signal detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_environment",
                    question = "Which environment is this for?",
                    category = ClarificationCategory.OPERATIONS,
                    suggestedAnswers = listOf("Development", "Staging", "Production"),
                ),
                selectionReason = "infrastructure or architecture mode signal detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_standards",
                    question = "Are there naming/tagging standards or existing modules to follow?",
                    category = ClarificationCategory.ARCHITECTURE,
                ),
                selectionReason = "infrastructure or architecture mode signal detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_rollback",
                    question = "Should rollback be considered in the plan?",
                    category = ClarificationCategory.OPERATIONS,
                    suggestedAnswers = listOf("Yes", "No", "Not specified"),
                    defaultAnswer = "Yes",
                ),
                selectionReason = "infrastructure or architecture mode signal detected",
            )
        }

        if (mode == PromptMode.DEBUGGING || isDebugRequest(lower)) {
            addQuestion(
                ClarificationQuestion(
                    id = "q_error",
                    question = "What error message or stack trace is seen?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
                selectionReason = "debug signal or debugging mode detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_recent_change",
                    question = "What changed recently?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
                selectionReason = "debug signal or debugging mode detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_expected",
                    question = "What behaviour was expected?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
                selectionReason = "debug signal or debugging mode detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_actual",
                    question = "What behaviour actually happened?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
                selectionReason = "debug signal or debugging mode detected",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_repro",
                    question = "Are there reliable reproduction steps?",
                    category = ClarificationCategory.TESTING,
                    suggestedAnswers = listOf("Yes", "No", "Partially"),
                ),
                selectionReason = "debug signal or debugging mode detected",
            )
        }

        if (domainInference.confidence < domainQuestionThreshold) {
            addQuestion(
                ClarificationQuestion(
                    id = "q_constraints_generic",
                    question = "Are there mandatory language, runtime, framework, or dependency constraints?",
                    category = ClarificationCategory.ARCHITECTURE,
                ),
                selectionReason = "low domain confidence fallback to neutral engineering clarification",
            )
            addQuestion(
                ClarificationQuestion(
                    id = "q_integrations_generic",
                    question = "Which external systems, interfaces, or data contracts must this integrate with?",
                    category = ClarificationCategory.REQUIREMENTS,
                ),
                selectionReason = "low domain confidence fallback to neutral engineering clarification",
            )
        }

        addQuestion(
            ClarificationQuestion(
                id = "q_testing",
                question = "Should tests be added?",
                category = ClarificationCategory.TESTING,
                suggestedAnswers = listOf("Unit tests", "Unit + integration", "No tests", "If framework exists"),
                defaultAnswer = "If framework exists",
            ),
            selectionReason = "baseline delivery quality clarification",
        )
        addQuestion(
            ClarificationQuestion(
                id = "q_docs",
                question = "Should deployment or usage instructions be included?",
                category = ClarificationCategory.DOCUMENTATION,
                suggestedAnswers = listOf("README only", "README + deployment guide", "No docs"),
                defaultAnswer = "README + deployment guide",
            ),
            selectionReason = "baseline delivery quality clarification",
        )

        val distinctQuestions = questions.distinctBy { it.id }
        lastSelectionDiagnostics = diagnostics.distinctBy { it.questionId }

        logger.log(
            Level.INFO,
            "Clarification generation summary: domain=${domainInference.profile.name} confidence=${"%.2f".format(domainInference.confidence)} questions=${distinctQuestions.size}",
        )

        return distinctQuestions
    }

    override fun analyzeRequest(
        request: String,
        answers: Map<String, String>,
        profile: BehaviourProfile,
        mode: PromptMode,
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
            clarificationQuestions = generateClarifications(request, profile, mode),
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

    private fun isWebRequest(lower: String): Boolean =
        lower.contains("web") ||
            lower.contains("website") ||
            lower.contains("frontend") ||
            lower.contains("ui") ||
            lower.contains("browser")

    private fun isInfrastructureRequest(lower: String): Boolean =
        lower.contains("infra") || lower.contains("terraform") || lower.contains("kubernetes") || lower.contains("cloud") || lower.contains("deploy")

    private fun isDebugRequest(lower: String): Boolean =
        lower.contains("debug") || lower.contains("error") || lower.contains("failure") || lower.contains("broken") || lower.contains("stack trace")

    private fun isSecuritySensitive(lower: String): Boolean =
        lower.contains("auth") ||
            lower.contains("secure") ||
            lower.contains("payment") ||
            lower.contains("sensitive") ||
            lower.contains("siem") ||
            lower.contains("soc") ||
            lower.contains("waf") ||
            lower.contains("threat") ||
            lower.contains("audit") ||
            lower.contains("compliance")

    private fun isPerformanceCritical(lower: String): Boolean =
        lower.contains("real-time") || lower.contains("fast") || lower.contains("performance") || lower.contains("latency")

    private fun isComplexData(lower: String): Boolean =
        lower.contains("data") || lower.contains("database") || lower.contains("model") || lower.contains("graph")

    private fun isDistributed(lower: String): Boolean =
        lower.contains("distributed") || lower.contains("concurrent") || lower.contains("async") || lower.contains("multi-thread")
}
