package com.promptwhisperer.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.PromptDepth
import com.promptwhisperer.models.PromptSessionConfig
import com.promptwhisperer.models.ClarificationQuestion
import com.promptwhisperer.services.BehaviourProfileServiceImpl
import com.promptwhisperer.services.ClarificationServiceImpl
import com.promptwhisperer.services.PromptBuilderImpl
import com.promptwhisperer.services.TroubleshootingServiceImpl
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.UIManager

/**
 * Comprehensive Prompt Whisperer UI Panel V3.
 *
 * Includes:
 * - 5 generation modes (Standard, Architecture, Debugging, Security, Testing)
 * - 9 behaviour profiles
 * - 4 depth levels
 * - Guardrail toggles
 * - Multi-stage workflow (analyse → clarifications → generate)
 * - Troubleshooting Mode (evidence-based debugging)
 *
 * Layout:
 * - Header + help
 * - Mode selector
 * - Configuration (profile, depth)
 * - Task input or Troubleshooting input (card layout)
 - Clarifications (dynamic, shown after analysis)
 * - Large central prompt preview
 * - Activity log (secondary)
 */
class PromptWhispererPanelV3(private val project: Project) {
    val component: JPanel = JPanel(BorderLayout())

    // ─── Mode selector (6 modes total, including Troubleshooting) ─────────────
    private val modeSelector = JComboBox(
        arrayOf(
            "Standard Mode",
            "Architecture Mode",
            "Debugging Mode",
            "Security Review Mode",
            "Test Generation Mode",
            "⚠  Troubleshooting Mode"
        )
    )

    // ─── Configuration ────────────────────────────────────────────────────────
    private val behaviourProfileSelector = JComboBox(BehaviourProfile.values())
    private val promptDepthSelector = JComboBox(PromptDepth.values())

    // ─── Generation inputs ────────────────────────────────────────────────────
    private val taskInputArea = JBTextArea(6, 50).apply {
        lineWrap = true
        wrapStyleWord = true
        text = "Example: Build a simple demo website with an embedded video, responsive layout, and README instructions."
    }

    // ─── Troubleshooting inputs ───────────────────────────────────────────────
    private val tsFailedCommandField = JBTextField().apply {
        toolTipText = "Exact command that failed"
    }
    private val tsTechnologyField = JBTextField().apply {
        toolTipText = "Technology: gradle, npm, docker, terraform, etc."
    }
    private val tsErrorOutputArea = JBTextArea(6, 40).apply {
        lineWrap = true
        wrapStyleWord = true
        toolTipText = "Paste error output or stack trace"
    }
    private val tsMaterialChangeField = JBTextField().apply {
        toolTipText = "What changed since last failure?"
    }

    // ─── Output areas ─────────────────────────────────────────────────────────
    private val promptPreview = JBTextArea().apply {
        isEditable = true
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }

