package com.promptwhisperer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Primary UI panel for the Prompt Whisperer tool window.
 * Delegates to services for context scanning, prompt generation and artefact persistence.
 */
class PromptWhispererPanel(project: Project) {
    val component: JPanel = JPanel(BorderLayout())

    private val inputField = JBTextField()
    private val generateButton = JButton("Generate Prompt")
    private val copyButton = JButton("Copy Prompt")
    private val saveButton = JButton("Save Prompt Artefact")
    private val resetButton = JButton("Reset Session")

    private val conversationArea = JBTextArea().apply { isEditable = false; rows = 8 }
    private val contextArea = JBTextArea().apply { isEditable = false; rows = 12 }
    private val promptPreview = JBTextArea().apply { isEditable = false; rows = 14 }

    init {
        val controls = JPanel(VerticalFlowLayout())
        controls.add(inputField)
        controls.add(generateButton)
        controls.add(copyButton)
        controls.add(saveButton)
        controls.add(resetButton)

        val left = JPanel(BorderLayout())
        left.add(JBScrollPane(conversationArea), BorderLayout.CENTER)
        left.preferredSize = Dimension(480, 600)

        val right = JPanel(BorderLayout())
        right.add(JBScrollPane(contextArea), BorderLayout.NORTH)
        right.add(JBScrollPane(promptPreview), BorderLayout.CENTER)
        right.preferredSize = Dimension(480, 600)

        val center = JPanel(BorderLayout())
        center.add(left, BorderLayout.WEST)
        center.add(right, BorderLayout.EAST)

        component.add(controls, BorderLayout.NORTH)
        component.add(center, BorderLayout.CENTER)

        wireActions()
    }

    private fun wireActions() {
        generateButton.addActionListener {
            val taskText = inputField.text.trim()
            if (taskText.isEmpty()) {
                conversationArea.append("Please enter a task description.\n")
                return@addActionListener
            }
            conversationArea.append("User: $taskText\n")
            // Placeholder behaviour: generate a deterministic prompt preview based on input
            val prompt = "# AI Coding Assistant Implementation Prompt\n\n## Task Summary\n$taskText\n\n## Existing Project Context\n(Use the context preview panel to select files to include)\n\n## Relevant Files and Modules\n- TODO: select files\n\n## Requirements\n- TODO\n\n## Architecture Constraints\n- TODO\n\n## Security Requirements\n- TODO\n\n## Operational Considerations\n- TODO\n\n## Testing Requirements\n- TODO\n\n## Documentation Requirements\n- TODO\n\n## Acceptance Criteria\n- TODO\n\n## Out of Scope\n- TODO\n\n## Instructions to Coding Assistant\n- TODO\n\n## Review Checklist\n- TODO\n"
            promptPreview.text = prompt
            conversationArea.append("Prompt Whisperer: Generated prompt preview.\n")
        }

        copyButton.addActionListener {
            val text = promptPreview.text
            if (text.isNotEmpty()) {
                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                val selection = java.awt.datatransfer.StringSelection(text)
                clipboard.setContents(selection, selection)
            }
        }

        saveButton.addActionListener {
            val promptText = promptPreview.text
            if (promptText.isEmpty()) {
                conversationArea.append("No prompt to save.\n")
                return@addActionListener
            }
            // Saving is not yet implemented in UI placeholder
            conversationArea.append("Saved artefact: (placeholder)\n")
        }

        resetButton.addActionListener {
            inputField.text = ""
            promptPreview.text = ""
            conversationArea.append("Session reset.\n")
        }
    }
}
