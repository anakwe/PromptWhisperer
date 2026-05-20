package com.promptwhisperer.ui.components

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.promptwhisperer.models.ClarificationQuestion
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Dynamic panel that renders clarification questions and captures inline answers.
 */
class ClarificationAnswersPanel {
    val component: JPanel = JPanel(BorderLayout(0, 8))

    private val helperLabel =
        JLabel(
            "Answer what you know. Leave blanks if you want safe assumptions or follow-up questions.",
            SwingConstants.LEFT,
        )
    private val statusLabel = JLabel("0 questions answered out of 0")
    private val questionsPanel = JPanel(GridLayout(0, 1, 0, 10))
    private val answerFields = mutableMapOf<String, JBTextArea>()
    private var currentQuestions: List<ClarificationQuestion> = emptyList()

    init {
        component.border = BorderFactory.createTitledBorder("Clarification Questions")

        val header =
            JPanel(BorderLayout(8, 0)).apply {
                helperLabel.foreground = Color(120, 120, 120)
                statusLabel.foreground = Color(53, 97, 180)
                add(helperLabel, BorderLayout.CENTER)
                add(statusLabel, BorderLayout.EAST)
            }

        val scroll =
            JBScrollPane(questionsPanel).apply {
                preferredSize = Dimension(0, 210)
                minimumSize = Dimension(0, 150)
                border = BorderFactory.createLineBorder(Color(215, 215, 215))
            }

        component.add(header, BorderLayout.NORTH)
        component.add(scroll, BorderLayout.CENTER)
        clearQuestions()
    }

    fun setQuestions(questions: List<ClarificationQuestion>) {
        currentQuestions = questions
        questionsPanel.removeAll()
        answerFields.clear()

        if (questions.isEmpty()) {
            helperLabel.text = "No clarification questions yet. Enter a task and click Analyse Request."
            questionsPanel.add(JLabel("No clarification questions yet. Enter a task and click Analyse Request."))
            updateStatus()
            refresh()
            return
        }

        helperLabel.text = "Answer the questions below, then click Generate Prompt."

        questions.forEachIndexed { index, question ->
            val card =
                JPanel(BorderLayout(0, 6)).apply {
                    border =
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color(205, 205, 205)),
                            BorderFactory.createEmptyBorder(8, 8, 8, 8),
                        )
                    background = Color(250, 250, 250)
                }

            val title =
                JLabel("Question ${index + 1}  [${question.category.displayName}]").apply {
                    font = font.deriveFont(Font.BOLD)
                    foreground = Color(59, 89, 152)
                }

            val prompt = JLabel(question.question)
            val answerInput =
                JBTextArea(3, 10).apply {
                    lineWrap = true
                    wrapStyleWord = true
                    text = question.answer.ifBlank { question.defaultAnswer.orEmpty() }
                    toolTipText = "Leave blank to use 'Not specified' in final prompt"
                }
            answerInput.document.addDocumentListener(
                object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent?) = updateStatus()

                    override fun removeUpdate(e: DocumentEvent?) = updateStatus()

                    override fun changedUpdate(e: DocumentEvent?) = updateStatus()
                },
            )

            answerFields[question.id] = answerInput

            card.add(title, BorderLayout.NORTH)
            card.add(prompt, BorderLayout.CENTER)
            card.add(JBScrollPane(answerInput), BorderLayout.SOUTH)
            questionsPanel.add(card)
        }

        updateStatus()
        refresh()
    }

    fun clearQuestions() {
        currentQuestions = emptyList()
        setQuestions(emptyList())
    }

    fun hasQuestions(): Boolean = currentQuestions.isNotEmpty()

    fun answersMap(): Map<String, String> =
        currentQuestions.associate { question ->
            question.id to (answerFields[question.id]?.text?.trim().orEmpty())
        }

    fun questionsWithAnswers(): List<ClarificationQuestion> {
        return currentQuestions.map { question ->
            question.copy(answer = answerFields[question.id]?.text?.trim().orEmpty())
        }
    }

    private fun updateStatus() {
        val total = currentQuestions.size
        val answered = answerFields.values.count { it.text.trim().isNotEmpty() }
        statusLabel.text = "$answered questions answered out of $total"
    }

    private fun refresh() {
        questionsPanel.revalidate()
        questionsPanel.repaint()
        component.revalidate()
        component.repaint()
    }
}
