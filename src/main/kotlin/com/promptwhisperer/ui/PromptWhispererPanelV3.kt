package com.promptwhisperer.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.PromptDepth
import com.promptwhisperer.models.PromptMode
import com.promptwhisperer.models.PromptSessionConfig
import com.promptwhisperer.services.ArtefactPersistenceServiceImpl
import com.promptwhisperer.services.BehaviourProfileServiceImpl
import com.promptwhisperer.services.ClarificationServiceImpl
import com.promptwhisperer.services.DiagnosticPhase
import com.promptwhisperer.services.FailedCommand
import com.promptwhisperer.services.PromptBuilderImpl
import com.promptwhisperer.services.TroubleshootingState
import com.promptwhisperer.services.TroubleshootingServiceImpl
import com.promptwhisperer.ui.components.ClarificationAnswersPanel
import com.promptwhisperer.ui.components.GuardrailSelectionPanel
import com.promptwhisperer.ui.components.PromptOutputPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingUtilities

/**
 * Prompt Whisperer V3 panel focused on implementation planning quality.
 */
class PromptWhispererPanelV3(private val project: Project) {
    val component: JPanel = JPanel(BorderLayout())

    private val modeSelector = JComboBox(PromptMode.values())
    private val behaviourProfileSelector = JComboBox(BehaviourProfile.values())
    private val promptDepthSelector = JComboBox(PromptDepth.values())

    private val taskInputArea = JBTextArea(6, 60).apply {
        lineWrap = true
        wrapStyleWord = true
        text = "Example: Build a Flappy Bird style web game with responsive controls and deployment notes."
    }

    private val tsFailedCommandField = JBTextField()
    private val tsTechnologyField = JBTextField()
    private val tsErrorOutputArea = JBTextArea(6, 60).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val tsMaterialChangeField = JBTextField()

    private val analyseButton = JButton("Analyse Request")
    private val clearQuestionsButton = JButton("Clear Questions")
    private val generateButton = JButton("Generate Prompt")
    private val saveButton = JButton("Save Artefact")
    private val resetButton = JButton("Reset")

