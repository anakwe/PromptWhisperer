package com.promptwhisperer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.promptwhisperer.services.DiagnosticPhase
import com.promptwhisperer.services.FailedCommand
import com.promptwhisperer.services.MaterialChange
import com.promptwhisperer.services.MaterialChangeCategory
import com.promptwhisperer.services.TroubleshootingServiceImpl
import com.promptwhisperer.services.TroubleshootingState
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Primary UI panel for the Prompt Whisperer tool window.
 *
 * Modes:
 *  - STANDARD: task-based prompt generation (original behaviour).
 *  - TROUBLESHOOTING: evidence-based debugging with no-loop protection.
 *
 * Delegates to services for context scanning, prompt generation,
 * troubleshooting analysis and artefact persistence.
 */
class PromptWhispererPanel(project: Project) {
    val component: JPanel = JPanel(BorderLayout())

    // -------------------------------------------------------------------------
    // Mode selector
    // -------------------------------------------------------------------------
    private val modeSelector = JComboBox(arrayOf("Standard Mode", "Troubleshooting Mode"))

    // -------------------------------------------------------------------------
    // Standard mode controls
    // -------------------------------------------------------------------------
    private val inputField = JBTextField()
    private val generateButton = JButton("Generate Prompt")
    private val copyButton = JButton("Copy Prompt")
    private val saveButton = JButton("Save Prompt Artefact")
    private val resetButton = JButton("Reset Session")

    // -------------------------------------------------------------------------
    // Troubleshooting mode controls
    // -------------------------------------------------------------------------
    private val tsFailedCommandField = JBTextField().apply {
        toolTipText = "Exact command that failed (e.g. ./gradlew test, npm install, docker build .)"
    }
    private val tsTechnologyField = JBTextField().apply {
        toolTipText = "Technology / tool (e.g. gradle, npm, python, docker, terraform, kubectl)"
    }
    private val tsErrorOutputArea = JBTextArea().apply {
        rows = 6
        toolTipText = "Paste the exact error output or stack trace here"
        lineWrap = true
        wrapStyleWord = true
    }
    private val tsMaterialChangeField = JBTextField().apply {
        toolTipText = "(Optional) Describe what changed since the last failure, if anything"
    }
    private val tsAnalyseButton = JButton("Analyse Failure")
    private val tsCopyButton = JButton("Copy Prompt")
    private val tsResetButton = JButton("Reset Troubleshooting Session")

    // -------------------------------------------------------------------------
    // Shared output areas
    // -------------------------------------------------------------------------
    private val conversationArea = JBTextArea().apply { isEditable = false; rows = 8; lineWrap = true; wrapStyleWord = true }
    private val promptPreview = JBTextArea().apply { isEditable = false; rows = 14; lineWrap = true; wrapStyleWord = true }

    // -------------------------------------------------------------------------
    // Troubleshooting state
    // -------------------------------------------------------------------------
    private val troubleshootingService = TroubleshootingServiceImpl()
    private var tsState = TroubleshootingState()
    private var lastTsPhase: DiagnosticPhase = DiagnosticPhase.UNKNOWN

