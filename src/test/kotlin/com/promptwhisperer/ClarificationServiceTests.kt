package com.promptwhisperer

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.PromptMode
import com.promptwhisperer.services.ClarificationServiceImpl
import kotlin.test.Test
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
    }
}
