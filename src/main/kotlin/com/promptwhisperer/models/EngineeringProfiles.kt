package com.promptwhisperer.models

/**
 * Engineering behaviour/personality profiles.
 *
 * These are NOT for roleplay personalities.
 * They represent distinct engineering cognition profiles that influence:
 * - clarification questions asked
 * - implementation philosophy
 * - prompt wording and constraints
 * - architectural concerns prioritized
 */
enum class BehaviourProfile(
    val displayName: String,
    val description: String,
    val philosophy: String,
    val priorityAreas: List<String>
) {
    BALANCED_ENGINEER(
        displayName = "Balanced Engineer",
        description = "Sensible defaults, moderate detail, maintainability focused.",
        philosophy = "Build good code today that you won't regret tomorrow.",
        priorityAreas = listOf("maintainability", "clarity", "simplicity", "testing")
    ),

    SENIOR_ARCHITECT(
        displayName = "Senior Architect",
        description = "Architecture-first, scalability aware, asks about modularity and future growth.",
        philosophy = "Design for clarity, extensibility, and future team members.",
        priorityAreas = listOf("architecture", "scalability", "modularity", "API design", "future growth")
    ),

    SECURITY_ENGINEER(
        displayName = "Security Engineer",
        description = "Defensive mindset, least privilege, secure defaults.",
        philosophy = "Fail safely; assume hostile actors.",
        priorityAreas = listOf("security", "input validation", "secrets", "permissions", "attack surface")
    ),

    DEVOPS_SRE(
        displayName = "DevOps / SRE",
        description = "Operational reliability, observability, deployment safety.",
        philosophy = "Build to be deployed, monitored, and fixed in production.",
        priorityAreas = listOf("observability", "reliability", "deployment", "rollback", "blast radius")
    ),

    RAPID_PROTOTYPE(
        displayName = "Rapid Prototype",
        description = "Prioritise speed, fewer constraints, minimal ceremony.",
        philosophy = "Get it working first; optimise later.",
        priorityAreas = listOf("speed", "iterative development", "MVP", "simplicity")
    ),

    ENTERPRISE_CONSULTANT(
        displayName = "Enterprise Consultant",
        description = "Extensive documentation, governance aware, process oriented.",
        philosophy = "Document everything and make compliance auditable.",
        priorityAreas = listOf("documentation", "governance", "compliance", "auditability", "processes")
    ),

    TROUBLESHOOTER(
        displayName = "Troubleshooter",
        description = "Root-cause-first debugging, evidence gathering, safe incremental fixes.",
        philosophy = "Diagnose before you operate.",
        priorityAreas = listOf("diagnostics", "evidence", "root cause", "safe fixes", "verification")
    ),

    TEACHING_MODE(
        displayName = "Teaching Mode",
        description = "Explain reasoning, educational explanations, clarify trade-offs.",
        philosophy = "Build understanding, not just code.",
        priorityAreas = listOf("education", "reasoning", "trade-offs", "explanation")
    ),

    MINIMALIST(
        displayName = "Minimalist",
        description = "Smallest safe implementation, avoid overengineering.",
        philosophy = "Do the simplest thing that could possibly work.",
        priorityAreas = listOf("simplicity", "minimal dependencies", "conciseness", "essentials only")
    );

    override fun toString(): String = displayName

    companion object {
        fun default(): BehaviourProfile = BALANCED_ENGINEER
    }
}

/**
 * Guardrail categories and toggles.
 *
 * These are configurable safeguards that influence prompt generation and constraints.
 */
enum class GuardrailCategory {
    ARCHITECTURE,
    SECURITY,
    CODE_QUALITY,
    OPERATIONAL
}

