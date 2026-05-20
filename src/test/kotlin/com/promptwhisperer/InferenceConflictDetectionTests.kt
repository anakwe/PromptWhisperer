package com.promptwhisperer

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.ClarificationCategory
import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.models.PromptDepth
import com.promptwhisperer.models.PromptSessionConfig
import com.promptwhisperer.models.RequestAnalysis
import com.promptwhisperer.services.InferenceEngine
import kotlin.test.Test
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
}

