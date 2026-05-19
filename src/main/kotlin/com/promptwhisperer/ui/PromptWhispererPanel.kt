package com.promptwhisperer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.promptwhisperer.services.ArtefactPersistenceServiceImpl
import com.promptwhisperer.services.DiagnosticPhase
import com.promptwhisperer.services.FailedCommand
import com.promptwhisperer.services.MaterialChange
import com.promptwhisperer.services.MaterialChangeCategory
import com.promptwhisperer.services.TroubleshootingServiceImpl
import com.promptwhisperer.services.TroubleshootingState
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.border.TitledBorder

/**
 * Primary UI panel for the Prompt Whisperer tool window.
 *
 * Generation modes (Standard, Architecture, Debugging, Security Review, Test Generation):
 *   1. Describe your task in plain language.
 *   2. Choose a generation mode.
 *   3. Click Generate Prompt — a complete, structured Copilot prompt appears.
 *   4. Review, then copy it into Copilot Chat.
 *
 * Troubleshooting Mode: evidence-based debugging with no-loop protection.
 * Fill in the failure details and click Analyse Failure.
 *
 * Security principles:
 *  - No external API calls. No telemetry. No hidden network access.
 *  - No automatic code modification. No automatic Copilot submission.
 */
class PromptWhispererPanel(private val project: Project) {
    val component: JPanel = JPanel(BorderLayout())

    // ─── Mode selector (6 modes) ──────────────────────────────────────────────
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

    // ─── Generation inputs ────────────────────────────────────────────────────
    private val taskInputArea = JBTextArea(5, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val generateButton = JButton("▶  Generate Prompt")
    private val copyButton     = JButton("⎘  Copy Prompt")
    private val saveButton     = JButton("  Save Artefact")
    private val resetButton    = JButton("↺  Reset")

    // ─── Troubleshooting inputs ───────────────────────────────────────────────
    private val tsFailedCommandField = JBTextField().apply {
        toolTipText = "Exact command that failed, e.g. ./gradlew test, npm install, docker build ."
    }
    private val tsTechnologyField = JBTextField().apply {
        toolTipText = "Technology / tool: gradle, npm, docker, terraform, kubectl, python…"
    }
    private val tsErrorOutputArea = JBTextArea(6, 40).apply {
        lineWrap = true
        wrapStyleWord = true
        toolTipText = "Paste the exact error output or stack trace here"
    }
    private val tsMaterialChangeField = JBTextField().apply {
        toolTipText = "What changed since the last failure? (leave blank if nothing changed)"
    }
    private val tsAnalyseButton = JButton("  Analyse Failure")
    private val tsCopyButton    = JButton("⎘  Copy Prompt")
    private val tsResetButton   = JButton("↺  Reset")

    // ─── Shared output area ───────────────────────────────────────────────────
    private val promptPreview = JBTextArea().apply {
        isEditable = true
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
    }
    private val logArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = Font(Font.MONOSPACED, Font.PLAIN, 11)
    }

    // ─── Services ─────────────────────────────────────────────────────────────
    private val troubleshootingService = TroubleshootingServiceImpl()
    private var tsState     = TroubleshootingState()
    private var lastTsPhase = DiagnosticPhase.UNKNOWN

    // ─── Card layout ──────────────────────────────────────────────────────────
    private val cardLayout = CardLayout()
    private val cardPanel  = JPanel(cardLayout)

