package com.promptwhisperer

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.ClarificationCategory
import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.models.PromptDepth
import com.promptwhisperer.models.PromptMode
import com.promptwhisperer.models.PromptSessionConfig
import com.promptwhisperer.services.PromptBuilderImpl
import kotlin.test.Test
import kotlin.test.assertTrue

class PromptClarificationTests {
    private val builder = PromptBuilderImpl()

    @Test
    fun `prompt renders clarification answers section with category format`() {
        val config =
            PromptSessionConfig(
                promptMode = PromptMode.STANDARD,
                behaviourProfile = BehaviourProfile.BALANCED_ENGINEER,
                promptDepth = PromptDepth.STANDARD,
                clarificationQuestions =
                    listOf(
                        ClarificationQuestion(
                            id = "q_frontend_approach",
                            question = "Which frontend framework or approach should be used?",
                            category = ClarificationCategory.ARCHITECTURE,
                            answer = "Use plain HTML, CSS and JavaScript",
                        ),
                    ),
            )

        val prompt = builder.buildImplementationPrompt("Build a game", config)

        assertTrue(prompt.contains("## Clarification Answers"))
        assertTrue(prompt.contains("### Architecture Question"))
        assertTrue(prompt.contains("Answer: Use plain HTML, CSS and JavaScript"))
        assertTrue(prompt.contains("Respect the requested implementation approach: Use plain HTML, CSS and JavaScript"))
    }

    @Test
    fun `unanswered clarification is rendered as not specified`() {
        val config =
            PromptSessionConfig(
                clarificationQuestions =
                    listOf(
                        ClarificationQuestion(
                            id = "q_testing",
                            question = "Should tests be added?",
                            category = ClarificationCategory.TESTING,
                            answer = "",
                        ),
                    ),
            )

        val prompt = builder.buildImplementationPrompt("Build a game", config)
        assertTrue(prompt.contains("Answer: Not specified"))
    }

    @Test
    fun `prompt generation works without clarification questions`() {
        val prompt =
            builder.buildImplementationPrompt(
                task = "Build a web app",
                config = PromptSessionConfig(clarificationQuestions = emptyList()),
            )

        assertTrue(prompt.contains("## Requested Outcome"))
    }

    @Test
    fun `security profile uses threat model voice`() {
        val prompt =
            builder.buildImplementationPrompt(
                task = "Build authentication for a web app",
                config =
                    PromptSessionConfig(
                        behaviourProfile = BehaviourProfile.SECURITY_ENGINEER,
                        clarificationQuestions =
                            listOf(
                                ClarificationQuestion(
                                    id = "q_frontend_approach",
                                    question = "Which frontend framework or approach should be used?",
                                    category = ClarificationCategory.ARCHITECTURE,
                                    answer = "Plain HTML/CSS/JS",
                                ),
                            ),
                    ),
            )

        assertTrue(prompt.contains("Reasoning stance: Threat-model-first architect"))
        assertTrue(prompt.contains("Threat boundary review and least-privilege checks"))
    }

    @Test
    fun `rapid prototype profile uses mvp coach voice`() {
        val prompt =
            builder.buildImplementationPrompt(
                task = "Build a flappy bird web game",
                config =
                    PromptSessionConfig(
                        behaviourProfile = BehaviourProfile.RAPID_PROTOTYPE,
                        promptDepth = PromptDepth.ENTERPRISE,
                    ),
            )

        assertTrue(prompt.contains("Reasoning stance: MVP delivery coach"))
        assertTrue(prompt.contains("Prioritize a playable, testable MVP slice first."))
        assertTrue(prompt.contains("Deliver playable MVP slice before optional architecture hardening"))
    }
}
