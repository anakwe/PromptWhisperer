package com.promptwhisperer.services

import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Service responsible for generating deterministic, testable AI assistant prompts.
 * Keeps prompt format stable and includes required sections.
 */
interface PromptGenerationService {
    fun generatePrompt(
        taskDescription: String,
        context: SafeProjectContext?,
    ): String
}

/**
 * Minimal implementation with deterministic ordering.
 */
class PromptGenerationServiceImpl : PromptGenerationService {
    override fun generatePrompt(
        taskDescription: String,
        context: SafeProjectContext?,
    ): String {
        val createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val projectName = context?.projectName ?: "unknown-project"
        val taskSlug = SlugUtil.slugify(taskDescription)
        val header = "# AI Coding Assistant Implementation Prompt\n\n"
        val sections =
            listOf(
                "## Task Summary\n$taskDescription\n",
                "## Existing Project Context\nProject: $projectName\nDetected files: ${context?.safeFiles?.joinToString(", ") ?: "(none)"}\n",
                "## Relevant Files and Modules\n${context?.safeFiles?.joinToString("\n") { "- $it" } ?: "- (none)"}\n",
                "## Requirements\n- Implementation-focused requirements (derived from task)\n",
                "## Architecture Constraints\n- Honor existing architecture; avoid breaking public APIs unless agreed\n",
                "## Security Requirements\n- Do not include secrets; follow security guidelines in SECURITY.md\n",
                "## Operational Considerations\n- Deployment impact, rollout strategy and rollback notes\n",
                "## Testing Requirements\n- Unit and integration tests; suggested test cases\n",
                "## Documentation Requirements\n- Update README and inline docs where applicable\n",
                "## Acceptance Criteria\n- Clear, testable success criteria\n",
                "## Out of Scope\n- Explicitly list what must NOT be modified\n",
                "## Instructions to Coding Assistant\n- Do not modify files outside the listed modules.\n- Provide code snippets, tests and a changelog entry.\n",
                "## Review Checklist\n- Security reviewed\n- Tests added\n- Documentation updated\n",
            )
        val deterministicPrompt = header + sections.joinToString("\n")
        return deterministicPrompt + "\n<!-- generated_at: $createdAt | project: $projectName | task_slug: $taskSlug -->"
    }
}