    // -------------------------------------------------------------------------
    // Card layout for mode switching
    // -------------------------------------------------------------------------
    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)

    init {
        buildStandardPanel()
        buildTroubleshootingPanel()

        // Mode selector at top
        val topBar = JPanel(BorderLayout())
        topBar.add(JLabel("Mode: "), BorderLayout.WEST)
        topBar.add(modeSelector, BorderLayout.CENTER)
        topBar.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)

        // Output row (conversation + prompt preview) — shared between modes
        val outputRow = JPanel(BorderLayout())
        val left = JPanel(BorderLayout())
        left.add(JLabel("Log"), BorderLayout.NORTH)
        left.add(JBScrollPane(conversationArea), BorderLayout.CENTER)
        left.preferredSize = Dimension(440, 400)

        val right = JPanel(BorderLayout())
        right.add(JLabel("Generated Prompt"), BorderLayout.NORTH)
        right.add(JBScrollPane(promptPreview), BorderLayout.CENTER)
        right.preferredSize = Dimension(440, 400)

        outputRow.add(left, BorderLayout.WEST)
        outputRow.add(right, BorderLayout.EAST)

        component.add(topBar, BorderLayout.NORTH)
        component.add(cardPanel, BorderLayout.CENTER)
        component.add(outputRow, BorderLayout.SOUTH)

        wireModeSelector()
        wireStandardActions()
        wireTroubleshootingActions()
    }

    // -------------------------------------------------------------------------
    // Panel builders
    // -------------------------------------------------------------------------

    private fun buildStandardPanel() {
        val panel = JPanel(VerticalFlowLayout())
        panel.border = BorderFactory.createTitledBorder("Standard Prompt Generation")
        panel.add(JLabel("Task description:"))
        panel.add(inputField)
        panel.add(generateButton)
        panel.add(copyButton)
        panel.add(saveButton)
        panel.add(resetButton)
        cardPanel.add(panel, "standard")
    }

    private fun buildTroubleshootingPanel() {
        val panel = JPanel(VerticalFlowLayout())
        panel.border = BorderFactory.createTitledBorder("Troubleshooting Mode — Evidence-Based Debugging")

        val noteLabel = JLabel(
            "<html><b>⚠ Troubleshooting Mode</b>: Run commands yourself. " +
                    "Paste output back. One step at a time. " +
                    "Commands are not repeated unless something material changed.</html>"
        )
        noteLabel.border = BorderFactory.createEmptyBorder(0, 0, 8, 0)
        panel.add(noteLabel)

        panel.add(JLabel("Failed command (exact):"))
        panel.add(tsFailedCommandField)

        panel.add(JLabel("Technology / tool (gradle, npm, docker, terraform, kubectl, python…):"))
        panel.add(tsTechnologyField)

        panel.add(JLabel("Error output / stack trace (paste exact output):"))
        panel.add(JBScrollPane(tsErrorOutputArea).apply { preferredSize = Dimension(860, 120) })

        panel.add(JLabel("Material change since last failure (leave blank if nothing changed):"))
        panel.add(tsMaterialChangeField)

        val buttonRow = JPanel()
        buttonRow.add(tsAnalyseButton)
        buttonRow.add(tsCopyButton)
        buttonRow.add(tsResetButton)
        panel.add(buttonRow)

        cardPanel.add(panel, "troubleshooting")
    }

    // -------------------------------------------------------------------------
    // Wiring
    // -------------------------------------------------------------------------

    private fun wireModeSelector() {
        modeSelector.addActionListener {
            when (modeSelector.selectedIndex) {
                0 -> cardLayout.show(cardPanel, "standard")
                1 -> cardLayout.show(cardPanel, "troubleshooting")
            }
        }
        // Start in standard mode
        cardLayout.show(cardPanel, "standard")
    }

    private fun wireStandardActions() {
        generateButton.addActionListener {
            val taskText = inputField.text.trim()
            if (taskText.isEmpty()) {
                conversationArea.append("Please enter a task description.\n")
                return@addActionListener
            }
            conversationArea.append("User: $taskText\n")
            val prompt = buildStandardPrompt(taskText)
            promptPreview.text = prompt
            conversationArea.append("Prompt Whisperer: Generated standard prompt.\n")
        }

        copyButton.addActionListener { copyPromptToClipboard() }

        saveButton.addActionListener {
            if (promptPreview.text.isEmpty()) {
                conversationArea.append("No prompt to save.\n")
                return@addActionListener
            }
            conversationArea.append("Saved artefact: (placeholder)\n")
        }

        resetButton.addActionListener {
            inputField.text = ""
            promptPreview.text = ""
            conversationArea.append("Session reset.\n")
        }
    }

    private fun wireTroubleshootingActions() {
        tsAnalyseButton.addActionListener {
            val cmd = tsFailedCommandField.text.trim()
            val errorOutput = tsErrorOutputArea.text.trim()
            val technology = tsTechnologyField.text.trim()
            val materialChangeText = tsMaterialChangeField.text.trim()

            if (cmd.isEmpty()) {
                conversationArea.append("[Troubleshooting] Please enter the failed command.\n")
                return@addActionListener
            }
            if (errorOutput.isEmpty()) {
                conversationArea.append("[Troubleshooting] Please paste the error output.\n")
                return@addActionListener
            }

            // Record any material change before checking retry eligibility
            if (materialChangeText.isNotEmpty()) {
                val change = MaterialChange(
                    category = MaterialChangeCategory.CONFIGURATION_CHANGED,
                    description = materialChangeText
                )
                tsState = troubleshootingService.recordMaterialChange(tsState, change)
                conversationArea.append("[Troubleshooting] Material change recorded: $materialChangeText\n")
            }

            // No-loop check
            if (!troubleshootingService.canRetryCommand(tsState, cmd)) {
                val noLoopMsg = "⛔ This command has already failed and nothing material has changed. " +
                        "We need a different diagnostic step."
                conversationArea.append("[Troubleshooting] $noLoopMsg\n")
                promptPreview.text = "## No-Loop Protection\n\n$noLoopMsg\n\n" +
                        "Record what changed before retrying:\n" +
                        "- Fill in the 'Material change' field above with what you changed.\n" +
                        "- Examples: updated JAVA_HOME, installed a dependency, fixed a config file.\n"
                return@addActionListener
            }

            // Record failure and generate prompt
            val failed = FailedCommand(command = cmd, errorOutput = errorOutput, technology = technology)
            val previousPhase = tsState.currentPhase
            tsState = troubleshootingService.recordFailure(tsState, failed)

            // Phase transition message
            val transition = troubleshootingService.detectPhaseTransition(previousPhase, tsState.currentPhase)
            if (transition != null) {
                conversationArea.append("[Troubleshooting] $transition\n")
            }

            // Generate and display the structured troubleshooting prompt
            val prompt = troubleshootingService.generateTroubleshootingPrompt(tsState, failed)
            promptPreview.text = prompt
            conversationArea.append("[Troubleshooting] Analysed: phase=${tsState.currentPhase}, blocker=${tsState.currentBlocker}\n")

            // Clear the material change field after processing
            tsMaterialChangeField.text = ""
            lastTsPhase = tsState.currentPhase
        }

        tsCopyButton.addActionListener { copyPromptToClipboard() }

        tsResetButton.addActionListener {
            tsState = TroubleshootingState()
            lastTsPhase = DiagnosticPhase.UNKNOWN
            tsFailedCommandField.text = ""
            tsTechnologyField.text = ""
            tsErrorOutputArea.text = ""
            tsMaterialChangeField.text = ""
            promptPreview.text = ""
            conversationArea.append("[Troubleshooting] Session reset.\n")
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun copyPromptToClipboard() {
        val text = promptPreview.text
        if (text.isNotEmpty()) {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            val selection = java.awt.datatransfer.StringSelection(text)
            clipboard.setContents(selection, selection)
        }
    }

    private fun buildStandardPrompt(taskText: String): String {
        return "# AI Coding Assistant Implementation Prompt\n\n" +
                "## Task Summary\n$taskText\n\n" +
                "## Existing Project Context\n(Use the context preview panel to select files to include)\n\n" +
                "## Relevant Files and Modules\n- TODO: select files\n\n" +
                "## Requirements\n- TODO\n\n" +
                "## Architecture Constraints\n- TODO\n\n" +
                "## Security Requirements\n- TODO\n\n" +
                "## Operational Considerations\n- TODO\n\n" +
                "## Testing Requirements\n- TODO\n\n" +
                "## Documentation Requirements\n- TODO\n\n" +
                "## Acceptance Criteria\n- TODO\n\n" +
                "## Out of Scope\n- TODO\n\n" +
                "## Instructions to Coding Assistant\n- TODO\n\n" +
                "## Review Checklist\n- TODO\n"
    }
}
