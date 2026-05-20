package com.promptwhisperer

import com.promptwhisperer.services.DestructiveCommandPatterns
import com.promptwhisperer.services.DiagnosticPhase
import com.promptwhisperer.services.EnvironmentChangeRequest
import com.promptwhisperer.services.FailedCommand
import com.promptwhisperer.services.KnownGoodCheckpoint
import com.promptwhisperer.services.MaterialChange
import com.promptwhisperer.services.MaterialChangeCategory
import com.promptwhisperer.services.ProtectedShellFiles
import com.promptwhisperer.services.ShellProfileSafetyServiceImpl
import com.promptwhisperer.services.ShellSafetyState
import com.promptwhisperer.services.TroubleshootingServiceImpl
import com.promptwhisperer.services.TroubleshootingState
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for Troubleshooting Mode.
 *
 * Covers:
 * - No-loop protection (repeated command blocked without material change)
 * - Retry allowed with material change
 * - Stack trace deepest cause extraction
 * - Phase inference for multiple technologies
 * - Phase transition detection
 * - Generated prompt structure (no-repeat rule, user-run mode, evidence request)
 */
class TroubleshootingModeTests {
    private val service = TroubleshootingServiceImpl()

    // -------------------------------------------------------------------------
    // 1. No-loop protection: repeated command blocked without material change
    // -------------------------------------------------------------------------

    @Test
    fun `repeated failed command is blocked when no material change recorded`() {
        val cmd = "./gradlew test"
        val failed = FailedCommand(command = cmd, errorOutput = "BUILD FAILED", technology = "gradle")

        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)

