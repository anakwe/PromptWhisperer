package com.promptwhisperer

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.PromptMode
import com.promptwhisperer.services.ClarificationServiceImpl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClarificationServiceTests {
    private val service = ClarificationServiceImpl()

    @Test
    fun `web game request generates practical ux and framework questions`() {
        val questions =
            service.generateClarifications(
                request = "Build a small Flappy Bird style web game",
                profile = BehaviourProfile.BALANCED_ENGINEER,
                mode = PromptMode.STANDARD,
            )

        val ids = questions.map { it.id }.toSet()
        assertTrue(ids.contains("q_frontend_approach"))
        assertTrue(ids.contains("q_controls"))
        assertTrue(ids.contains("q_responsive"))
        assertTrue(ids.contains("q_score_tracking"))
    }

    @Test
    fun `security mode adds threat and compliance clarifications`() {
        val questions =
            service.generateClarifications(
                request = "Review login flow",
                profile = BehaviourProfile.SECURITY_ENGINEER,
                mode = PromptMode.SECURITY_REVIEW,
            )

        val ids = questions.map { it.id }.toSet()
        assertTrue(ids.contains("q_security_asset"))
        assertTrue(ids.contains("q_threat_model"))
        assertTrue(ids.contains("q_compliance"))
        assertFalse(ids.contains("q_controls"))
        assertFalse(ids.contains("q_score_tracking"))
        assertFalse(ids.contains("q_sound"))
    }

    @Test
    fun `game prompts receive gameplay-specific clarification questions`() {
        val questions =
            service.generateClarifications(
                request = "Build a small Flappy Bird style web game with a leaderboard",
                profile = BehaviourProfile.BALANCED_ENGINEER,
                mode = PromptMode.STANDARD,
            )

        val ids = questions.map { it.id }.toSet()
        assertTrue(ids.contains("q_frontend_approach"))
        assertTrue(ids.contains("q_controls"))
        assertTrue(ids.contains("q_responsive"))
        assertTrue(ids.contains("q_score_tracking"))
        assertTrue(ids.contains("q_sound"))
    }

    @Test
    fun `security tool prompt never receives gameplay clarifications in stateless-like generation`() {
        val questions =
            service.generateClarifications(
                request = "Design a SIEM and WAF security engineering triage workflow with audit evidence",
                profile = BehaviourProfile.SECURITY_ENGINEER,
                mode = PromptMode.STANDARD,
            )

        val ids = questions.map { it.id }.toSet()
        assertTrue(ids.contains("q_security_asset"))
        assertTrue(ids.contains("q_threat_model"))
        assertTrue(ids.contains("q_compliance"))
        assertFalse(ids.contains("q_controls"))
        assertFalse(ids.contains("q_score_tracking"))
        assertFalse(ids.contains("q_sound"))
    }

    @Test
    fun `generic web app receives neutral ui clarifications but no game questions`() {
        val questions =
            service.generateClarifications(
                request = "Build a web app dashboard for support operations",
                profile = BehaviourProfile.BALANCED_ENGINEER,
                mode = PromptMode.STANDARD,
            )

        val ids = questions.map { it.id }.toSet()
        assertTrue(ids.contains("q_frontend_approach"))
        assertTrue(ids.contains("q_responsive"))
        assertTrue(ids.contains("q_accessibility"))
        assertFalse(ids.contains("q_controls"))
        assertFalse(ids.contains("q_score_tracking"))
        assertFalse(ids.contains("q_sound"))
    }

    @Test
    fun `diagnostics record why gameplay question was selected`() {
        service.generateClarifications(
            request = "Create a game with gameplay, score, pause, restart, and leaderboard",
            profile = BehaviourProfile.BALANCED_ENGINEER,
            mode = PromptMode.STANDARD,
        )

        val diagnostics = service.getLastSelectionDiagnostics()
        val controlsDiagnostic = diagnostics.firstOrNull { it.questionId == "q_controls" }

        assertTrue(controlsDiagnostic != null)
        assertTrue(controlsDiagnostic.reason.contains("domainProfile=GAME"))
        assertTrue(controlsDiagnostic.reason.contains("confidence="))
    }
}