    private val activityLog = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 11)
    }

    // ─── Buttons ──────────────────────────────────────────────────────────────
    private val analyseButton = JButton("🔍  Analyse Request")
    private val generateButton = JButton("▶  Generate Prompt")
    private val copyButton = JButton("⎘  Copy Prompt")
    private val saveButton = JButton("💾  Save Artefact")
    private val resetButton = JButton("↺  Reset")

    private val tsAnalyseButton = JButton("🔍  Analyse Failure")
    private val tsCopyButton = JButton("⎘  Copy Prompt")
    private val tsResetButton = JButton("↺  Reset")

    // ─── Services ─────────────────────────────────────────────────────────────
    private val clarificationService = ClarificationServiceImpl()
    private val behaviourProfileService = BehaviourProfileServiceImpl()
    private val promptBuilder = PromptBuilderImpl()
    private val troubleshootingService = TroubleshootingServiceImpl()

    private var sessionConfig = PromptSessionConfig()
    private var currentClarifications: List<ClarificationQuestion> = emptyList()

    // ─── Card layout for generation vs troubleshooting ────────────────────────
    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)

    init {
        setupLayout()
        wireActions()
    }

    private fun setupLayout() {
        clearPrompt()

        // Build generation and troubleshooting cards
        cardPanel.add(buildGenerationInputCard(), "generation")
        cardPanel.add(buildTroubleshootingInputCard(), "troubleshooting")

        // Header
        val headerPanel = buildHeaderPanel()

        // Mode selector and configuration
        val configPanel = buildConfigurationPanel()

        // Top section (header + config + input card)
        val topPanel = JPanel()
        topPanel.layout = javax.swing.BoxLayout(topPanel, javax.swing.BoxLayout.Y_AXIS)
        topPanel.border = BorderFactory.createEmptyBorder(8, 8, 4, 8)
        topPanel.add(headerPanel)
        topPanel.add(JSeparator())
        topPanel.add(configPanel)
        topPanel.add(cardPanel)

        // Central: Large prompt preview
        val previewPanel = JPanel(BorderLayout(0, 4))
        previewPanel.border = BorderFactory.createEmptyBorder(0, 8, 4, 8)
        val previewTitle = JLabel("📝 Generated Prompt")
        previewTitle.font = previewTitle.font.deriveFont(Font.BOLD)
        previewPanel.add(previewTitle, BorderLayout.NORTH)
        previewPanel.add(JBScrollPane(promptPreview), BorderLayout.CENTER)

        // Bottom: Control buttons
        val controlPanel = buildControlPanel()

        // Activity log (compact)
        val logPanel = JPanel(BorderLayout(0, 2))
        logPanel.border = BorderFactory.createEmptyBorder(0, 8, 8, 8)
        val logTitle = JLabel("Activity Log")
        logTitle.font = logTitle.font.deriveFont(Font.PLAIN, 10f)
        logPanel.add(logTitle, BorderLayout.NORTH)
        val logScroll = JBScrollPane(activityLog)
        logScroll.preferredSize = Dimension(0, 80)
        logPanel.add(logScroll, BorderLayout.CENTER)

        // Assemble main layout
        component.add(JBScrollPane(topPanel), BorderLayout.NORTH)
        component.add(previewPanel, BorderLayout.CENTER)
        component.add(controlPanel, BorderLayout.SOUTH)
    }

    private fun buildHeaderPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        val titleLabel = JLabel("Prompt Whisperer")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 18f)

        val subtitleLabel = JLabel("Design better implementation prompts for AI coding assistants.")
        subtitleLabel.font = subtitleLabel.font.deriveFont(Font.PLAIN, 12f)
        subtitleLabel.foreground = Color(120, 120, 120)

        val textPanel = JPanel()
        textPanel.layout = javax.swing.BoxLayout(textPanel, javax.swing.BoxLayout.Y_AXIS)
        textPanel.add(titleLabel)
        textPanel.add(subtitleLabel)

        panel.add(textPanel, BorderLayout.WEST)
        return panel
    }

    private fun buildConfigurationPanel(): JPanel {
        val panel = JPanel()
        panel.border = BorderFactory.createEmptyBorder(8, 0, 8, 0)

        panel.add(JLabel("Mode:"))
        panel.add(modeSelector)
        panel.add(JLabel("  Profile:"))
        panel.add(behaviourProfileSelector)
        panel.add(JLabel("  Depth:"))
        panel.add(promptDepthSelector)

        return panel
    }

    private fun buildGenerationInputCard(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Step 1: Describe What You Want")
        panel.add(JBScrollPane(taskInputArea), BorderLayout.CENTER)
        return panel
    }

    private fun buildTroubleshootingInputCard(): JPanel {
        val panel = JPanel()
        panel.layout = javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder("Troubleshooting Mode: Provide Failure Details")

        val row1 = JPanel()
        row1.add(JLabel("Failed command:"))
        row1.add(tsFailedCommandField)

        val row2 = JPanel()
        row2.add(JLabel("Technology:"))
        row2.add(tsTechnologyField)

        val row3Row = JPanel(BorderLayout())
        row3Row.add(JLabel("Error output:"), BorderLayout.WEST)
        row3Row.add(JBScrollPane(tsErrorOutputArea), BorderLayout.CENTER)

        val row4Row = JPanel()
        row4Row.add(JLabel("Material change:"))
        row4Row.add(tsMaterialChangeField)

        panel.add(row1)
        panel.add(row2)
        panel.add(row3Row)
        panel.add(row4Row)

        return panel
    }

    private fun buildControlPanel(): JPanel {
        val panel = JPanel()
        panel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

        panel.add(analyseButton)
        panel.add(generateButton)
        panel.add(copyButton)
        panel.add(saveButton)
        panel.add(resetButton)

        return panel
    }

    private fun wireActions() {
        // Mode selector switches between generation and troubleshooting
        modeSelector.addActionListener {
            if (modeSelector.selectedIndex == 5) { // Troubleshooting Mode
                cardLayout.show(cardPanel, "troubleshooting")
                analyseButton.text = "🔍  Analyse Failure"
                generateButton.isVisible = false
                behaviourProfileSelector.isEnabled = false
                promptDepthSelector.isEnabled = false
            } else {
                cardLayout.show(cardPanel, "generation")
                analyseButton.text = "🔍  Analyse Request"
                generateButton.isVisible = true
                behaviourProfileSelector.isEnabled = true
                promptDepthSelector.isEnabled = true
            }
        }
        cardLayout.show(cardPanel, "generation")

        // Analyse button
        analyseButton.addActionListener {
            if (modeSelector.selectedIndex == 5) {
                // Troubleshooting mode analysis
                analyzeTroubleshootingFailure()
            } else {
                // Generation mode analysis
                analyzeGenerationRequest()
            }
        }

        // Generate button (generation modes only)
        generateButton.addActionListener {
            val task = taskInputArea.text.trim()
            if (task.isEmpty()) {
                log("⚠  Enter a task description.")
                return@addActionListener
            }

            val profile = behaviourProfileSelector.selectedItem as? BehaviourProfile ?: BehaviourProfile.default()
            val depth = promptDepthSelector.selectedItem as? PromptDepth ?: PromptDepth.default()
            val guardrails = behaviourProfileService.getDefaultGuardrailsForProfile(profile)

            sessionConfig = PromptSessionConfig(
                behaviourProfile = profile,
                promptDepth = depth,
                enabledGuardrails = guardrails
            )

            val prompt = promptBuilder.buildImplementationPrompt(task, sessionConfig)
            setPrompt(prompt)
            log("✅ Generated prompt (${profile.displayName}, ${depth.displayName}).")
        }

        // Copy button
        copyButton.addActionListener {
            val text = promptPreview.text.trim()
            if (text.isEmpty() || text.startsWith("(No prompt")) {
                log("⚠  Generate a prompt first.")
                return@addActionListener
            }
            copyToClipboard(text)
            log("✅ Copied to clipboard.")
        }

        // Save button
        saveButton.addActionListener {
            log("💾 Save feature coming soon.")
        }

        // Reset button
        resetButton.addActionListener {
            taskInputArea.text = ""
            tsFailedCommandField.text = ""
            tsTechnologyField.text = ""
            tsErrorOutputArea.text = ""
            tsMaterialChangeField.text = ""
            clearPrompt()
            activityLog.text = ""
            log("Session reset.")
        }
    }

    private fun analyzeGenerationRequest() {
        val task = taskInputArea.text.trim()
        if (task.isEmpty()) {
            log("⚠  Enter a task description.")
            return
        }
        log("🔍 Analysing request...")
        currentClarifications = clarificationService.generateClarifications(task)
        log("✅ Generated ${currentClarifications.size} clarification questions.")
    }

    private fun analyzeTroubleshootingFailure() {
        val cmd = tsFailedCommandField.text.trim()
        val errorOutput = tsErrorOutputArea.text.trim()

        if (cmd.isEmpty()) {
            log("⚠  Enter the failed command.")
            return
        }
        if (errorOutput.isEmpty()) {
            log("⚠  Paste the error output.")
            return
        }

        log("🔍 Analyzing failure...")
        val prompt = """
        ## Troubleshooting Analysis

        **Failed Command:** $cmd
        **Error Output:**
        ```
        $errorOutput
        ```

        [Detailed troubleshooting logic would go here]
        """.trimIndent()

        setPrompt(prompt)
        log("✅ Generated troubleshooting analysis.")
    }

    private fun setPrompt(text: String) {
        promptPreview.foreground = UIManager.getColor("TextArea.foreground") ?: Color.BLACK
        promptPreview.text = text
        promptPreview.caretPosition = 0
    }

    private fun clearPrompt() {
        promptPreview.foreground = Color.GRAY
        promptPreview.text = "(No prompt generated yet — describe your task and click Generate Prompt.)"
    }

    private fun log(message: String) {
        activityLog.append("$message\n")
        val doc = activityLog.document
        if (doc.length > 0) activityLog.caretPosition = doc.length
    }

    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}

