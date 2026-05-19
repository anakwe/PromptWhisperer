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
import java.awt.BorderLayout
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
import javax.swing.border.TitledBorder

/**
 * Refactored Prompt Whisperer UI Panel.
 *
 * Major redesign:
 * - Header + collapsible help
 * - Configuration area (profile, depth, guardrails)
 * - Task input (multiline)
 * - Clarification questions (dynamic)
 * - Control buttons
 * - **Large central generated prompt area (primary focus)**
 * - Activity log (secondary, collapsible)
 *
 * This layout makes the generated prompt the centerpiece of the tool.
 */
class PromptWhispererPanelV2(private val project: Project) {
    val component: JPanel = JPanel(BorderLayout())

    // ─── Configuration ────────────────────────────────────────────────────────
    private val behaviourProfileSelector = JComboBox(BehaviourProfile.values())
    private val promptDepthSelector = JComboBox(PromptDepth.values())

    // ─── Inputs ───────────────────────────────────────────────────────────────
    private val taskInputArea = JBTextArea(6, 50).apply {
        lineWrap = true
        wrapStyleWord = true
        text = "Example: Build a simple demo website with an embedded video, responsive layout, and README instructions."
    }

    // ─── Outputs ──────────────────────────────────────────────────────────────
    private val promptPreview = JBTextArea().apply {
        isEditable = true
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        foreground = Color.GRAY
        text = "(No prompt generated yet — describe your task and click Analyse Request.)"
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

    // ─── Services ─────────────────────────────────────────────────────────────
    private val clarificationService = ClarificationServiceImpl()
    private val behaviourProfileService = BehaviourProfileServiceImpl()
    private val promptBuilder = PromptBuilderImpl()

    private var sessionConfig = PromptSessionConfig()
    private var currentClarifications: List<ClarificationQuestion> = emptyList()

    init {
        setupLayout()
        wireActions()
    }

    private fun setupLayout() {
        // ─── Top: Header + Help ───────────────────────────────────────────────
        val headerPanel = buildHeaderPanel()

        // ─── Configuration section ────────────────────────────────────────────
        val configPanel = buildConfigurationPanel()

        // ─── Task input ───────────────────────────────────────────────────────
        val taskPanel = buildTaskInputPanel()

        // ─── Clarification questions (dynamic) ────────────────────────────────
        val clarificationPanel = buildClarificationPanel()

        // ─── Combine top sections ─────────────────────────────────────────────
        val topPanel = JPanel()
        topPanel.layout = javax.swing.BoxLayout(topPanel, javax.swing.BoxLayout.Y_AXIS)
        topPanel.add(headerPanel)
        topPanel.add(JSeparator())
        topPanel.add(configPanel)
        topPanel.add(taskPanel)
        topPanel.add(clarificationPanel)

        // ─── Central: Large prompt preview ────────────────────────────────────
        val previewPanel = JPanel(BorderLayout(0, 4))
        previewPanel.border = BorderFactory.createEmptyBorder(8, 8, 4, 8)
        val previewTitle = JLabel("📝 Generated Prompt")
        previewTitle.font = previewTitle.font.deriveFont(Font.BOLD)
        previewPanel.add(previewTitle, BorderLayout.NORTH)
        previewPanel.add(JBScrollPane(promptPreview), BorderLayout.CENTER)

        // ─── Bottom: Control buttons ───────────────────────────────────────────
        val controlPanel = buildControlPanel()

        // ─── Log section (collapsible) ────────────────────────────────────────
        val logPanel = buildLogPanel()

        // ─── Main layout ──────────────────────────────────────────────────────
        component.add(JBScrollPane(topPanel), BorderLayout.NORTH)
        component.add(previewPanel, BorderLayout.CENTER)
        component.add(controlPanel, BorderLayout.SOUTH)

        // Add log as a resizable panel at the very bottom (optional future)
    }

    private fun buildHeaderPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(12, 8, 4, 8)

        val titleLabel = JLabel("Prompt Whisperer")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 18f)

