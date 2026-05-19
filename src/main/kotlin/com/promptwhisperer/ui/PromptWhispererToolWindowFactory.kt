package com.promptwhisperer.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * Factory that creates the Prompt Whisperer tool window.
 * Keeps UI code separate from business logic services.
 */
class PromptWhispererToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun shouldBeAvailable(project: Project): Boolean = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = PromptWhispererPanelV3(project)
        val content = toolWindow.contentManager.factory.createContent(panel.component, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
