package com.promptwhisperer.ui.components

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.Guardrail
import com.promptwhisperer.models.GuardrailCategory
import com.promptwhisperer.services.BehaviourProfileService
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Guardrail configuration panel with grouped categories and reset-to-defaults support.
 */
class GuardrailSelectionPanel(
    private val behaviourProfileService: BehaviourProfileService,
) {
    val component: JPanel = JPanel(BorderLayout(0, 6))

    private val enabledCountLabel = JLabel("Enabled guardrails: 0")
    private val resetButton = JButton("Reset to Recommended Defaults")

    private val checkboxById = mutableMapOf<String, JCheckBox>()

    init {
        setupLayout()
    }

    fun applyDefaults(profile: BehaviourProfile) {
        val defaults =
            behaviourProfileService.getDefaultGuardrailsForProfile(profile)
                .associateBy { it.id }

        checkboxById.forEach { (id, checkBox) ->
            checkBox.isSelected = defaults[id]?.enabled == true
        }
        refreshEnabledCount()
    }

    fun selectedGuardrails(): List<Guardrail> {
        val selectedIds =
            checkboxById
                .filterValues { it.isSelected }
                .keys
                .toSet()

        return Guardrail.all().map { guardrail ->
            guardrail.copy(enabled = selectedIds.contains(guardrail.id))
        }
    }

    fun setOnReset(action: () -> Unit) {
        resetButton.addActionListener { action() }
    }

    fun setOnSelectionChanged(action: (Int) -> Unit) {
        checkboxById.values.forEach { box ->
            box.addActionListener {
                refreshEnabledCount()
                action(enabledCount())
            }
        }
    }

    fun enabledCount(): Int = checkboxById.values.count { it.isSelected }

    private fun setupLayout() {
        val categoriesPanel = JPanel(GridLayout(2, 2, 8, 8))

        GuardrailCategory.values().forEach { category ->
            categoriesPanel.add(buildCategoryPanel(category))
        }

        val footer =
            JPanel(BorderLayout()).apply {
                add(enabledCountLabel, BorderLayout.WEST)
                add(resetButton, BorderLayout.EAST)
                border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
            }

        component.border = BorderFactory.createTitledBorder("Guardrails")
        component.add(categoriesPanel, BorderLayout.CENTER)
        component.add(footer, BorderLayout.SOUTH)
        refreshEnabledCount()
    }

    private fun buildCategoryPanel(category: GuardrailCategory): JPanel {
        val panel = JPanel(GridLayout(0, 1, 0, 2))
        panel.border = BorderFactory.createTitledBorder(categoryTitle(category))

        Guardrail.all()
            .filter { it.category == category }
            .forEach { guardrail ->
                val checkBox =
                    JCheckBox(guardrail.name).apply {
                        toolTipText = guardrail.description
                    }
                checkboxById[guardrail.id] = checkBox
                panel.add(checkBox)
            }

        return panel
    }

    private fun categoryTitle(category: GuardrailCategory): String =
        when (category) {
            GuardrailCategory.ARCHITECTURE -> "Architecture"
            GuardrailCategory.SECURITY -> "Security"
            GuardrailCategory.CODE_QUALITY -> "Code Quality"
            GuardrailCategory.OPERATIONAL -> "Operational Safety"
        }

    private fun refreshEnabledCount() {
        enabledCountLabel.text = "Enabled guardrails: ${enabledCount()}"
        enabledCountLabel.foreground = if (enabledCount() > 0) Color(45, 127, 84) else Color(153, 102, 0)
    }
}
