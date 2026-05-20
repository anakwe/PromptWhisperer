package com.promptwhisperer.ui.components

import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Font
import java.util.regex.Pattern
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

/**
 * Prompt output surface with metadata, raw markdown editor, and markdown preview modes.
 */
class PromptOutputPanel {
    val component: JPanel = JPanel(BorderLayout(0, 6))

    private val metadataLabel = JLabel("No prompt generated yet.")
    private val stateLabel = JLabel("Idle")

    private val rawEditor =
        JTextPane().apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            isEditable = true
        }

    private val markdownPreview =
        JEditorPane("text/html", "").apply {
            isEditable = false
        }

    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)

    private val rawModeButton = JButton("Raw Markdown")
    private val previewModeButton = JButton("Markdown Preview")
    private val copyButton = JButton("Copy Prompt")
    private val exportButton = JButton("Export .md")

    private val defaultForeground = UIManager.getColor("Label.foreground") ?: Color(60, 63, 65)

    var onCopy: ((String) -> Unit)? = null
    var onExport: ((String) -> Unit)? = null

    // Swing document callbacks cannot mutate style attributes synchronously.
    private var isApplyingStyles = false
    private var styleRefreshQueued = false

    init {
        setupLayout()
        wireActions()
        clear("No prompt generated yet. Describe a task and click Analyse Request.")
    }

    fun setPrompt(
        promptText: String,
        profileName: String,
        depthName: String,
        enabledGuardrails: Int,
        generationState: String,
    ) {
        rawEditor.text = promptText
        highlightMarkdown()
        markdownPreview.text = renderMarkdown(promptText)
        rawEditor.caretPosition = 0
        markdownPreview.caretPosition = 0

        val wordCount = promptText.split(Regex("\\s+")).count { it.isNotBlank() }
        metadataLabel.text = "Behaviour Profile: $profileName   |   Prompt Depth: $depthName   |   Guardrails Enabled: $enabledGuardrails   |   Generated Prompt Length: ${"%,d".format(wordCount)} words"
        metadataLabel.foreground = defaultForeground
        setState(generationState, Color(45, 127, 84))
    }

    fun clear(message: String) {
        rawEditor.text = message
        markdownPreview.text = renderMarkdown(message)
        metadataLabel.text = message
        metadataLabel.foreground = Color(120, 120, 120)
        setState("Waiting for analysis", Color(120, 120, 120))
    }

    fun getPromptText(): String = rawEditor.text

    fun setState(
        text: String,
        color: Color,
    ) {
        stateLabel.text = text
        stateLabel.foreground = color
    }

    private fun setupLayout() {
        val metaPanel =
            JPanel(BorderLayout(6, 0)).apply {
                border =
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color(220, 220, 220)),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8),
                    )
                add(metadataLabel, BorderLayout.CENTER)
                add(stateLabel, BorderLayout.EAST)
            }

        val toolbar =
            JPanel().apply {
                border = BorderFactory.createEmptyBorder(0, 8, 0, 8)
                add(rawModeButton)
                add(previewModeButton)
                add(copyButton)
                add(exportButton)
            }

        cardPanel.add(JBScrollPane(rawEditor), "raw")
        cardPanel.add(JBScrollPane(markdownPreview), "preview")

        component.border =
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
            )
        component.add(metaPanel, BorderLayout.NORTH)
        component.add(cardPanel, BorderLayout.CENTER)
        component.add(toolbar, BorderLayout.SOUTH)
        cardLayout.show(cardPanel, "raw")
    }

    private fun wireActions() {
        rawModeButton.addActionListener {
            cardLayout.show(cardPanel, "raw")
            setState("Raw markdown mode", Color(59, 89, 152))
        }
        previewModeButton.addActionListener {
            markdownPreview.text = renderMarkdown(rawEditor.text)
            markdownPreview.caretPosition = 0
            cardLayout.show(cardPanel, "preview")
            setState("Markdown preview mode", Color(59, 89, 152))
        }

        copyButton.addActionListener {
            onCopy?.invoke(rawEditor.text)
        }

        exportButton.addActionListener {
            onExport?.invoke(rawEditor.text)
        }

        rawEditor.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = scheduleStyleRefresh()

                override fun removeUpdate(e: DocumentEvent?) = scheduleStyleRefresh()

                override fun changedUpdate(e: DocumentEvent?) = scheduleStyleRefresh()

                private fun scheduleStyleRefresh() {
                    if (isApplyingStyles || styleRefreshQueued) {
                        return
                    }
                    styleRefreshQueued = true
                    SwingUtilities.invokeLater {
                        styleRefreshQueued = false
                        if (isApplyingStyles) {
                            return@invokeLater
                        }
                        isApplyingStyles = true
                        try {
                            highlightMarkdown()
                            if (stateLabel.text != "Waiting for analysis") {
                                setState("Prompt updated", Color(153, 102, 0))
                            }
                        } finally {
                            isApplyingStyles = false
                        }
                    }
                }
            },
        )
    }

    private fun highlightMarkdown() {
        val text = rawEditor.text
        val document = rawEditor.styledDocument
        val normal = SimpleAttributeSet().apply { StyleConstants.setForeground(this, defaultForeground) }
        document.setCharacterAttributes(0, document.length, normal, true)

        applyStyle(document, text, "(?m)^#{1,6}\\s.*$", Color(53, 97, 180), true)
        applyStyle(document, text, "(?m)^```.*$", Color(130, 66, 180), true)
        applyStyle(document, text, "(?m)^-\\s.*$", Color(22, 125, 79), false)
        applyStyle(document, text, "`[^`]+`", Color(130, 66, 180), false)
        applyStyle(document, text, "\\*\\*[^*]+\\*\\*", Color(53, 97, 180), true)
    }

    private fun applyStyle(
        doc: StyledDocument,
        text: String,
        regex: String,
        color: Color,
        bold: Boolean,
    ) {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(text)
        val style =
            SimpleAttributeSet().apply {
                StyleConstants.setForeground(this, color)
                StyleConstants.setBold(this, bold)
            }
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false)
        }
    }

    private fun renderMarkdown(markdown: String): String {
        val escaped =
            markdown
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")

        val lines = escaped.lines()
        val html = StringBuilder("<html><body style='font-family: -apple-system, Segoe UI, sans-serif; font-size: 12px;'>")
        var inCode = false
        lines.forEach { line ->
            when {
                line.startsWith("```") -> {
                    inCode = !inCode
                    html.append(if (inCode) "<pre style='background:#f5f5f5;padding:8px;border:1px solid #ddd;'>" else "</pre>")
                }

                inCode -> html.append(line).append("\n")
                line.startsWith("### ") -> html.append("<h3>").append(line.removePrefix("### ")).append("</h3>")
                line.startsWith("## ") -> html.append("<h2>").append(line.removePrefix("## ")).append("</h2>")
                line.startsWith("# ") -> html.append("<h1>").append(line.removePrefix("# ")).append("</h1>")
                line.startsWith("- ") -> html.append("<div>&bull; ").append(line.removePrefix("- ")).append("</div>")
                line.isBlank() -> html.append("<br/>")
                else -> html.append("<div>").append(line).append("</div>")
            }
        }
        html.append("</body></html>")
        return html.toString()
    }
}