    init {
        cardPanel.add(buildGenerationCard(), "generation")
        cardPanel.add(buildTroubleshootingCard(), "troubleshooting")

        clearPrompt()

        // North: help banner + mode selector + input card
        val northPanel = JPanel(BorderLayout(0, 6))
        northPanel.border = BorderFactory.createEmptyBorder(8, 8, 4, 8)
        northPanel.add(buildHelpPanel(),        BorderLayout.NORTH)
        northPanel.add(buildModeSelectorRow(),  BorderLayout.CENTER)
        northPanel.add(cardPanel,               BorderLayout.SOUTH)

        // Centre: prompt preview — gets all available vertical space
        val previewPanel = JPanel(BorderLayout(0, 4))
        previewPanel.border = BorderFactory.createEmptyBorder(0, 8, 4, 8)
        val previewTitle = JLabel("Generated Prompt")
        previewTitle.font = previewTitle.font.deriveFont(Font.BOLD)
        previewPanel.add(previewTitle,               BorderLayout.NORTH)
        previewPanel.add(JBScrollPane(promptPreview), BorderLayout.CENTER)

        // South: activity log (compact)
        val logPanel = JPanel(BorderLayout(0, 2))
        logPanel.border = BorderFactory.createEmptyBorder(0, 8, 8, 8)
        val logTitle = JLabel("Activity Log")
        logTitle.font = logTitle.font.deriveFont(Font.PLAIN, 11f)
        logPanel.add(logTitle, BorderLayout.NORTH)
        val logScroll = JBScrollPane(logArea)
        logScroll.preferredSize = Dimension(0, 90)
        logPanel.add(logScroll, BorderLayout.CENTER)

        component.add(northPanel,    BorderLayout.NORTH)
        component.add(previewPanel,  BorderLayout.CENTER)
        component.add(logPanel,      BorderLayout.SOUTH)

        wireModeSelector()
        wireGenerationActions()
        wireTroubleshootingActions()
        cardLayout.show(cardPanel, "generation")
    }

    // ─── Help banner ──────────────────────────────────────────────────────────