        val subtitleLabel = JLabel("Design better implementation prompts for AI coding assistants.")
        subtitleLabel.font = subtitleLabel.font.deriveFont(Font.PLAIN, 12f)
        subtitleLabel.foreground = Color(120, 120, 120)

        val textPanel = JPanel()
        textPanel.layout = javax.swing.BoxLayout(textPanel, javax.swing.BoxLayout.Y_AXIS)
        textPanel.add(titleLabel)
        textPanel.add(subtitleLabel)

        val helpButton = JButton("❓ Help")
        helpButton.addActionListener {
            showHelpDialog()
        }

        panel.add(textPanel, BorderLayout.WEST)
        panel.add(helpButton, BorderLayout.EAST)
        return panel
    }

    private fun buildConfigurationPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

        val profileRow = JPanel()
        profileRow.add(JLabel("Profile:"))
        profileRow.add(behaviourProfileSelector)

        val depthRow = JPanel()
        depthRow.add(JLabel("Depth:"))
        depthRow.add(promptDepthSelector)

        val configInnerPanel = JPanel()
        configInnerPanel.layout = javax.swing.BoxLayout(configInnerPanel, javax.swing.BoxLayout.Y_AXIS)
        configInnerPanel.add(profileRow)
        configInnerPanel.add(depthRow)

        panel.add(configInnerPanel, BorderLayout.WEST)
        return panel
    }

    private fun buildTaskInputPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Step 1: Describe What You Want")
        panel.add(JBScrollPane(taskInputArea), BorderLayout.CENTER)
        val minHeight = 100
        panel.preferredSize = Dimension(panel.width, minHeight)
        return panel
    }

    private fun buildClarificationPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Step 2: Clarifications (Auto-Generated After Analysis)")
        panel.add(JLabel("(Awaiting analysis...)"), BorderLayout.CENTER)
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

    private fun buildLogPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(0, 8, 8, 8)
        val logTitle = JLabel("Activity Log (collapsible)")
        logTitle.font = logTitle.font.deriveFont(Font.PLAIN, 10f)
        panel.add(logTitle, BorderLayout.NORTH)
        val logScroll = JBScrollPane(activityLog)
        logScroll.preferredSize = Dimension(0, 80)
        panel.add(logScroll, BorderLayout.CENTER)
        return panel
    }

    private fun wireActions() {
        analyseButton.addActionListener {
            val task = taskInputArea.text.trim()
            if (task.isEmpty()) {
                log("⚠  Enter a task description before analysing.")
                return@addActionListener
            }

            log("🔍 Analysing request...")
            currentClarifications = clarificationService.generateClarifications(task)
            log("✅ Generated ${currentClarifications.size} clarification questions.")

            // TODO: Show clarifications in UI
        }

        generateButton.addActionListener {
            val task = taskInputArea.text.trim()
            if (task.isEmpty()) {
                log("⚠  Enter a task description before generating a prompt.")
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
            log("✅ Generated prompt with ${profile.displayName} profile at ${depth.displayName} depth.")
        }

        copyButton.addActionListener {
            val text = promptPreview.text.trim()
            if (text.isEmpty() || text.startsWith("(No prompt")) {
                log("⚠  Generate a prompt first.")
                return@addActionListener
            }
            copyToClipboard(text)
            log("✅ Prompt copied to clipboard.")
        }

        saveButton.addActionListener {
            log("💾 Save Artefact feature coming soon.")
        }

        resetButton.addActionListener {
            taskInputArea.text = ""
            clearPrompt()
            activityLog.text = ""
            currentClarifications = emptyList()
            log("Session reset.")
        }
    }

    private fun showHelpDialog() {
        log("❓ Help: Click 'Analyse Request' to generate clarification questions, then 'Generate Prompt' to create the final prompt.")
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