data class Guardrail(
    val id: String,
    val category: GuardrailCategory,
    val name: String,
    val description: String,
    val enabled: Boolean = true,
    val appliesTo: List<BehaviourProfile> = BehaviourProfile.values().toList()
) {
    companion object {
        fun architecture(): List<Guardrail> = listOf(
            Guardrail(
                id = "arch_think_before_coding",
                category = GuardrailCategory.ARCHITECTURE,
                name = "Think before coding",
                description = "Require design/analysis phase before implementation."
            ),
            Guardrail(
                id = "arch_require_plan",
                category = GuardrailCategory.ARCHITECTURE,
                name = "Require implementation plan",
                description = "Break down the change into clear steps."
            ),
            Guardrail(
                id = "arch_avoid_rewrites",
                category = GuardrailCategory.ARCHITECTURE,
                name = "Avoid broad rewrites",
                description = "Prefer incremental changes over architectural rewrites."
            ),
            Guardrail(
                id = "arch_minimize_deps",
                category = GuardrailCategory.ARCHITECTURE,
                name = "Minimise dependency additions",
                description = "Avoid adding new external dependencies unless absolutely necessary."
            )
        )

        fun security(): List<Guardrail> = listOf(
            Guardrail(
                id = "sec_no_network",
                category = GuardrailCategory.SECURITY,
                name = "Prevent hidden network calls",
                description = "No surprise external API calls or telemetry."
            ),
            Guardrail(
                id = "sec_no_telemetry",
                category = GuardrailCategory.SECURITY,
                name = "Prevent telemetry",
                description = "No analytics, tracking, or monitoring without explicit user action."
            ),
            Guardrail(
                id = "sec_no_secrets",
                category = GuardrailCategory.SECURITY,
                name = "Prevent secret exposure",
                description = "No hardcoded keys, tokens, or credentials."
            ),
            Guardrail(
                id = "sec_input_validation",
                category = GuardrailCategory.SECURITY,
                name = "Require input validation",
                description = "Sanitise and validate all user-supplied input."
            ),
            Guardrail(
                id = "sec_least_privilege",
                category = GuardrailCategory.SECURITY,
                name = "Principle of least privilege",
                description = "Request minimal permissions and access rights."
            )
        )

        fun codeQuality(): List<Guardrail> = listOf(
            Guardrail(
                id = "cq_require_tests",
                category = GuardrailCategory.CODE_QUALITY,
                name = "Require tests",
                description = "Unit, integration or E2E tests must accompany new code."
            ),
            Guardrail(
                id = "cq_require_docs",
                category = GuardrailCategory.CODE_QUALITY,
                name = "Require documentation",
                description = "Update README, API docs, or architectural documentation."
            ),
            Guardrail(
                id = "cq_require_comments",
                category = GuardrailCategory.CODE_QUALITY,
                name = "Require inline comments",
                description = "Non-obvious logic must have clear comments."
            ),
            Guardrail(
                id = "cq_follow_conventions",
                category = GuardrailCategory.CODE_QUALITY,
                name = "Follow existing conventions",
                description = "Match naming, structure, and patterns already in the codebase."
            ),
            Guardrail(
                id = "cq_focused_functions",
                category = GuardrailCategory.CODE_QUALITY,
                name = "Keep functions focused",
                description = "Single responsibility; avoid god functions."
            )
        )

        fun operational(): List<Guardrail> = listOf(
            Guardrail(
                id = "op_rollback_plan",
                category = GuardrailCategory.OPERATIONAL,
                name = "Require rollback considerations",
                description = "Plan for undoing or reverting the change safely."
            ),
            Guardrail(
                id = "op_backwards_compat",
                category = GuardrailCategory.OPERATIONAL,
                name = "Preserve backwards compatibility",
                description = "Avoid breaking changes to public APIs or data formats."
            ),
            Guardrail(
                id = "op_blast_radius",
                category = GuardrailCategory.OPERATIONAL,
                name = "Minimise blast radius",
                description = "Limit the scope of potential impact if something goes wrong."
            ),
            Guardrail(
                id = "op_no_cascading",
                category = GuardrailCategory.OPERATIONAL,
                name = "Avoid cascading changes",
                description = "Do not require simultaneous changes in multiple unrelated areas."
            )
        )

        fun all(): List<Guardrail> = architecture() + security() + codeQuality() + operational()
    }
}

/**
 * Prompt generation depth levels.
 */
enum class PromptDepth(val displayName: String, val description: String, val detailMultiplier: Float) {
    MINIMAL(
        displayName = "Minimal",
        description = "Concise implementation prompt, essentials only.",
        detailMultiplier = 0.5f
    ),
    STANDARD(
        displayName = "Standard",
        description = "Balanced engineering detail, professional quality.",
        detailMultiplier = 1.0f
    ),
    DETAILED(
        displayName = "Detailed",
        description = "Senior engineer level specification with architecture.",
        detailMultiplier = 1.5f
    ),
    ENTERPRISE(
        displayName = "Enterprise",
        description = "Architecture, operations, governance, and compliance.",
        detailMultiplier = 2.0f
    );

    override fun toString(): String = displayName

    companion object {
        fun default(): PromptDepth = STANDARD
    }
}

