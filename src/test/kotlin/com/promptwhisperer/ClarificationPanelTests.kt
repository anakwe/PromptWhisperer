package com.promptwhisperer

import com.promptwhisperer.models.ClarificationCategory
import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.ui.components.ClarificationAnswersPanel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClarificationPanelTests {
    @Test
    fun `questions are stored in panel state and can be cleared`() {
        val panel = ClarificationAnswersPanel()
        val questions = listOf(
            ClarificationQuestion(
                id = "q_controls",
                question = "Should keyboard controls, touch controls or both be supported?",
                category = ClarificationCategory.UX,
                answer = "Both"
            )
        )

        panel.setQuestions(questions)
        assertTrue(panel.hasQuestions())
        assertTrue(panel.questionsWithAnswers().size == 1)

        panel.clearQuestions()
        assertFalse(panel.hasQuestions())
    }
}