    private val activityLog = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 11)
    }

    private val clarificationPanel = ClarificationAnswersPanel()
    private val guardrailPanel = GuardrailSelectionPanel(BehaviourProfileServiceImpl())
    private val promptOutputPanel = PromptOutputPanel()

    private val clarificationService = ClarificationServiceImpl()
    private val behaviourProfileService = BehaviourProfileServiceImpl()
    private val promptBuilder = PromptBuilderImpl()
    private val troubleshootingService = TroubleshootingServiceImpl()
    private val artefactPersistenceService = ArtefactPersistenceServiceImpl(project)

    private val cardLayout = CardLayout()
    private val inputCard = JPanel(cardLayout)

    private val profileInfoLabel = JLabel()
    private val depthInfoLabel = JLabel()
    private val workflowStateLabel = JLabel("Stage 1: Enter your request and click Analyse Request")

    private var sessionConfig = PromptSessionConfig()
    private var didAnalyseRequest = false

    init {
        setupLayout()
        wireActions()
        applyProfileDefaults()
        promptOutputPanel.clear("No prompt generated yet. Describe a task and click Analyse Request.")
    }

    private fun setupLayout() {
        modeSelector.renderer = modeSelector.renderer
        behaviourProfileSelector.toolTipText = "Behaviour profile defines implementation philosophy"
        promptDepthSelector.toolTipText = "Prompt depth defines detail level"

        val topPanel = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(buildHeaderPanel())
            add(buildControlsPanel())
            add(buildInputCards())
            add(clarificationPanel.component)
            add(guardrailPanel.component)
            add(buildActionPanel())
            add(buildLogPanel())
        }

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = JBScrollPane(topPanel)
            bottomComponent = promptOutputPanel.component
            resizeWeight = 0.42
            dividerSize = 8
            border = BorderFactory.createEmptyBorder()
        }

        component.add(splitPane, BorderLayout.CENTER)
    }

    private fun buildHeaderPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val title = JLabel("Prompt Whisperer").apply {
            font = font.deriveFont(Font.BOLD, 20f)
        }

        val subtitle = JLabel("Local-first engineering implementation planning for AI coding assistants").apply {
            font = font.deriveFont(Font.PLAIN, 12f)
            foreground = Color(110, 110, 110)
        }

        val textPanel = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            add(title)
            add(subtitle)
        }

        workflowStateLabel.foreground = Color(59, 89, 152)
        panel.add(textPanel, BorderLayout.WEST)
        panel.add(workflowStateLabel, BorderLayout.EAST)
        return panel
    }

    private fun buildControlsPanel(): JPanel {
        val panel = JPanel(BorderLayout(8, 6)).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
            )
        }

        val selectors = JPanel().apply {
            add(JLabel("Mode:"))
            add(modeSelector)
            add(JLabel("Profile:"))
            add(behaviourProfileSelector)
            add(JLabel("Depth:"))
            add(promptDepthSelector)
        }

        val infoPanel = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            profileInfoLabel.foreground = Color(45, 127, 84)
            depthInfoLabel.foreground = Color(53, 97, 180)
            add(profileInfoLabel)
            add(depthInfoLabel)
        }

        panel.add(selectors, BorderLayout.NORTH)
        panel.add(infoPanel, BorderLayout.CENTER)
        return panel
    }

    private fun buildInputCards(): JPanel {
        val generationCard = JPanel(BorderLayout(0, 4)).apply {
            border = BorderFactory.createTitledBorder("Stage 1: Describe the implementation goal")
            add(JBScrollPane(taskInputArea), BorderLayout.CENTER)
        }

        val troubleshootingCard = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Troubleshooting Input")

            val row1 = JPanel(BorderLayout(8, 0)).apply {
                add(JLabel("Failed command"), BorderLayout.WEST)
                add(tsFailedCommandField, BorderLayout.CENTER)
            }
            val row2 = JPanel(BorderLayout(8, 0)).apply {
                add(JLabel("Technology"), BorderLayout.WEST)
                add(tsTechnologyField, BorderLayout.CENTER)
            }
            val row3 = JPanel(BorderLayout(8, 0)).apply {
                add(JLabel("Error output"), BorderLayout.NORTH)
                add(JBScrollPane(tsErrorOutputArea), BorderLayout.CENTER)
                preferredSize = Dimension(0, 130)
            }
            val row4 = JPanel(BorderLayout(8, 0)).apply {
                add(JLabel("Material change since last run"), BorderLayout.WEST)
                add(tsMaterialChangeField, BorderLayout.CENTER)
            }

            add(row1)
            add(row2)
            add(row3)
            add(row4)
        }

        inputCard.add(generationCard, PromptMode.STANDARD.name)
        inputCard.add(generationCard, PromptMode.ARCHITECTURE.name)
        inputCard.add(generationCard, PromptMode.DEBUGGING.name)
        inputCard.add(generationCard, PromptMode.SECURITY_REVIEW.name)
        inputCard.add(generationCard, PromptMode.TEST_GENERATION.name)
        inputCard.add(troubleshootingCard, PromptMode.TROUBLESHOOTING.name)

        return inputCard
    }

    private fun buildActionPanel(): JPanel {
        return JPanel().apply {
            border = BorderFactory.createEmptyBorder(8, 0, 2, 0)
            add(analyseButton)
            add(clearQuestionsButton)
            add(generateButton)
            add(saveButton)
            add(resetButton)
        }
    }

    private fun buildLogPanel(): JPanel {
        return JPanel(BorderLayout(0, 4)).apply {
            border = BorderFactory.createTitledBorder("Activity")
            val scroll = JBScrollPane(activityLog).apply { preferredSize = Dimension(0, 90) }
            add(scroll, BorderLayout.CENTER)
        }
    }

    private fun wireActions() {
        modeSelector.addActionListener {
            val mode = selectedMode()
            cardLayout.show(inputCard, mode.name)
            val isTroubleshooting = mode == PromptMode.TROUBLESHOOTING
            generateButton.isVisible = !isTroubleshooting
            behaviourProfileSelector.isEnabled = !isTroubleshooting
            promptDepthSelector.isEnabled = !isTroubleshooting
            clearQuestions(keepLog = true)
            workflowStateLabel.text = if (isTroubleshooting) {
                "Troubleshooting mode: capture evidence, then analyse"
            } else {
                "Stage 1: Enter your request and click Analyse Request"
            }
            workflowStateLabel.foreground = Color(59, 89, 152)
        }

        behaviourProfileSelector.addActionListener {
            applyProfileDefaults()
        }

        promptDepthSelector.addActionListener {
            updateDepthInfo()
        }

        analyseButton.addActionListener {
            if (selectedMode() == PromptMode.TROUBLESHOOTING) {
                runTroubleshootingAnalysis()
            } else {
                runClarificationAnalysis()
            }
        }

        generateButton.addActionListener {
            generateFinalPrompt()
        }

        clearQuestionsButton.addActionListener {
            clearQuestions(keepLog = false)
            log("Clarification questions cleared.")
        }

        saveButton.addActionListener {
            savePromptArtefact()
        }

        resetButton.addActionListener {
            resetSession()
        }

        guardrailPanel.setOnReset {
            applyProfileDefaults()
            log("Guardrails reset to recommended defaults.")
        }

        guardrailPanel.setOnSelectionChanged { count ->
            promptOutputPanel.setState("Guardrails updated: $count enabled", Color(59, 89, 152))
        }

        promptOutputPanel.onCopy = { text ->
            if (text.isBlank() || text.startsWith("No prompt generated yet")) {
                log("Generate a prompt first before copying.")
            } else {
                copyToClipboard(text)
                log("Prompt copied to clipboard.")
                promptOutputPanel.setState("Prompt copied", Color(45, 127, 84))
            }
        }

        promptOutputPanel.onExport = { text ->
            exportPrompt(text)
        }

        applyProfileDefaults()
        updateDepthInfo()
        generateButton.isEnabled = true
    }

    private fun applyProfileDefaults() {
        val profile = selectedProfile()
        guardrailPanel.applyDefaults(profile)
        profileInfoLabel.text = "Profile: ${profile.displayName} — ${profile.description}"
        profileInfoLabel.toolTipText = behaviourProfileService.getProfileDescription(profile)
        promptOutputPanel.setState("Profile selected: ${profile.displayName}", Color(45, 127, 84))
    }

    private fun updateDepthInfo() {
        val depth = selectedDepth()
        depthInfoLabel.text = "Depth: ${depth.displayName} — ${depth.description}"
        promptOutputPanel.setState("Depth set: ${depth.displayName}", Color(53, 97, 180))
    }

    private fun runClarificationAnalysis() {
        val task = taskInputArea.text.trim()
        if (task.isBlank()) {
            workflowStateLabel.text = "Enter a task description before analysing."
            workflowStateLabel.foreground = Color(153, 61, 61)
            log("Enter a task description before analysing.")
            return
        }

        if (clarificationPanel.hasQuestions()) {
            log("Existing clarification questions replaced with a new analysis for the current task.")
        }

        val profile = selectedProfile()
        val mode = selectedMode()
        val questions = clarificationService.generateClarifications(task, profile, mode)
        clarificationPanel.setQuestions(questions)
        didAnalyseRequest = true

        workflowStateLabel.text = "Stage 2: Answer clarification questions, then click Generate Prompt"
        workflowStateLabel.foreground = Color(59, 89, 152)
        generateButton.isEnabled = true
        log("Analysis complete: ${questions.size} clarification questions generated.")
        if (questions.isNotEmpty()) {
            log("Step 2 ready: answer questions in 'Clarification Questions', then click Generate Prompt.")
        }
        promptOutputPanel.setState("Clarifications ready", Color(59, 89, 152))

        SwingUtilities.invokeLater {
            clarificationPanel.component.scrollRectToVisible(clarificationPanel.component.bounds)
            clarificationPanel.component.requestFocusInWindow()
        }
    }

    private fun generateFinalPrompt() {
        val task = taskInputArea.text.trim()
        if (task.isBlank()) {
            log("No request entered for generation.")
            return
        }

        if (!didAnalyseRequest) {
            log("No clarification answers provided. Generating from the initial request only.")
        }

        val profile = selectedProfile()
        val depth = selectedDepth()
        val mode = selectedMode()
        val guardrails = guardrailPanel.selectedGuardrails().filter { it.enabled }
        val answers = clarificationPanel.answersMap()
        val clarificationQuestions = clarificationPanel.questionsWithAnswers()
        val analysis = clarificationService.analyzeRequest(task, answers)

        sessionConfig = PromptSessionConfig(
            promptMode = mode,
            behaviourProfile = profile,
            promptDepth = depth,
            enabledGuardrails = guardrails,
            requestAnalysis = analysis,
            clarificationQuestions = clarificationQuestions
        )

        val prompt = promptBuilder.buildImplementationPrompt(task, sessionConfig)
        promptOutputPanel.setPrompt(
            promptText = prompt,
            profileName = profile.displayName,
            depthName = depth.displayName,
            enabledGuardrails = guardrails.size,
            generationState = "Generated successfully"
        )
        workflowStateLabel.text = "Prompt generated. Review/edit in raw markdown or preview mode."
        workflowStateLabel.foreground = Color(45, 127, 84)
        log("Prompt generated in ${mode.displayName} using ${profile.displayName} profile.")
    }

    private fun runTroubleshootingAnalysis() {
        val command = tsFailedCommandField.text.trim()
        val technology = tsTechnologyField.text.trim().ifBlank { "unknown technology" }
        val errorOutput = tsErrorOutputArea.text.trim()
        val materialChange = tsMaterialChangeField.text.trim().ifBlank { "Not specified" }

        if (command.isBlank() || errorOutput.isBlank()) {
            workflowStateLabel.text = "Troubleshooting needs command + error output"
            workflowStateLabel.foreground = Color(153, 61, 61)
            log("Troubleshooting analysis requires command and error output.")
            return
        }

        val failedCommand = FailedCommand(
            command = command,
            errorOutput = errorOutput,
            technology = technology,
            phase = DiagnosticPhase.UNKNOWN
        )
        val state = TroubleshootingState(
            currentBlocker = materialChange,
            failedCommands = listOf(failedCommand)
        )
        val prompt = troubleshootingService.generateTroubleshootingPrompt(state, failedCommand)

        promptOutputPanel.setPrompt(
            promptText = prompt,
            profileName = BehaviourProfile.TROUBLESHOOTER.displayName,
            depthName = PromptDepth.DETAILED.displayName,
            enabledGuardrails = guardrailPanel.enabledCount(),
            generationState = "Troubleshooting analysis generated"
        )
        workflowStateLabel.text = "Troubleshooting prompt generated from captured evidence."
        workflowStateLabel.foreground = Color(45, 127, 84)
        log("Troubleshooting analysis generated.")
    }

    private fun savePromptArtefact() {
        val text = promptOutputPanel.getPromptText().trim()
        if (text.isBlank() || text.startsWith("No prompt generated yet")) {
            log("Generate a prompt before saving artefacts.")
            return
        }

        try {
            val artefact = artefactPersistenceService.savePromptArtefact(text, project)
            log("Saved artefact: .prompt-whisperer/prompts/${artefact.filename}")
        } catch (ex: Exception) {
            log("Save failed: ${ex.message ?: "Unknown error"}")
        }
    }

    private fun exportPrompt(prompt: String) {
        if (prompt.isBlank() || prompt.startsWith("No prompt generated yet")) {
            log("Generate a prompt before exporting.")
            return
        }

        val chooser = JFileChooser().apply {
            selectedFile = File(project.basePath ?: ".", "prompt-whisperer-export.prompt.md")
        }

        val result = chooser.showSaveDialog(component)
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                chooser.selectedFile.writeText(prompt)
                log("Exported prompt: ${chooser.selectedFile.absolutePath}")
                promptOutputPanel.setState("Prompt exported", Color(45, 127, 84))
            } catch (ex: Exception) {
                log("Export failed: ${ex.message ?: "Unknown error"}")
            }
        }
    }

    private fun resetSession() {
        taskInputArea.text = ""
        tsFailedCommandField.text = ""
        tsTechnologyField.text = ""
        tsErrorOutputArea.text = ""
        tsMaterialChangeField.text = ""
        clearQuestions(keepLog = true)
        applyProfileDefaults()
        generateButton.isEnabled = true
        activityLog.text = ""
        workflowStateLabel.text = "Stage 1: Enter your request and click Analyse Request"
        workflowStateLabel.foreground = Color(59, 89, 152)
        promptOutputPanel.clear("No prompt generated yet. Describe a task and click Analyse Request.")
        log("Session reset.")
    }

    private fun clearQuestions(keepLog: Boolean) {
        clarificationPanel.clearQuestions()
        didAnalyseRequest = false
        if (!keepLog) {
            workflowStateLabel.text = "Clarification questions cleared. You can analyse again or generate directly."
            workflowStateLabel.foreground = Color(153, 102, 0)
        }
    }

    private fun selectedMode(): PromptMode = modeSelector.selectedItem as? PromptMode ?: PromptMode.STANDARD

    private fun selectedProfile(): BehaviourProfile =
        behaviourProfileSelector.selectedItem as? BehaviourProfile ?: BehaviourProfile.default()

    private fun selectedDepth(): PromptDepth =
        promptDepthSelector.selectedItem as? PromptDepth ?: PromptDepth.default()

    private fun log(message: String) {
        activityLog.append("$message\n")
        val doc = activityLog.document
        if (doc.length > 0) {
            activityLog.caretPosition = doc.length
        }
    }

    private fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}

