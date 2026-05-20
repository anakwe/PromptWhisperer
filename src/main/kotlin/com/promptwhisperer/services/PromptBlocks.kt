package com.promptwhisperer.services

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.GuardrailCategory
import com.promptwhisperer.models.PromptDepth
import com.promptwhisperer.models.PromptMode
import com.promptwhisperer.models.PromptSessionConfig

/**
 * Immutable prompt build context shared by all composable prompt blocks.
 */
data class PromptBuildContext(
    val task: String,
    val config: PromptSessionConfig,
) {
    val inference: PromptInference by lazy { InferenceEngine().analyze(task, config) }
}

/**
 * Reusable block contract for prompt composition.
 */
interface PromptBlock {
    fun render(context: PromptBuildContext): String
}

class CorePromptBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String =
        """
        # AI Implementation Planning Prompt

        You are assisting with implementation planning and delivery, not just code generation.

        ## Requested Outcome

        ${context.task}

        ## Working Mode

        - Mode: ${context.config.promptMode.displayName}
        - Behaviour Profile: ${context.config.behaviourProfile.displayName}
        - Prompt Depth: ${context.config.promptDepth.displayName}
        """.trimIndent()
}

class BehaviourProfileBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val profile = context.config.behaviourProfile
        val profileInstructions =
            when (profile) {
                BehaviourProfile.BALANCED_ENGINEER ->
                    listOf(
                        "Use practical, maintainable defaults.",
                        "Balance speed with code quality.",
                        "Prefer incremental delivery.",
                    )
                BehaviourProfile.SENIOR_ARCHITECT ->
                    listOf(
                        "Think architecture-first and call out module boundaries.",
                        "Explain key trade-offs before implementation.",
                        "Guard long-term extensibility.",
                    )
                BehaviourProfile.SECURITY_ENGINEER ->
                    listOf(
                        "Prioritize secure defaults and least privilege.",
                        "Model trust boundaries and input validation paths.",
                        "Call out dependency and secret risks.",
                    )
                BehaviourProfile.DEVOPS_SRE ->
                    listOf(
                        "Optimize for operability, resilience, and observability.",
                        "Call out deployment and rollback safety.",
                        "Minimize blast radius for changes.",
                    )
                BehaviourProfile.RAPID_PROTOTYPE ->
                    listOf(
                        "Deliver a fast, testable vertical slice.",
                        "Keep design lightweight while avoiding dead ends.",
                        "List hardening items for later.",
                    )
                BehaviourProfile.ENTERPRISE_CONSULTANT ->
                    listOf(
                        "Include governance, auditability, and documentation paths.",
                        "Explicitly address standards and handover concerns.",
                        "Provide implementation and rollout checkpoints.",
                    )
                BehaviourProfile.TROUBLESHOOTER ->
                    listOf(
                        "Diagnose from evidence before proposing fixes.",
                        "Prefer smallest safe fix first.",
                        "Include verification and rollback guidance.",
                    )
                BehaviourProfile.TEACHING_MODE ->
                    listOf(
                        "Explain intent, trade-offs, and why each step exists.",
                        "Sequence tasks to maximize understanding.",
                        "Use clear beginner-to-intermediate language where possible.",
                    )
                BehaviourProfile.MINIMALIST ->
                    listOf(
                        "Do the smallest safe implementation that meets intent.",
                        "Avoid unnecessary abstractions and dependencies.",
                        "Keep output concise and implementation-focused.",
                    )
            }

        return """
            ## Behaviour Guidance

            - Profile philosophy: ${profile.philosophy}
            - Priority areas: ${profile.priorityAreas.joinToString(", ")}
            - Reasoning stance: ${context.inference.voiceFrame}
            ${profileInstructions.joinToString("\n") { "- $it" }}
            ${context.inference.voiceInstructions.joinToString("\n") { "- $it" }}
            """.trimIndent()
    }
}

class PromptDepthBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val depth = context.config.promptDepth
        val expectations =
            when (depth) {
                PromptDepth.MINIMAL ->
                    listOf(
                        "Keep response concise and implementation-focused.",
                        "Include only essential architecture and testing notes.",
                    )
                PromptDepth.STANDARD ->
                    listOf(
                        "Provide balanced implementation guidance.",
                        "Include key trade-offs and practical verification steps.",
                    )
                PromptDepth.DETAILED ->
                    listOf(
                        "Provide senior-level implementation planning detail.",
                        "Include architecture, risk, testing, and operational checks.",
                    )
                PromptDepth.ENTERPRISE ->
                    listOf(
                        "Include governance, rollout, documentation, and operational controls.",
                        "Surface compliance and organizational-readiness considerations.",
                    )
            }

        return """
            ## Prompt Depth Expectations

            - Selected depth: ${depth.displayName}
            - Description: ${depth.description}
            ${expectations.joinToString("\n") { "- $it" }}
            """.trimIndent()
    }
}

class ClarificationAnswersBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val clarifications = context.config.clarificationQuestions
        val analysis = context.config.requestAnalysis

        if (clarifications.isEmpty() && analysis == null) {
            return ""
        }

        val answerLines =
            if (clarifications.isEmpty()) {
                "No clarification answers were provided."
            } else {
                clarifications.joinToString("\n\n") { question ->
                    """### ${question.category.displayName} Question
Question: ${question.question}
Answer: ${question.resolvedAnswer()}"""
                }
            }

        val concernLines =
            analysis?.inferredConcerns?.takeIf { it.isNotEmpty() }
                ?.joinToString("\n") { "- inferred concern: $it" } ?: ""

        val suggestedLines =
            buildList {
                analysis?.suggestedFramework?.let { add("- suggested framework: $it") }
                analysis?.suggestedArchitecture?.let { add("- suggested architecture: $it") }
            }.joinToString("\n")

        return """
            ## Clarification Answers

            $answerLines
            ${if (concernLines.isNotBlank()) "\n$concernLines" else ""}
            ${if (suggestedLines.isNotBlank()) "\n$suggestedLines" else ""}
            """.trimIndent()
    }
}

class ImplementationConsiderationsBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val lines =
            (context.inference.architectureConcerns + context.inference.operationalConcerns + context.inference.securityConcerns)
                .distinct()

        if (lines.isEmpty()) {
            return ""
        }

        return """
            ## Implementation Considerations

            ${lines.joinToString("\n") { "- $it" }}
            """.trimIndent()
    }
}

class RecommendedArchitectureBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val recommendations = context.inference.recommendedArchitecture
        if (recommendations.isEmpty()) {
            return ""
        }

        return """
            ## Recommended Architecture

            ${recommendations.joinToString("\n") { "- $it" }}
            """.trimIndent()
    }
}

class PlanningBalanceBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val conflicts = context.inference.conflicts
        if (conflicts.isEmpty()) {
            return ""
        }

        val balance =
            when (context.config.behaviourProfile) {
                BehaviourProfile.RAPID_PROTOTYPE ->
                    listOf(
                        "Build a production-capable MVP first.",
                        "Keep architecture extensible with lightweight boundaries.",
                        "Track hardening opportunities in a follow-up list.",
                    )
                BehaviourProfile.SECURITY_ENGINEER ->
                    listOf(
                        "Keep scope focused, but do not defer critical validation and access controls.",
                        "Document minimum security controls required for first release.",
                    )
                else ->
                    listOf(
                        "Preserve delivery momentum while keeping design reversible.",
                        "Separate immediate scope from future governance hardening.",
                    )
            }

        return """
            ## Planning Balance

            ${conflicts.joinToString("\n") { "- $it" }}

            Recommended balance:
            ${balance.joinToString("\n") { "- $it" }}
            """.trimIndent()
    }
}

class EngineeringTradeOffsBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val tradeoffs = context.inference.tradeoffs
        if (tradeoffs.isEmpty()) {
            return ""
        }

        return """
            ## Engineering Trade-Offs

            ${tradeoffs.joinToString("\n") { "- $it" }}
            """.trimIndent()
    }
}

class SuggestedDeliveryPrioritiesBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val priorities = context.inference.deliveryPriorities
        if (priorities.isEmpty()) {
            return ""
        }

        return """
            ## Suggested Delivery Priorities

            ${priorities.mapIndexed { index, item -> "${index + 1}. $item" }.joinToString("\n")}
            """.trimIndent()
    }
}

class ArchitectureBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        if (context.config.promptDepth == PromptDepth.MINIMAL && context.config.promptMode != PromptMode.ARCHITECTURE) {
            return ""
        }

        val preferredApproach =
            context.config.clarificationQuestions
                .firstOrNull { it.id == "q_frontend_approach" }
                ?.resolvedAnswer()

        val preferenceLine =
            if (preferredApproach.isNullOrBlank() || preferredApproach == "Not specified") {
                "- If a framework choice is ambiguous, ask before introducing new frameworks."
            } else {
                "- Respect the requested implementation approach: $preferredApproach. Do not switch frameworks unless explicitly justified."
            }

        return """
            ## Architecture Planning

            - Preserve existing project conventions and modular boundaries.
            - Identify impacted components/files before changing code.
            - Propose an implementation sequence with low-risk checkpoints.
            - Explain whether any new abstractions are required and why.
            $preferenceLine
            ${if (context.config.promptDepth.detailMultiplier >= 1.5f) "- Document interfaces/contracts between affected layers." else ""}
            """.trimIndent()
    }
}

class SecurityBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val guardrails = context.config.enabledGuardrails.filter { it.category == GuardrailCategory.SECURITY }
        if (guardrails.isEmpty() && context.config.promptMode != PromptMode.SECURITY_REVIEW) {
            return ""
        }

        val lines =
            if (guardrails.isEmpty()) {
                listOf(
                    "- Apply secure defaults and input validation.",
                    "- Do not leak secrets or credentials.",
                    "- Avoid hidden network calls and telemetry.",
                )
            } else {
                guardrails.map { "- ${it.name}: ${it.description}" }
            }

        return """
            ## Security Requirements

            ${lines.joinToString("\n")}
            """.trimIndent()
    }
}

class TestingBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val requiresTests = context.config.enabledGuardrails.any { it.id == "cq_require_tests" }
        if (!requiresTests && context.config.promptMode != PromptMode.TEST_GENERATION && context.config.promptDepth == PromptDepth.MINIMAL) {
            return ""
        }

        val modeExtras =
            if (context.config.promptMode == PromptMode.TEST_GENERATION) {
                "- Include unit + integration test strategy and sample test cases."
            } else {
                ""
            }

        return """
            ## Testing Expectations

            - Define test scope for happy path, edge cases, and regressions.
            - Align with the project's existing test tooling and conventions.
            - Provide commands to verify targeted tests and full suite.
            ${if (context.config.promptDepth.detailMultiplier >= 1.5f) "- Explain any risk areas that need additional test coverage." else ""}
            $modeExtras
            """.trimIndent()
    }
}

class DocumentationBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val includeDocs =
            context.config.enabledGuardrails.any { it.id == "cq_require_docs" } ||
                context.config.promptDepth.detailMultiplier > 1.0f
        if (!includeDocs) {
            return ""
        }

        return """
            ## Documentation Expectations

            - Update user-facing docs when behavior changes.
            - Add concise comments for non-obvious implementation logic.
            ${if (context.config.promptDepth.detailMultiplier >= 1.5f) "- Add architectural notes describing trade-offs and rationale." else ""}
            """.trimIndent()
    }
}

class OperationalBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val operational = context.config.enabledGuardrails.filter { it.category == GuardrailCategory.OPERATIONAL }
        if (operational.isEmpty() && context.config.promptDepth == PromptDepth.MINIMAL) {
            return ""
        }

        val lines =
            if (operational.isEmpty()) {
                listOf(
                    "- Keep blast radius low.",
                    "- Avoid broad cross-module coupling.",
                )
            } else {
                operational.map { "- ${it.name}: ${it.description}" }
            }

        return """
            ## Operational Considerations

            ${lines.joinToString("\n")}
            ${if (context.config.promptDepth.detailMultiplier >= 1.5f) "- Include rollback/mitigation steps for risky changes." else ""}
            """.trimIndent()
    }
}

class ModeSpecificBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        return when (context.config.promptMode) {
            PromptMode.STANDARD -> ""
            PromptMode.ARCHITECTURE ->
                """
                ## Mode Focus: Architecture

                - Provide a high-level component view before coding steps.
                - Highlight boundaries, data flow, and dependency direction.
                - Call out evolution path for future requirements.
                """.trimIndent()

            PromptMode.DEBUGGING ->
                """
                ## Mode Focus: Debugging

                - Start with probable failure hypotheses.
                - Propose evidence collection steps before changing code.
                - Provide fix plan with verification and rollback checks.
                """.trimIndent()

            PromptMode.SECURITY_REVIEW ->
                """
                ## Mode Focus: Security Review

                - Include threat surfaces, trust boundaries, and abuse scenarios.
                - Prioritize remediation by risk and implementation effort.
                - Confirm least privilege and validation strategy.
                """.trimIndent()

            PromptMode.TEST_GENERATION ->
                """
                ## Mode Focus: Test Generation

                - Derive tests directly from acceptance criteria.
                - Include clear test naming and scenario grouping.
                - Call out mock/stub strategy and deterministic setup.
                """.trimIndent()

            PromptMode.TROUBLESHOOTING ->
                """
                ## Mode Focus: Troubleshooting

                - Diagnose from logs/errors first.
                - Avoid speculative rewrites.
                - Show safe, incremental remediation path.
                """.trimIndent()
        }
    }
}

class ConstraintsBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val constraints =
            mutableListOf(
                "Do not modify unrelated files.",
                "Do not add telemetry/tracking unless explicitly requested.",
                "Do not automatically submit prompts or code anywhere.",
            )

        if (context.config.enabledGuardrails.any { it.id == "sec_no_network" }) {
            constraints.add("No hidden external API calls or network activity.")
        }
        if (context.config.enabledGuardrails.any { it.id == "sec_no_secrets" }) {
            constraints.add("No hardcoded secrets, keys, tokens, or credentials.")
        }
        if (context.config.enabledGuardrails.any { it.id == "arch_avoid_rewrites" }) {
            constraints.add("Prefer incremental implementation over broad rewrites.")
        }

        return """
            ## Constraints

            ${constraints.joinToString("\n") { "- $it" }}
            """.trimIndent()
    }
}

class OutputExpectationsBlock : PromptBlock {
    override fun render(context: PromptBuildContext): String {
        val depthExpectations =
            when (context.config.promptDepth) {
                PromptDepth.MINIMAL ->
                    listOf(
                        "Code changes with short rationale.",
                        "Modified file list.",
                        "Quick verification commands.",
                    )
                PromptDepth.STANDARD ->
                    listOf(
                        "Implementation approach summary.",
                        "Changed files with intent.",
                        "Verification commands and assumptions.",
                    )
                PromptDepth.DETAILED ->
                    listOf(
                        "Design rationale and trade-offs.",
                        "Implementation plan + code changes.",
                        "Test strategy and validation commands.",
                        "Risk and mitigation notes.",
                    )
                PromptDepth.ENTERPRISE ->
                    listOf(
                        "Impact summary and architecture rationale.",
                        "Detailed execution plan and affected modules.",
                        "Comprehensive validation and rollback steps.",
                        "Operational, security, and documentation outcomes.",
                    )
            }

        return """
            ## Output Expected

            ${depthExpectations.joinToString("\n") { "- $it" }}
            """.trimIndent()
    }
}