    private fun buildHelpPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color(90, 130, 200), 1, true),
            "How to use Prompt Whisperer",
            TitledBorder.LEFT, TitledBorder.TOP
        )
        val label = JLabel(
            """<html><body style='font-size:11px'>
            Prompt Whisperer turns a rough coding idea into a structured prompt for GitHub Copilot.<br/>
            <b>Steps:</b>&nbsp;
            Describe your task &rarr; Choose a mode &rarr; Generate &rarr; Copy &rarr; Paste into Copilot Chat.<br/>
            <b>Modes:&nbsp;</b>
            <b>Standard</b> &mdash; implementation prompt with requirements and acceptance criteria.&nbsp;&nbsp;
            <b>Architecture</b> &mdash; plan structure before coding.&nbsp;&nbsp;
            <b>Debugging</b> &mdash; diagnose before changing code.&nbsp;&nbsp;
            <b>Security Review</b> &mdash; secrets, permissions, input validation.&nbsp;&nbsp;
            <b>Test Generation</b> &mdash; add tests matching the project style.&nbsp;&nbsp;
            <b>&#x26A0; Troubleshooting</b> &mdash; evidence-based debugging with no-loop protection.
            </body></html>""".trimIndent()
        )
        label.border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
        panel.add(label, BorderLayout.CENTER)
        return panel
    }

    private fun buildModeSelectorRow(): JPanel {
        val panel = JPanel(BorderLayout(8, 0))
        val label = JLabel("Mode:")
        label.font = label.font.deriveFont(Font.BOLD)
        panel.add(label,        BorderLayout.WEST)
        panel.add(modeSelector, BorderLayout.CENTER)
        return panel
    }

    // ─── Generation card ──────────────────────────────────────────────────────

    private fun buildGenerationCard(): JPanel {
        val panel = JPanel(BorderLayout(0, 6))
        panel.border = BorderFactory.createEmptyBorder(8, 0, 0, 0)

        val inputSection = JPanel(BorderLayout(0, 4))
        inputSection.border = BorderFactory.createTitledBorder("Step 1 — Describe your task")
        val example = JLabel(
            "<html><i style='color:gray'>Example: Build a simple demo website with an embedded " +
                "video, responsive layout and README documentation.</i></html>"
        )
        example.border = BorderFactory.createEmptyBorder(0, 0, 4, 0)
        inputSection.add(example,                     BorderLayout.NORTH)
        inputSection.add(JBScrollPane(taskInputArea), BorderLayout.CENTER)

        val buttonSection = JPanel(BorderLayout())
        buttonSection.border = BorderFactory.createTitledBorder("Step 2 — Generate and review")
        val buttonRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 2))
        buttonRow.add(generateButton)
        buttonRow.add(copyButton)
        buttonRow.add(saveButton)
        buttonRow.add(resetButton)
        buttonSection.add(buttonRow, BorderLayout.CENTER)

        panel.add(inputSection,  BorderLayout.CENTER)
        panel.add(buttonSection, BorderLayout.SOUTH)
        return panel
    }

    // ─── Troubleshooting card ─────────────────────────────────────────────────

    private fun buildTroubleshootingCard(): JPanel {
        val panel = JPanel(VerticalFlowLayout())
        panel.border = BorderFactory.createTitledBorder("Troubleshooting Mode — Evidence-Based Debugging")

        val note = JLabel(
            "<html><b>&#x26A0; Troubleshooting Mode</b>: Run commands yourself. " +
                "Paste output back. One step at a time. " +
                "Commands are not repeated unless something material has changed.</html>"
        )
        note.border = BorderFactory.createEmptyBorder(0, 0, 8, 0)
        panel.add(note)

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

        return panel
    }

    // ─── Wiring ───────────────────────────────────────────────────────────────

    private fun wireModeSelector() {
        modeSelector.addActionListener {
            if (modeSelector.selectedIndex == 5) {
                cardLayout.show(cardPanel, "troubleshooting")
            } else {
                cardLayout.show(cardPanel, "generation")
            }
        }
        cardLayout.show(cardPanel, "generation")
    }

    private fun wireGenerationActions() {
        generateButton.addActionListener {
            val task = taskInputArea.text.trim()
            if (task.isEmpty()) {
                log("⚠  Enter a task description before generating a prompt.")
                return@addActionListener
            }
            val mode   = currentGenerationMode()
            val prompt = generatePromptForMode(task, mode)
            setPrompt(prompt)
            log("✅  Generated ${mode.displayName} prompt.")
        }

        copyButton.addActionListener {
            val text = promptPreview.text.trim()
            if (text.isEffectivelyEmpty()) {
                log("⚠  Generate a prompt first.")
                return@addActionListener
            }
            copyToClipboard(text)
            log("✅  Prompt copied to clipboard.")
        }

        saveButton.addActionListener {
            val text = promptPreview.text.trim()
            if (text.isEffectivelyEmpty()) {
                log("⚠  Generate a prompt before saving.")
                return@addActionListener
            }
            try {
                val artefact = ArtefactPersistenceServiceImpl(project).savePromptArtefact(text, project)
                log("✅  Saved: .prompt-whisperer/prompts/${artefact.filename}")
            } catch (e: Exception) {
                log("❌  Save failed: ${e.message}")
            }
        }

        resetButton.addActionListener {
            taskInputArea.text = ""
            clearPrompt()
            logArea.text = ""
            log("Session reset.")
        }
    }

    private fun wireTroubleshootingActions() {
        tsAnalyseButton.addActionListener {
            val cmd              = tsFailedCommandField.text.trim()
            val errorOutput      = tsErrorOutputArea.text.trim()
            val technology       = tsTechnologyField.text.trim()
            val materialChangeText = tsMaterialChangeField.text.trim()

            if (cmd.isEmpty())         { log("[TS] ⚠  Enter the failed command.");   return@addActionListener }
            if (errorOutput.isEmpty()) { log("[TS] ⚠  Paste the error output.");     return@addActionListener }

            if (materialChangeText.isNotEmpty()) {
                val change = MaterialChange(
                    category    = MaterialChangeCategory.CONFIGURATION_CHANGED,
                    description = materialChangeText
                )
                tsState = troubleshootingService.recordMaterialChange(tsState, change)
                log("[TS] Material change recorded: $materialChangeText")
            }

            if (!troubleshootingService.canRetryCommand(tsState, cmd)) {
                val msg = "⛔  This command has already failed and nothing material has changed. " +
                    "We need a different diagnostic step."
                log("[TS] $msg")
                setPrompt("## No-Loop Protection\n\n$msg\n\n" +
                    "Fill in the 'Material change' field above with what changed before retrying.")
                return@addActionListener
            }

            val failed        = FailedCommand(command = cmd, errorOutput = errorOutput, technology = technology)
            val previousPhase = tsState.currentPhase
            tsState           = troubleshootingService.recordFailure(tsState, failed)

            troubleshootingService.detectPhaseTransition(previousPhase, tsState.currentPhase)
                ?.let { log("[TS] $it") }

            val prompt = troubleshootingService.generateTroubleshootingPrompt(tsState, failed)
            setPrompt(prompt)
            log("[TS] Analysed — phase=${tsState.currentPhase}, blocker=${tsState.currentBlocker}")

            tsMaterialChangeField.text = ""
            lastTsPhase = tsState.currentPhase
        }

        tsCopyButton.addActionListener {
            val text = promptPreview.text.trim()
            if (text.isEffectivelyEmpty()) {
                log("[TS] ⚠  Analyse a failure first.")
                return@addActionListener
            }
            copyToClipboard(text)
            log("[TS] ✅  Prompt copied to clipboard.")
        }

        tsResetButton.addActionListener {
            tsState     = TroubleshootingState()
            lastTsPhase = DiagnosticPhase.UNKNOWN
            tsFailedCommandField.text  = ""
            tsTechnologyField.text     = ""
            tsErrorOutputArea.text     = ""
            tsMaterialChangeField.text = ""
            clearPrompt()
            logArea.text = ""
            log("[TS] Session reset.")
        }
    }

    // ─── Prompt generation ────────────────────────────────────────────────────

    private enum class GenerationMode(val displayName: String) {
        STANDARD("Standard"),
        ARCHITECTURE("Architecture"),
        DEBUGGING("Debugging"),
        SECURITY_REVIEW("Security Review"),
        TEST_GENERATION("Test Generation")
    }

    private fun currentGenerationMode(): GenerationMode = when (modeSelector.selectedIndex) {
        0 -> GenerationMode.STANDARD
        1 -> GenerationMode.ARCHITECTURE
        2 -> GenerationMode.DEBUGGING
        3 -> GenerationMode.SECURITY_REVIEW
        4 -> GenerationMode.TEST_GENERATION
        else -> GenerationMode.STANDARD
    }

    private fun generatePromptForMode(task: String, mode: GenerationMode): String = when (mode) {
        GenerationMode.STANDARD        -> buildStandardPrompt(task)
        GenerationMode.ARCHITECTURE    -> buildArchitecturePrompt(task)
        GenerationMode.DEBUGGING       -> buildDebuggingPrompt(task)
        GenerationMode.SECURITY_REVIEW -> buildSecurityReviewPrompt(task)
        GenerationMode.TEST_GENERATION -> buildTestGenerationPrompt(task)
    }

    private fun buildStandardPrompt(task: String): String = """
# Copilot Implementation Prompt

## Goal
$task

## Requirements
- Build only what is requested.
- Ask before making assumptions.
- Keep the implementation simple and maintainable.
- Follow existing project conventions and code style.
- Do not introduce unnecessary dependencies.

## Suggested Implementation Plan
1. Inspect the existing project structure and identify relevant files.
2. Implement the smallest working version of the feature.
3. Add or update tests if the project already has a test framework.
4. Add or update documentation where appropriate.
5. Confirm the implementation compiles and existing behaviour is not broken.

## Acceptance Criteria
- The requested feature works as described.
- The code compiles without errors.
- Existing tests still pass.
- Any new files are named clearly and placed in the correct location.
- The implementation is easy to review.

## Constraints
- Do not modify files unrelated to this feature.
- Do not add telemetry, tracking or analytics.
- Do not make hidden network calls.
- Do not hardcode secrets, tokens or credentials.
- Do not make broad architectural changes unless directly required.

## Output Expected From Copilot
- Brief explanation of the intended changes.
- List of files to create or modify.
- Code changes with inline comments where the logic is non-obvious.
- Test or verification steps so I can confirm it works.
    """.trimIndent()

    private fun buildArchitecturePrompt(task: String): String = """
# Copilot Architecture Review Prompt

## Goal
$task

## Instructions — Plan Before You Code
Before writing any code:
1. Analyse the existing project structure and identify all relevant modules, packages and files.
2. Propose a clear design — name the components and describe their responsibilities.
3. Explain trade-offs between at least two alternative approaches.
4. Identify risks, breaking changes and affected APIs.
5. Estimate scope: small / medium / large.
6. **Wait for my confirmation before implementing anything.**

## Design Output Expected
- Structured description of the proposed architecture (component names, responsibilities, data flow).
- List of files to create or modify, with the reason for each change.
- Trade-off analysis: what you chose and why you did not choose the alternatives.
- Identified risks and proposed mitigation.
- Clear flag if any existing public interfaces or APIs would break.
- Estimated scope of change.

## Constraints
- Do not start writing implementation code until the design is confirmed.
- Do not propose a full rewrite unless absolutely necessary — prefer incremental changes.
- Prefer patterns and conventions already established in this project.
- Flag every breaking change explicitly before the implementation begins.
    """.trimIndent()

    private fun buildDebuggingPrompt(task: String): String = """
# Copilot Debugging Prompt

## Problem Description
$task

## Instructions — Evidence-Based Debugging
1. **Do not change any code yet.**
2. Analyse the error message, stack trace or described behaviour carefully.
3. Identify the **deepest meaningful root cause** — distinguish it from cascading errors.
4. Cite specific evidence from files, logs or error messages.
5. Propose the **smallest safe, targeted fix** — not a refactor.
6. Provide a verification command so I can confirm the fix worked.
7. Do not make broad dependency changes or version upgrades unless directly proven necessary.
8. Do not repeat a failed command unless I confirm something material has changed.

## Root Cause Analysis Required
Provide your analysis in exactly this format:

- **Observed symptom:** [what actually happened]
- **Likely root cause:** [deepest cause, not the top-level wrapper]
- **Evidence cited:** [specific file, line, class or error message]
- **Proposed fix (smallest possible):** [one targeted change]
- **Verification command:** [exact command to run to confirm the fix]
- **What to watch for after the fix:** [what success or a new failure looks like]

## Constraints
- One fix at a time — do not propose multiple simultaneous changes.
- Do not touch unrelated code.
- Do not change toolchain, environment or dependency versions unless directly proven as the cause.
- If the fix does not work, report back with new evidence before trying another approach.
    """.trimIndent()

    private fun buildSecurityReviewPrompt(task: String): String = """
# Copilot Security Review Prompt

## Scope
$task

## Security Review Checklist
Review the specified code, feature or configuration for each of the following:

- [ ] Hardcoded secrets, tokens, API keys or passwords
- [ ] Unsafe file reads, writes or path traversal risks
- [ ] Unvalidated or unsanitised user input
- [ ] Unnecessary external network calls or third-party API dependencies
- [ ] Overly broad permissions or privilege escalation
- [ ] Sensitive data written to logs or error messages
- [ ] Unsafe prompt construction or prompt injection risks
- [ ] Outdated, vulnerable or unverified dependencies
- [ ] Missing or insufficient authentication and authorisation checks
- [ ] Insecure default settings or unsafe configuration values

## Output Expected
For each finding, provide:
- **Risk:** short, clear description of the issue
- **Severity:** Critical / High / Medium / Low
- **Location:** file name and line reference where possible
- **Recommended fix:** specific, targeted change

Also include:
- A summary of items checked and confirmed safe.
- Any items that could not be assessed and the reason why.

## Constraints
- Report all findings before proposing any fixes.
- Do not automatically apply fixes — present them for review first.
- Do not add telemetry, tracking or external monitoring as part of any fix.
- Do not introduce new network dependencies to resolve a security finding.
    """.trimIndent()

    private fun buildTestGenerationPrompt(task: String): String = """
# Copilot Test Generation Prompt

## Scope
$task

## Instructions
1. Inspect the existing test framework, test file structure and naming conventions in this project.
2. Match existing patterns — assertion style, setup/teardown, mocking approach, file organisation.
3. Add tests covering all three categories:
   - **Happy path** — expected normal behaviour with valid inputs
   - **Edge cases** — empty input, boundary values, null or missing data, unexpected formats
   - **Regression cases** — scenarios that have failed before or are likely to regress
4. Do not add brittle tests that depend on timing, test execution order or external services.
5. Do not introduce a new test framework — use what is already present in this project.
6. Provide the exact command to run only the new tests after adding them.
7. Provide the exact command to run the full test suite.

## Test Output Expected
- New test file(s) or targeted additions to existing test files.
- Clear test names that describe the **behaviour** being tested, not the implementation detail.
- A focused run command for only the new tests.
- A full suite run command.

## Constraints
- Tests must be deterministic and produce the same result on every run.
- Test observable behaviour — not internal implementation details.
- Each test should have a single, clear responsibility and assertion.
- If a test requires external dependencies (database, network, file system), mock or stub them.
- Do not leave TODO placeholders in test files.
    """.trimIndent()

    // ─── Helpers ──────────────────────────────────────────────────────────────

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
        logArea.append("$message\n")
        val doc = logArea.document
        if (doc.length > 0) logArea.caretPosition = doc.length
    }

    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    /** True if promptPreview contains only the placeholder text or is blank. */
    private fun String.isEffectivelyEmpty(): Boolean =
        isEmpty() || startsWith("(No prompt generated")
}
