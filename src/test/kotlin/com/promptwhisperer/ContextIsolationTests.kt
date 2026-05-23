package com.promptwhisperer

import com.promptwhisperer.models.ContextScope
import com.promptwhisperer.models.PromptSessionConfig
import com.promptwhisperer.services.ContextContaminationDetector
import com.promptwhisperer.services.DomainProfiler
import com.promptwhisperer.services.EnhancementMemorySnapshot
import com.promptwhisperer.services.EnhancementMemoryStore
import com.promptwhisperer.services.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextIsolationTests {
    @Test
    fun `stateless scope does not retain enhancement memory`() {
        val store = EnhancementMemoryStore()
        val snapshot = EnhancementMemorySnapshot(assumptions = listOf("leaderboard persistence"), taskFingerprint = "abc")

        store.write(ContextScope.STATELESS, "project-a", snapshot)
        val loaded = store.read(ContextScope.STATELESS, "project-a")

        assertTrue(loaded.assumptions.isEmpty())
        assertEquals(null, loaded.taskFingerprint)
    }

    @Test
    fun `project scope isolates memory between projects`() {
        val store = EnhancementMemoryStore()
        store.write(ContextScope.PROJECT, "project-game", EnhancementMemorySnapshot(assumptions = listOf("gameplay loop")))
        store.write(ContextScope.PROJECT, "project-security", EnhancementMemorySnapshot(assumptions = listOf("threat model")))

        val gameAssumptions = store.read(ContextScope.PROJECT, "project-game").assumptions
        val securityAssumptions = store.read(ContextScope.PROJECT, "project-security").assumptions

        assertEquals(listOf("gameplay loop"), gameAssumptions)
        assertEquals(listOf("threat model"), securityAssumptions)
    }

    @Test
    fun `contamination detector flags game assumptions for security task`() {
        val detector = ContextContaminationDetector(DomainProfiler())
        val warnings =
            detector.detect(
                task = "Harden SIEM alert triage workflow and WAF remediation rules",
                inheritedAssumptions = listOf("Add gameplay controls", "Include leaderboard persistence"),
            )

        assertTrue(warnings.isNotEmpty())
        assertTrue(warnings.any { it.contains("leaderboard", ignoreCase = true) })
    }

    @Test
    fun `inference reports contamination warnings when inherited assumptions mismatch domain`() {
        val config =
            PromptSessionConfig(
                inheritedAssumptions = listOf("touch controls", "sound effects", "leaderboard"),
                enableContaminationDetection = true,
            )

        val inference = InferenceEngine().analyze("Security engineering tool for SOC triage", config)

        assertTrue(inference.contaminationWarnings.isNotEmpty())
        assertTrue(inference.contextSources.contains("reused enhancement memory"))
    }
}
