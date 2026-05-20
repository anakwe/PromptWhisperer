package com.promptwhisperer.services

import com.promptwhisperer.models.PromptSessionConfig

/**
 * Modular prompt builder.
 *
 * Composes a final prompt from independent, reusable blocks:
 * - Core Implementation Block
 * - Behaviour Profile Block
 * - Security Block
 * - Architecture Block
 * - Operational Block
 * - Documentation Block
 * - Testing Block
 * - Guardrail Block
 * - Depth-specific details
 *
 * This avoids giant template strings and allows dynamic composition.
 */
interface PromptBuilder {
    fun buildImplementationPrompt(task: String, config: PromptSessionConfig): String
}

class PromptBuilderImpl : PromptBuilder {
    private val blocks: List<PromptBlock> = listOf(
        CorePromptBlock(),
        BehaviourProfileBlock(),
        PromptDepthBlock(),
        ClarificationAnswersBlock(),
        PlanningBalanceBlock(),
        ImplementationConsiderationsBlock(),
        RecommendedArchitectureBlock(),
        EngineeringTradeOffsBlock(),
        SuggestedDeliveryPrioritiesBlock(),
        ArchitectureBlock(),
        SecurityBlock(),
        TestingBlock(),
        DocumentationBlock(),
        OperationalBlock(),
        ModeSpecificBlock(),
        ConstraintsBlock(),
        OutputExpectationsBlock()
    )

    override fun buildImplementationPrompt(task: String, config: PromptSessionConfig): String {
        val context = PromptBuildContext(task = task, config = config)
        return blocks
            .mapNotNull { it.render(context).takeIf { output -> output.isNotBlank() } }
            .joinToString("\n\n")
    }
}