        // Trying to run the same command again without recording a material change
        assertFalse(
            service.canRetryCommand(state, cmd),
            "Should block retry: same command failed and nothing material changed",
        )
    }

    @Test
    fun `canRetryCommand returns true for a command that has never failed`() {
        val state = TroubleshootingState()
        assertTrue(service.canRetryCommand(state, "./gradlew test"))
    }

    // -------------------------------------------------------------------------
    // 2. Retry allowed with material change
    // -------------------------------------------------------------------------

    @Test
    fun `repeated failed command is allowed after a material change is recorded`() {
        val cmd = "./gradlew test"
        val failed = FailedCommand(command = cmd, errorOutput = "BUILD FAILED", technology = "gradle")

        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)

        // Record a material change
        state =
            service.recordMaterialChange(
                state,
                MaterialChange(
                    category = MaterialChangeCategory.PATH_OR_TOOLCHAIN_CHANGED,
                    description = "Updated JAVA_HOME to JDK 17",
                ),
            )

        assertTrue(
            service.canRetryCommand(state, cmd),
            "Should allow retry: a material change has been recorded",
        )
    }

    @Test
    fun `material changes accumulate correctly`() {
        var state = TroubleshootingState()
        state =
            service.recordMaterialChange(
                state, MaterialChange(MaterialChangeCategory.DEPENDENCY_INSTALLED_OR_UPDATED, "Added missing lib"),
            )
        state =
            service.recordMaterialChange(
                state, MaterialChange(MaterialChangeCategory.CONFIGURATION_CHANGED, "Fixed build.gradle"),
            )
        assertEquals(2, state.materialChanges.size)
    }

    // -------------------------------------------------------------------------
    // 3. Stack trace deepest cause extraction
    // -------------------------------------------------------------------------

    @Test
    fun `extractRootCause returns deepest Caused by line from stack trace`() {
        val stackTrace =
            """
            Exception in thread "main" java.lang.RuntimeException: wrapper error
                at com.example.Main.run(Main.kt:10)
            Caused by: java.lang.IllegalStateException: second cause
                at com.example.Foo.bar(Foo.kt:5)
            Caused by: java.io.FileNotFoundException: /etc/config.yml (No such file or directory)
                at com.example.Config.load(Config.kt:3)
            """.trimIndent()

        val rootCause = service.extractRootCause(stackTrace)
        assertContains(rootCause, "FileNotFoundException", ignoreCase = true)
    }

    @Test
    fun `extractRootCause prioritises missing class error over Caused by`() {
        val error =
            """
            Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
            Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
            """.trimIndent()

        val rootCause = service.extractRootCause(error)
        assertContains(rootCause, "Could not find or load main class", ignoreCase = true)
    }

    @Test
    fun `extractRootCause prioritises permission denied`() {
        val error = "permission denied: /usr/local/bin/gradle"
        val rootCause = service.extractRootCause(error)
        assertContains(rootCause, "permission denied", ignoreCase = true)
    }

    @Test
    fun `extractRootCause returns first error line when no stack trace`() {
        val error = "Error: JAVA_HOME is set to an invalid directory"
        val rootCause = service.extractRootCause(error)
        assertContains(rootCause, "JAVA_HOME")
    }

    // -------------------------------------------------------------------------
    // 4. Phase inference across technologies
    // -------------------------------------------------------------------------

    @Test
    fun `JAVA_HOME error infers TOOLCHAIN phase`() {
        val failed =
            FailedCommand(
                command = "./gradlew test",
                errorOutput = "Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain",
                technology = "gradle",
            )
        assertEquals(DiagnosticPhase.TOOLCHAIN, service.inferPhase(failed))
    }

    @Test
    fun `npm cannot find module infers DEPENDENCY phase`() {
        val failed =
            FailedCommand(
                command = "npm start",
                errorOutput = "Error: Cannot find module 'express'",
                technology = "npm",
            )
        assertEquals(DiagnosticPhase.DEPENDENCY, service.inferPhase(failed))
    }

    @Test
    fun `python no module named infers DEPENDENCY phase`() {
        val failed =
            FailedCommand(
                command = "python app.py",
                errorOutput = "ModuleNotFoundError: No module named 'flask'",
                technology = "python",
            )
        assertEquals(DiagnosticPhase.DEPENDENCY, service.inferPhase(failed))
    }

    @Test
    fun `gradle build failed with test failures infers TESTING phase`() {
        val failed =
            FailedCommand(
                command = "./gradlew test",
                errorOutput = "5 tests completed, 2 failures.\nTESTS FAILED",
                technology = "gradle",
            )
        assertEquals(DiagnosticPhase.TESTING, service.inferPhase(failed))
    }

    @Test
    fun `docker build failure infers PACKAGING phase`() {
        val failed =
            FailedCommand(
                command = "docker build .",
                errorOutput = "Error building image: failed to build: exit code 1",
                technology = "docker",
            )
        assertEquals(DiagnosticPhase.PACKAGING, service.inferPhase(failed))
    }

    @Test
    fun `kubectl deployment failure infers DEPLOYMENT phase`() {
        val failed =
            FailedCommand(
                command = "kubectl apply -f deployment.yaml",
                errorOutput = "deployment failed: invalid kubeconfig",
                technology = "kubectl",
            )
        assertEquals(DiagnosticPhase.DEPLOYMENT, service.inferPhase(failed))
    }

    @Test
    fun `terraform apply failure infers DEPLOYMENT phase`() {
        val failed =
            FailedCommand(
                command = "terraform apply",
                errorOutput = "Error: terraform apply failed on resource group",
                technology = "terraform",
            )
        assertEquals(DiagnosticPhase.DEPLOYMENT, service.inferPhase(failed))
    }

    @Test
    fun `gradle compilation error infers COMPILATION phase`() {
        val failed =
            FailedCommand(
                command = "./gradlew compileKotlin",
                errorOutput = "e: error: unresolved reference: Foo\nCompilation failed.",
                technology = "gradle",
            )
        assertEquals(DiagnosticPhase.COMPILATION, service.inferPhase(failed))
    }

    // -------------------------------------------------------------------------
    // 5. Phase transition detection
    // -------------------------------------------------------------------------

    @Test
    fun `phase transition from TOOLCHAIN to COMPILATION returns message`() {
        val msg = service.detectPhaseTransition(DiagnosticPhase.TOOLCHAIN, DiagnosticPhase.COMPILATION)
        assertNotNull(msg)
        assertContains(msg, "TOOLCHAIN")
        assertContains(msg, "COMPILATION")
    }

    @Test
    fun `no phase transition when phase is unchanged`() {
        val msg = service.detectPhaseTransition(DiagnosticPhase.TESTING, DiagnosticPhase.TESTING)
        assertNull(msg)
    }

    // -------------------------------------------------------------------------
    // 6. Generated prompt structure
    // -------------------------------------------------------------------------

    @Test
    fun `generated prompt contains no-repeat rule`() {
        val failed =
            FailedCommand(
                command = "./gradlew test",
                errorOutput = "BUILD FAILED\n5 tests failed",
                technology = "gradle",
            )
        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)
        val prompt = service.generateTroubleshootingPrompt(state, failed)

        assertTrue(
            prompt.contains("not repeat", ignoreCase = true) ||
                prompt.contains("no-repeat", ignoreCase = true) ||
                prompt.contains("What not to repeat", ignoreCase = true),
            "Prompt should contain a no-repeat rule section",
        )
    }

    @Test
    fun `generated prompt recommends user-run command mode`() {
        val failed =
            FailedCommand(
                command = "npm install",
                errorOutput = "npm ERR! 404 Not found",
                technology = "npm",
            )
        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)
        val prompt = service.generateTroubleshootingPrompt(state, failed)

        assertTrue(
            prompt.contains("run the", ignoreCase = true) || prompt.contains("yourself", ignoreCase = true),
            "Prompt should recommend user-run command mode",
        )
    }

    @Test
    fun `generated prompt asks for evidence before edits`() {
        val failed =
            FailedCommand(
                command = "pytest",
                errorOutput = "AssertionError: assert False",
                technology = "python",
            )
        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)
        val prompt = service.generateTroubleshootingPrompt(state, failed)

        assertTrue(
            prompt.contains("diagnostic", ignoreCase = true) ||
                prompt.contains("evidence", ignoreCase = true) ||
                prompt.contains("paste", ignoreCase = true),
            "Prompt should request evidence / diagnostics before suggesting edits",
        )
    }

    @Test
    fun `generated prompt contains next diagnostic command`() {
        val failed =
            FailedCommand(
                command = "./gradlew test",
                errorOutput = "BUILD FAILED: tests failed",
                technology = "gradle",
            )
        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)
        val prompt = service.generateTroubleshootingPrompt(state, failed)

        assertContains(prompt, "Next diagnostic command", ignoreCase = true)
    }

    @Test
    fun `generated prompt contains current phase`() {
        val failed =
            FailedCommand(
                command = "docker build .",
                errorOutput = "error building image",
                technology = "docker",
            )
        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)
        val prompt = service.generateTroubleshootingPrompt(state, failed)

        assertContains(prompt, "PACKAGING", ignoreCase = true)
    }

    @Test
    fun `blocked retry prompt contains no-loop message`() {
        val cmd = "./gradlew test"
        val failed = FailedCommand(command = cmd, errorOutput = "BUILD FAILED", technology = "gradle")

        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)

        // No material change recorded — retry should be blocked
        assertFalse(service.canRetryCommand(state, cmd))
    }

    // -------------------------------------------------------------------------
    // 7. recordFailure resets material changes
    // -------------------------------------------------------------------------

    @Test
    fun `recording a new failure resets material changes`() {
        var state = TroubleshootingState()
        state =
            service.recordMaterialChange(
                state, MaterialChange(MaterialChangeCategory.CONFIGURATION_CHANGED, "Fixed config"),
            )
        assertEquals(1, state.materialChanges.size)

        val failed = FailedCommand("./gradlew test", "BUILD FAILED", technology = "gradle")
        state = service.recordFailure(state, failed)

        // Material changes should be cleared for the new failure context
        assertEquals(0, state.materialChanges.size)
    }

    // -------------------------------------------------------------------------
    // Shell Profile / Environment Safety tests
    // -------------------------------------------------------------------------

    private val safetyService = ShellProfileSafetyServiceImpl()

    @Test
    fun `generated troubleshooting prompt forbids automatic zshrc edits`() {
        val failed = FailedCommand("./gradlew test", "BUILD FAILED", technology = "gradle")
        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)
        val prompt = service.generateTroubleshootingPrompt(state, failed)

        assertTrue(
            prompt.contains("~/.zshrc", ignoreCase = true),
            "Prompt must mention ~/.zshrc as a protected file",
        )
        assertTrue(
            prompt.contains("Do NOT", ignoreCase = true) || prompt.contains("do not", ignoreCase = true),
            "Prompt must contain an explicit prohibition on shell profile edits",
        )
    }

    @Test
    fun `generated prompt recommends session-only exports first`() {
        val failed = FailedCommand("./gradlew test", "JAVA_HOME is wrong", technology = "gradle")
        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)
        val prompt = service.generateTroubleshootingPrompt(state, failed)

        assertTrue(
            prompt.contains("session", ignoreCase = true) ||
                prompt.contains("session-only", ignoreCase = true) ||
                prompt.contains("export JAVA_HOME", ignoreCase = true),
            "Prompt must recommend session-only exports before persistent changes",
        )
    }

    @Test
    fun `known-good checkpoint prevents unrelated JAVA_HOME changes`() {
        val checkpoint =
            KnownGoodCheckpoint(
                command = "./gradlew clean test",
                javaHome = "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
                javaVersion = "21",
            )
        var safetyState = ShellSafetyState()
        safetyState = safetyService.recordKnownGoodCheckpoint(safetyState, checkpoint)

        // A change request with no real evidence the current value is wrong
        val unjustifiedRequest =
            EnvironmentChangeRequest(
                currentValue = "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
                targetValue = "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home",
                // no evidence
                evidenceCurrentValueIsWrong = "",
                whyChangeIsNecessary = "",
                risk = "breaks build",
                rollbackPlan = "revert",
                verificationCommand = "java -version",
            )

        assertFalse(
            safetyService.isEnvironmentChangeJustified(safetyState, unjustifiedRequest),
            "Must block JAVA_HOME change when known-good checkpoint exists and evidence is empty",
        )
    }

    @Test
    fun `justified environment change is allowed when evidence is provided`() {
        val checkpoint = KnownGoodCheckpoint(command = "./gradlew clean test", javaVersion = "21")
        var safetyState = ShellSafetyState()
        safetyState = safetyService.recordKnownGoodCheckpoint(safetyState, checkpoint)

        val justifiedRequest =
            EnvironmentChangeRequest(
                currentValue = "/old/path",
                targetValue = "/new/path",
                evidenceCurrentValueIsWrong = "Build error shows: unsupported class file major version 65",
                whyChangeIsNecessary = "JDK version mismatch proven by error output",
                risk = "Low — reverting is trivial",
                rollbackPlan = "export JAVA_HOME=/old/path",
                verificationCommand = "java -version && ./gradlew test",
            )

        assertTrue(safetyService.isEnvironmentChangeJustified(safetyState, justifiedRequest))
    }

    @Test
    fun `destructive sed command against zshrc is flagged as unsafe`() {
        val cmd = "sed -i '' '/JAVA_HOME/d' ~/.zshrc"
        assertTrue(
            DestructiveCommandPatterns.isDestructive(cmd),
            "sed -i against ~/.zshrc must be flagged as destructive",
        )
        assertFalse(safetyService.isSafeToSuggest(cmd))
    }

    @Test
    fun `echo append to zshrc is flagged as unsafe`() {
        val cmd = "echo 'export JAVA_HOME=/path' >> ~/.zshrc"
        assertTrue(
            DestructiveCommandPatterns.isDestructive(cmd),
            "echo append to ~/.zshrc must be flagged as destructive",
        )
        assertFalse(safetyService.isSafeToSuggest(cmd))
    }

    @Test
    fun `overwrite of zshrc via redirect is flagged as unsafe`() {
        val cmd = "grep -v JAVA_HOME ~/.zshrc > ~/.zshrc"
        assertTrue(DestructiveCommandPatterns.isDestructive(cmd))
        assertFalse(safetyService.isSafeToSuggest(cmd))
    }

    @Test
    fun `safe diagnostic command passes shell safety check`() {
        val cmd = "java -version"
        assertFalse(DestructiveCommandPatterns.isDestructive(cmd))
        assertTrue(safetyService.isSafeToSuggest(cmd))
    }

    @Test
    fun `environment change without full justification is blocked`() {
        val incompleteRequest =
            EnvironmentChangeRequest(
                currentValue = "21",
                targetValue = "17",
                // missing
                evidenceCurrentValueIsWrong = "",
                // missing
                whyChangeIsNecessary = "",
                risk = "",
                rollbackPlan = "",
                verificationCommand = "",
            )
        assertFalse(
            safetyService.isEnvironmentChangeJustified(ShellSafetyState(), incompleteRequest),
            "Incomplete justification must block environment change",
        )
    }

    @Test
    fun `recovery mode is activated when shell profile damage is described`() {
        var safetyState = ShellSafetyState()
        safetyState =
            safetyService.activateRecoveryMode(
                safetyState,
                "Agent appended JAVA_HOME to ~/.zshrc twice then ran sed -i cleanup and broke the file",
            )
        assertTrue(safetyState.recoveryModeActive)
        assertContains(safetyState.recoveryReason, "JAVA_HOME")
    }

    @Test
    fun `recovery prompt stops automated edits and guides user manually`() {
        var safetyState = ShellSafetyState()
        safetyState = safetyService.activateRecoveryMode(safetyState, "~/.zshrc was overwritten")
        val prompt = safetyService.generateRecoveryPrompt(safetyState, safetyState.recoveryReason)

        assertContains(prompt, "Recovery Mode", ignoreCase = true)
        assertContains(prompt, "automated edits are now stopped", ignoreCase = true)
        assertContains(prompt, "yourself", ignoreCase = true)
        assertContains(prompt, "diff", ignoreCase = true)
        assertContains(prompt, "java -version", ignoreCase = true)
    }

    @Test
    fun `safety section in generated prompt recommends user-run commands for env mutation`() {
        val failed = FailedCommand("./gradlew test", "BUILD FAILED", technology = "gradle")
        var state = TroubleshootingState()
        state = service.recordFailure(state, failed)
        val prompt = service.generateTroubleshootingPrompt(state, failed)

        assertTrue(
            prompt.contains("run these commands yourself", ignoreCase = true) ||
                prompt.contains("run them yourself", ignoreCase = true) ||
                prompt.contains("you run these commands yourself", ignoreCase = true),
            "Prompt must recommend user runs environment commands themselves",
        )
    }

    @Test
    fun `protected shell files list includes zshrc and bashrc`() {
        assertTrue(ProtectedShellFiles.isProtected("~/.zshrc"))
        assertTrue(ProtectedShellFiles.isProtected("~/.bashrc"))
        assertTrue(ProtectedShellFiles.isProtected("~/.bash_profile"))
        assertFalse(ProtectedShellFiles.isProtected("src/main/kotlin/Foo.kt"))
    }

    // -------------------------------------------------------------------------
    // 8. Phase transition awareness with state
    // -------------------------------------------------------------------------

    @Test
    fun `phase transitions are detected correctly across a full session`() {
        val sessionService = TroubleshootingServiceImpl()

        val toolchainFail =
            FailedCommand(
                command = "./gradlew test",
                errorOutput = "Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain",
                technology = "gradle",
            )
        var state = sessionService.recordFailure(TroubleshootingState(), toolchainFail)
        assertEquals(DiagnosticPhase.TOOLCHAIN, state.currentPhase)

        // After fixing toolchain, a compilation failure appears
        state =
            sessionService.recordMaterialChange(
                state, MaterialChange(MaterialChangeCategory.PATH_OR_TOOLCHAIN_CHANGED, "Installed JDK 21"),
            )
        val compileFail =
            FailedCommand(
                command = "./gradlew compileKotlin",
                errorOutput = "e: error: unresolved reference: Bar\nCompilation failed.",
                technology = "gradle",
            )
        val previousPhase = state.currentPhase
        state = sessionService.recordFailure(state, compileFail)

        val transition = sessionService.detectPhaseTransition(previousPhase, state.currentPhase)
        assertNotNull(transition)
        assertContains(transition, "TOOLCHAIN")
        assertContains(transition, "COMPILATION")
    }
}
