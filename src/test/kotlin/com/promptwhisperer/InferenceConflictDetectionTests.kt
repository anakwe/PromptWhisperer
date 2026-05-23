package com.promptwhisperer

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.ClarificationCategory
import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.models.PromptDepth
import com.promptwhisperer.models.PromptSessionConfig
import com.promptwhisperer.models.RequestAnalysis
import com.promptwhisperer.services.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InferenceConflictDetectionTests {
    private val engine = InferenceEngine()

    @Test
    fun `detects production ready versus prototype scope conflict`() {
        val config =
            PromptSessionConfig(
                clarificationQuestions =
                    listOf(
                        ClarificationQuestion(
                            id = "q_scope",
                            question = "What delivery scope do you want?",
                            category = ClarificationCategory.REQUIREMENTS,
                            answer = "Prototype",
                        ),
                    ),
            )

        val conflicts = engine.detectConflicts("Build a production-ready service", config)

        assertTrue(conflicts.any { it.summary.contains("production-ready", ignoreCase = true) })
    }

    @Test
    fun `detects no framework preference versus framework recommendation conflict`() {
        val config =
            PromptSessionConfig(
                requestAnalysis =
                    RequestAnalysis(
                        originalRequest = "Build UI",
                        clarificationQuestions = emptyList(),
                        suggestedFramework = "React (JavaScript/TypeScript)",
                    ),
                clarificationQuestions =
                    listOf(
                        ClarificationQuestion(
                            id = "q_frontend_approach",
                            question = "Which frontend framework or approach should be used?",
                            category = ClarificationCategory.ARCHITECTURE,
                            answer = "No framework",
                        ),
                    ),
            )

        val conflicts = engine.detectConflicts("Build a web dashboard", config)

        assertTrue(conflicts.any { it.summary.contains("no framework", ignoreCase = true) })
    }

    @Test
    fun `detects auth request without threat model conflict`() {
        val config =
            PromptSessionConfig(
                clarificationQuestions =
                    listOf(
                        ClarificationQuestion(
                            id = "q_threat_model",
                            question = "What threat model should be considered?",
                            category = ClarificationCategory.SECURITY,
                            answer = "",
                        ),
                    ),
            )

        val conflicts = engine.detectConflicts("Implement OAuth login", config)

        assertTrue(conflicts.any { it.summary.contains("threat model", ignoreCase = true) })
    }

    @Test
    fun `detects enterprise depth versus rapid profile conflict`() {
        val config =
            PromptSessionConfig(
                behaviourProfile = BehaviourProfile.RAPID_PROTOTYPE,
                promptDepth = PromptDepth.ENTERPRISE,
            )

        val conflicts = engine.detectConflicts("Build MVP app", config)

        assertTrue(conflicts.any { it.summary.contains("rapid MVP delivery", ignoreCase = true) })
    }

    @Test
    fun `no-auth prompt does not create authentication threat model conflict`() {
        val config = PromptSessionConfig()

        val conflicts =
            engine.detectConflicts(
                "Build a security operations triage tool. Do not build authentication initially.",
                config,
            )

        assertFalse(conflicts.any { it.summary.contains("Authentication is requested", ignoreCase = true) })
    }

    @Test
    fun `explicit frontend stack overrides no preference`() {
        val config =
            PromptSessionConfig(
                clarificationQuestions =
                    listOf(
                        ClarificationQuestion(
                            id = "q_frontend_approach",
                            question = "Which frontend framework or approach should be used?",
                            category = ClarificationCategory.ARCHITECTURE,
                            answer = "No preference",
                        ),
                    ),
            )

        val architecture =
            engine.analyze(
                "Create a security admin UI using React + TypeScript + Tailwind + shadcn/ui.",
                config,
            ).recommendedArchitecture

        assertTrue(architecture.any { it == "Frontend: React + TypeScript + Tailwind CSS + shadcn/ui" })
    }

    @Test
    fun `explicit backend stack overrides no preference`() {
        val config =
            PromptSessionConfig(
                clarificationQuestions =
                    listOf(
                        ClarificationQuestion(
                            id = "q_backend",
                            question = "Which backend stack should be used?",
                            category = ClarificationCategory.ARCHITECTURE,
                            answer = "No preference",
                        ),
                    ),
            )

        val architecture = engine.analyze("Use Spring Boot for the API service.", config).recommendedArchitecture

        assertTrue(architecture.any { it == "Backend: Spring Boot" })
    }

    @Test
    fun `explicit storage choice overrides no preference`() {
        val config =
            PromptSessionConfig(
                clarificationQuestions =
                    listOf(
                        ClarificationQuestion(
                            id = "q_storage",
                            question = "Which storage should be used?",
                            category = ClarificationCategory.ARCHITECTURE,
                            answer = "No preference",
                        ),
                    ),
            )

        val architecture = engine.analyze("Persist findings in PostgreSQL.", config).recommendedArchitecture

        assertTrue(architecture.any { it == "Storage: PostgreSQL" })
    }
}
