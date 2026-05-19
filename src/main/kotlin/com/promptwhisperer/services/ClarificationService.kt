package com.promptwhisperer.services

import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.models.RequestAnalysis

/**
 * Generates context-aware clarification questions based on a raw request.
 *
 * This service infers what the user is trying to build and asks targeted questions
 * to resolve ambiguity before prompt generation.
 *
 * Example:
 * User: "Build a flappy bird style web game"
 * Questions:
 * - Preferred framework?
 * - Mobile responsive?
 * - Include sound?
 * - Local-only or backend leaderboard?
 * - Include tests?
 * - Accessibility important?
 */
interface ClarificationService {
    fun generateClarifications(request: String): List<ClarificationQuestion>
    fun analyzeRequest(request: String, answers: Map<String, String>): RequestAnalysis
}

class ClarificationServiceImpl : ClarificationService {
    override fun generateClarifications(request: String): List<ClarificationQuestion> {
        val lower = request.lowercase()
        val questions = mutableListOf<ClarificationQuestion>()

        // Framework/Technology questions
        if (isWebRequest(lower)) {
            questions.add(
                ClarificationQuestion(
                    id = "q_framework",
                    question = "Preferred web framework or stack?",
                    category = "Technology",
                    answers = listOf("React", "Vue", "Svelte", "Plain HTML/CSS/JS", "No preference"),
                    defaultAnswer = "No preference"
                )
            )
        }

        // UI/UX questions
        if (isUIRequest(lower)) {
            questions.add(
                ClarificationQuestion(
                    id = "q_responsive",
                    question = "Mobile responsive design required?",
                    category = "Design",
                    answers = listOf("Yes, mobile-first", "Yes, desktop-first", "Desktop only", "Not specified"),
                    defaultAnswer = "Not specified"
                )
            )
            questions.add(
                ClarificationQuestion(
                    id = "q_accessibility",
                    question = "Accessibility (a11y) important?",
                    category = "Design",
                    answers = listOf("Yes, WCAG AA standard", "Yes, basic support", "Not required", "Unknown"),
                    defaultAnswer = "Unknown"
                )
            )
        }

        // Backend questions
        if (isBackendRequest(lower)) {
            questions.add(
                ClarificationQuestion(
                    id = "q_database",
                    question = "Database preference?",
                    category = "Backend",
                    answers = listOf("PostgreSQL", "MySQL", "MongoDB", "SQLite", "No preference"),
                    defaultAnswer = "No preference"
                )
            )
        }

        // API questions
        if (isAPIRequest(lower)) {
            questions.add(
                ClarificationQuestion(
                    id = "q_api_style",
                    question = "API style?",
                    category = "Backend",
                    answers = listOf("REST", "GraphQL", "gRPC", "No preference"),
                    defaultAnswer = "No preference"
                )
            )
        }

        // Testing questions
        questions.add(
            ClarificationQuestion(
                id = "q_testing",
                question = "Testing requirements?",
                category = "Quality",
                answers = listOf("Unit tests required", "Unit + integration tests", "E2E tests", "No tests", "No preference"),
                defaultAnswer = "No preference"
            )
        )

        // Documentation questions
        questions.add(
            ClarificationQuestion(
                id = "q_documentation",
                question = "Documentation level?",
                category = "Quality",
                answers = listOf("Minimal (README only)", "Moderate (README + code comments)", "Comprehensive (+ API docs)", "No preference"),
                defaultAnswer = "No preference"
            )
        )

        // Deployment questions
        if (isDeployableRequest(lower)) {
            questions.add(
                ClarificationQuestion(
                    id = "q_deployment",
                    question = "Deployment target?",
                    category = "Operations",
                    answers = listOf("Self-hosted", "Docker", "Serverless (AWS Lambda, etc.)", "Docker + Kubernetes", "Local dev only", "No preference"),
                    defaultAnswer = "No preference"
                )
            )
        }

        // Performance/Scale
        if (isScalableRequest(lower)) {
            questions.add(
                ClarificationQuestion(
                    id = "q_scale",
                    question = "Expected scale/load?",
                    category = "Operations",
                    answers = listOf("Single user or small team", "Hundreds of concurrent users", "Thousands+", "Unknown"),
                    defaultAnswer = "Unknown"
                )
            )
        }

        return questions
    }

    override fun analyzeRequest(request: String, answers: Map<String, String>): RequestAnalysis {
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
            suggestedArchitecture = suggestedArchitecture
        )
    }

    private fun inferFramework(answers: Map<String, String>): String? {
        val framework = answers["q_framework"]
        return when (framework) {
            "React" -> "React (JavaScript/TypeScript)"
            "Vue" -> "Vue 3 (JavaScript/TypeScript)"
            "Svelte" -> "Svelte (JavaScript/TypeScript)"
            "Plain HTML/CSS/JS" -> "Vanilla JavaScript"
            else -> null
        }
    }

    private fun inferArchitecture(answers: Map<String, String>): String? {
        val deployment = answers["q_deployment"]
        val testing = answers["q_testing"]

        return if (deployment == "Docker + Kubernetes") {
            "Microservices with containerized deployment"
        } else if (deployment == "Serverless (AWS Lambda, etc.)") {
            "Serverless/FaaS architecture"
        } else if (testing?.contains("E2E") == true) {
            "Layered architecture (UI / API / Data)"
        } else {
            null
        }
    }

    private fun isWebRequest(lower: String): Boolean =
        lower.contains("web") || lower.contains("website") || lower.contains("app") || lower.contains("frontend")

    private fun isUIRequest(lower: String): Boolean =
        lower.contains("ui") || lower.contains("design") || lower.contains("interface") || lower.contains("layout")

    private fun isBackendRequest(lower: String): Boolean =
        lower.contains("backend") || lower.contains("api") || lower.contains("server") || lower.contains("database")

    private fun isAPIRequest(lower: String): Boolean =
        lower.contains("api") || lower.contains("rest") || lower.contains("graphql")

    private fun isDeployableRequest(lower: String): Boolean =
        lower.contains("deploy") || lower.contains("production") || lower.contains("docker") || lower.contains("kubernetes")

    private fun isScalableRequest(lower: String): Boolean =
        lower.contains("scale") || lower.contains("load") || lower.contains("concurrent") || lower.contains("traffic")

    private fun isSecuritySensitive(lower: String): Boolean =
        lower.contains("auth") || lower.contains("secure") || lower.contains("payment") || lower.contains("sensitive")

    private fun isPerformanceCritical(lower: String): Boolean =
        lower.contains("real-time") || lower.contains("fast") || lower.contains("performance") || lower.contains("latency")

    private fun isComplexData(lower: String): Boolean =
        lower.contains("data") || lower.contains("database") || lower.contains("model") || lower.contains("graph")

    private fun isDistributed(lower: String): Boolean =
        lower.contains("distributed") || lower.contains("concurrent") || lower.contains("async") || lower.contains("multi-thread")
}

