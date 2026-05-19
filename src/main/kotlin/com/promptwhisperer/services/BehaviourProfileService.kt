package com.promptwhisperer.services

import com.promptwhisperer.models.BehaviourProfile
import com.promptwhisperer.models.Guardrail
import com.promptwhisperer.models.GuardrailCategory

/**
 * Service for managing behaviour profile effects on prompt generation.
 *
 * Each profile:
 * - Suggests relevant clarification questions
 * - Pre-enables or disables certain guardrails
 * - Influences wording and tone
 * - Prioritises certain engineering concerns
 */
interface BehaviourProfileService {
    fun getDefaultGuardrailsForProfile(profile: BehaviourProfile): List<Guardrail>
    fun getProfileDescription(profile: BehaviourProfile): String
}

class BehaviourProfileServiceImpl : BehaviourProfileService {
    override fun getDefaultGuardrailsForProfile(profile: BehaviourProfile): List<Guardrail> {
        val allGuardrails = Guardrail.all()

        return when (profile) {
            BehaviourProfile.BALANCED_ENGINEER -> {
                // Enable sensible defaults
                allGuardrails.map { g ->
                    when (g.id) {
                        // Enable these
                        "cq_require_tests",
                        "cq_follow_conventions",
                        "sec_input_validation",
                        "arch_avoid_rewrites",
                        "op_backwards_compat" -> g.copy(enabled = true)
                        else -> g.copy(enabled = false)
                    }
                }
            }

            BehaviourProfile.SENIOR_ARCHITECT -> {
                allGuardrails.map { g ->
                    when (g.id) {
                        // Architecture-focused
                        "arch_think_before_coding",
                        "arch_require_plan",
                        "arch_avoid_rewrites",
                        "arch_minimize_deps",
                        "cq_require_docs",
                        "cq_follow_conventions",
                        "op_backwards_compat" -> g.copy(enabled = true)
                        else -> g.copy(enabled = false)
                    }
                }
            }

            BehaviourProfile.SECURITY_ENGINEER -> {
                allGuardrails.map { g ->
                    g.copy(
                        enabled = g.category == GuardrailCategory.SECURITY ||
                                g.id in listOf("cq_require_tests", "op_backwards_compat")
                    )
                }
            }

            BehaviourProfile.DEVOPS_SRE -> {
                allGuardrails.map { g ->
                    when (g.id) {
                        "arch_require_plan",
                        "arch_avoid_rewrites",
                        "op_rollback_plan",
                        "op_backwards_compat",
                        "op_blast_radius",
                        "op_no_cascading",
                        "cq_require_docs",
                        "cq_require_tests",
                        "sec_no_network" -> g.copy(enabled = true)
                        else -> g.copy(enabled = false)
                    }
                }
            }

            BehaviourProfile.RAPID_PROTOTYPE -> {
                allGuardrails.map { g ->
                    when (g.id) {
                        "cq_follow_conventions" -> g.copy(enabled = true)
                        else -> g.copy(enabled = false)
                    }
                }
            }

            BehaviourProfile.ENTERPRISE_CONSULTANT -> {
                allGuardrails.map { g ->
                    g.copy(enabled = true)
                }
            }

            BehaviourProfile.TROUBLESHOOTER -> {
                allGuardrails.map { g ->
                    when (g.id) {
                        "arch_avoid_rewrites",
                        "arch_minimize_deps",
                        "sec_input_validation",
                        "cq_require_tests",
                        "op_rollback_plan" -> g.copy(enabled = true)
                        else -> g.copy(enabled = false)
                    }
                }
            }

            BehaviourProfile.TEACHING_MODE -> {
                allGuardrails.map { g ->
                    when (g.id) {
                        "cq_require_docs",
                        "cq_require_comments",
                        "cq_follow_conventions",
                        "cq_require_tests" -> g.copy(enabled = true)
                        else -> g.copy(enabled = false)
                    }
                }
            }

            BehaviourProfile.MINIMALIST -> {
                allGuardrails.map { g ->
                    when (g.id) {
                        "arch_minimize_deps",
                        "arch_avoid_rewrites",
                        "cq_focused_functions",
                        "sec_no_network",
                        "sec_no_secrets" -> g.copy(enabled = true)
                        else -> g.copy(enabled = false)
                    }
                }
            }
        }
    }

    override fun getProfileDescription(profile: BehaviourProfile): String = when (profile) {
        BehaviourProfile.BALANCED_ENGINEER ->
            "Sensible defaults. Build good code today that you won't regret tomorrow."

        BehaviourProfile.SENIOR_ARCHITECT ->
            "Architecture-first. Design for clarity, extensibility, and future team members."

        BehaviourProfile.SECURITY_ENGINEER ->
            "Security-focused. Fail safely; assume hostile actors."

        BehaviourProfile.DEVOPS_SRE ->
            "Operations-first. Build to be deployed, monitored, and fixed in production."

        BehaviourProfile.RAPID_PROTOTYPE ->
            "Speed-focused. Get it working first; optimise later."

        BehaviourProfile.ENTERPRISE_CONSULTANT ->
            "Comprehensive. Document everything and make compliance auditable."

        BehaviourProfile.TROUBLESHOOTER ->
            "Diagnosis-first. Diagnose before you operate."

        BehaviourProfile.TEACHING_MODE ->
            "Educational. Build understanding, not just code."

        BehaviourProfile.MINIMALIST ->
            "Essential-only. Do the simplest thing that could possibly work."
    }
}

