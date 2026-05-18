package com.promptwhisperer.services

/**
 * DiagnosticPhase represents the current phase of a troubleshooting session.
 * Allows phase-aware diagnosis and phase-transition messaging across all technologies.
 */
enum class DiagnosticPhase {
    ENVIRONMENT,
    TOOLCHAIN,
    DEPENDENCY,
    CONFIGURATION,
    COMPILATION,
    TESTING,
    RUNTIME,
    PACKAGING,
    DEPLOYMENT,
    UNKNOWN
}

/**
 * Represents a command that has failed, the error it produced,
 * and (optionally) the phase it belongs to.
 */
data class FailedCommand(
    val command: String,
    val errorOutput: String,
    val phase: DiagnosticPhase = DiagnosticPhase.UNKNOWN,
    val technology: String = ""  // e.g. "gradle", "npm", "docker"
)

/**
 * Represents a fix that was attempted in response to a failure.
 * Records whether it produced a material change.
 */
data class AttemptedFix(
    val description: String,
    val materialChange: Boolean
)

/**
 * Represents a material change: something that justifies retrying a previously failed command.
 * A retry is only allowed when at least one MaterialChange has been recorded since the last failure.
 */
data class MaterialChange(
    val category: MaterialChangeCategory,
    val description: String
)

/**
 * Categories of material change that can justify retrying a failed command.
 */
enum class MaterialChangeCategory {
    CONFIGURATION_CHANGED,
    DEPENDENCY_INSTALLED_OR_UPDATED,
    SOURCE_CODE_CHANGED,
    ENVIRONMENT_VARIABLE_CHANGED,
    PATH_OR_TOOLCHAIN_CHANGED,
    CACHE_OR_DAEMON_CLEARED,
    PERMISSIONS_CHANGED,
    MISSING_FILE_RESTORED,
    COMMAND_ARGUMENTS_CHANGED,
    TEST_DATA_CHANGED
}

/**
 * The full state of a troubleshooting session.
 * Tracks all evidence, attempts, material changes and the current blocker.
 */
data class TroubleshootingState(
    val failedCommands: List<FailedCommand> = emptyList(),
    val attemptedFixes: List<AttemptedFix> = emptyList(),
    val materialChanges: List<MaterialChange> = emptyList(),
    val resolvedBlockers: List<String> = emptyList(),
    val currentBlocker: String? = null,
    val currentPhase: DiagnosticPhase = DiagnosticPhase.UNKNOWN,
    val nextSuggestedCommand: String? = null,
    val userShouldRunCommand: Boolean = true
)

// =============================================================================
// Shell Profile / Environment Safety
// Hard safety principle: Fix the current problem without mutating the user's machine.
// =============================================================================

/**
 * Files that are treated as high-risk and must never be modified automatically.
 * Any suggestion to write to these files must be shown to the user for manual application.
 */
object ProtectedShellFiles {
    val paths = setOf(
        "~/.zshrc",
        "~/.bashrc",
        "~/.bash_profile",
        "~/.profile",
        "~/.config",
        "~/.gradle/gradle.properties",
        "~/.npmrc",
        "~/.pip/pip.conf",
        "~/.m2/settings.xml"
    )

    /** Returns true if the given command or path targets a protected shell/config file. */
    fun isProtected(target: String): Boolean =
        paths.any { target.contains(it, ignoreCase = true) }
}

/**
 * Patterns that indicate a destructive or risky automated shell mutation.
 * These must never be suggested or run automatically.
 */
object DestructiveCommandPatterns {
    private val patterns = listOf(
        Regex("""sed\s+-i.*~\/\.(zshrc|bashrc|bash_profile|profile)"""),
        Regex("""grep.*>.*~\/\.(zshrc|bashrc|bash_profile|profile)"""),
        Regex("""rm\s+.*~\/\.(zshrc|bashrc|bash_profile|profile)"""),
        Regex(""">\s*~\/\.(zshrc|bashrc|bash_profile|profile)"""),   // overwrite
        Regex("""echo\s+.*>>\s*~\/\.(zshrc|bashrc|bash_profile|profile)"""),  // append
        Regex("""cp\s+.*~\/\.(zshrc|bashrc|bash_profile|profile)""")  // overwrite via cp
    )

    /** Returns true if the command matches a pattern that would destructively modify a shell profile. */
    fun isDestructive(command: String): Boolean =
        patterns.any { it.containsMatchIn(command) }
}

/**
 * A known-good checkpoint: a build/test command that the user has confirmed succeeds.
 * Once set, environment variables, toolchain versions, wrapper versions and
 * shell startup files must not be changed unless the current failure directly
 * proves they are the cause.
 */
data class KnownGoodCheckpoint(
    val command: String,           // e.g. "./gradlew clean test"
    val javaHome: String = "",
    val javaVersion: String = "",
    val gradleVersion: String = "",
    val nodeVersion: String = "",
    val pythonVersion: String = "",
    val notes: String = ""
)

/**
 * Justification required before proposing any environment or version change.
 * If this is not fully filled in, the change must not be suggested.
 */
data class EnvironmentChangeRequest(
    val currentValue: String,
    val targetValue: String,
    val evidenceCurrentValueIsWrong: String,
    val whyChangeIsNecessary: String,
    val risk: String,
    val rollbackPlan: String,
    val verificationCommand: String
) {
    /** Returns true only if all fields are non-blank — change is justified. */
    fun isJustified(): Boolean = listOf(
        currentValue, targetValue, evidenceCurrentValueIsWrong,
        whyChangeIsNecessary, risk, rollbackPlan, verificationCommand
    ).all { it.isNotBlank() }
}

/**
 * Tracks whether Recovery Mode is active.
 * Recovery Mode is triggered when shell profile damage is detected.
 * In Recovery Mode all automated edits stop and the user is guided manually.
 */
data class ShellSafetyState(
    val knownGoodCheckpoint: KnownGoodCheckpoint? = null,
    val recoveryModeActive: Boolean = false,
    val recoveryReason: String = "",
    val pendingEnvironmentChange: EnvironmentChangeRequest? = null
)

/**
 * ShellProfileSafetyService enforces environment safety rules.
 * It detects risky commands, validates environment change justifications,
 * protects known-good checkpoints and activates Recovery Mode when needed.
 */
interface ShellProfileSafetyService {
    /** Record a known-good checkpoint (user confirmed this command succeeds). */
    fun recordKnownGoodCheckpoint(state: ShellSafetyState, checkpoint: KnownGoodCheckpoint): ShellSafetyState

    /** Returns true if the command is safe to suggest (not a shell profile mutation). */
    fun isSafeToSuggest(command: String): Boolean

    /**
     * Returns true if the proposed environment change is justified given the
     * known-good checkpoint and the provided justification.
     */
    fun isEnvironmentChangeJustified(
        state: ShellSafetyState,
        request: EnvironmentChangeRequest
    ): Boolean

    /** Activate Recovery Mode with the reason why profile damage was detected. */
    fun activateRecoveryMode(state: ShellSafetyState, reason: String): ShellSafetyState

    /**
     * Generate a shell-profile safety section to embed in troubleshooting prompts.
     * This section warns against automatic shell mutations and recommends session-only exports.
     */
    fun generateSafetySection(state: ShellSafetyState): String

    /** Generate a Recovery Mode prompt when shell profile damage is detected. */
    fun generateRecoveryPrompt(state: ShellSafetyState, damageDescription: String): String
}

/**
 * Default implementation of ShellProfileSafetyService.
 */
class ShellProfileSafetyServiceImpl : ShellProfileSafetyService {

    override fun recordKnownGoodCheckpoint(
        state: ShellSafetyState,
        checkpoint: KnownGoodCheckpoint
    ): ShellSafetyState = state.copy(knownGoodCheckpoint = checkpoint)

    override fun isSafeToSuggest(command: String): Boolean {
        if (DestructiveCommandPatterns.isDestructive(command)) return false
        if (ProtectedShellFiles.isProtected(command)) return false
        return true
    }

    override fun isEnvironmentChangeJustified(
        state: ShellSafetyState,
        request: EnvironmentChangeRequest
    ): Boolean {
        if (!request.isJustified()) return false
        // If a known-good checkpoint exists, the change needs direct evidence
        // that the current value is actually wrong — not just a guess.
        if (state.knownGoodCheckpoint != null) {
            return request.evidenceCurrentValueIsWrong.isNotBlank() &&
                    request.whyChangeIsNecessary.isNotBlank()
        }
        return true
    }

    override fun activateRecoveryMode(state: ShellSafetyState, reason: String): ShellSafetyState =
        state.copy(recoveryModeActive = true, recoveryReason = reason)

    override fun generateSafetySection(state: ShellSafetyState): String {
        val checkpointNote = if (state.knownGoodCheckpoint != null) {
            """
### 🔒 Known-good checkpoint protected
The following command was confirmed working — do not change the environment unless the current failure directly proves it is the cause:
```
${state.knownGoodCheckpoint.command}
```
Do not change: JAVA_HOME, PATH, JVM version, Gradle wrapper version, Kotlin version, or shell startup files.
""".trimIndent()
        } else ""

        return """
### 🛡️ Shell Profile and Environment Safety

> **Hard rule: Fix the current problem without mutating the user's machine.**

- ❌ Do NOT write to `~/.zshrc`, `~/.bashrc`, `~/.bash_profile` or any shell startup file automatically.
- ❌ Do NOT run `sed -i`, `grep -v >`, `rm`, or `echo >>` against shell profile files.
- ❌ Do NOT append duplicate `export JAVA_HOME` or `export PATH` lines.
- ✅ For environment troubleshooting, use **session-only** exports first:
  ```
  export JAVA_HOME=/path/to/jdk   # session only — not written to any file
  ```
- ✅ If a permanent change is needed, show the user the exact line and ask them to add it manually.
- ✅ For environment commands, the user should run them and paste output back — not the agent.

> "For environment changes, I recommend you run these commands yourself rather than letting an agent mutate your shell profile."

$checkpointNote
""".trimIndent()
    }

    override fun generateRecoveryPrompt(state: ShellSafetyState, damageDescription: String): String {
        return """
## 🚨 Recovery Mode — Shell Profile Damage Detected

**Reason:** $damageDescription

All automated edits are now stopped.

### Recovery steps (run these yourself — do not let the agent run them)

1. Check whether a backup exists:
   ```
   ls -la ~/.zshrc.backup* ~/.zshrc.bak* 2>/dev/null || echo "No backup found"
   ```

2. If a backup exists, inspect the diff before restoring:
   ```
   diff ~/.zshrc <backup-file>
   ```

3. Restore only with your confirmation — apply minimal manual edits, not a full overwrite.

4. After restoring, verify your environment:
   ```
   echo "JAVA_HOME=${'$'}JAVA_HOME"
   which java
   java -version
   ```

5. Verify the project build:
   ```
   ./gradlew clean test
   ```

### What NOT to do
- Do not let the agent run `sed -i` against your shell profile.
- Do not let the agent `cp` a backup over your current file without reviewing the diff.
- Do not proceed to code changes until your shell environment is verified working.
""".trimIndent()
    }
}

/**
 * TroubleshootingService is the core of Troubleshooting Mode.
 * It enforces the no-loop rule, identifies phases, extracts root causes
 * from stack traces, generates structured prompts, and detects phase transitions.
 */
interface TroubleshootingService {
    /** Add a newly failed command to the state. */
    fun recordFailure(state: TroubleshootingState, failed: FailedCommand): TroubleshootingState

    /** Record a material change that occurred since the last failure. */
    fun recordMaterialChange(state: TroubleshootingState, change: MaterialChange): TroubleshootingState

    /** Record an attempted fix. */
    fun recordAttemptedFix(state: TroubleshootingState, fix: AttemptedFix): TroubleshootingState

    /**
     * Check whether retrying the given command is allowed.
     * Returns false if the same command was already in failedCommands and no material change has been recorded since.
     */
    fun canRetryCommand(state: TroubleshootingState, command: String): Boolean

    /**
     * Infer the DiagnosticPhase from a FailedCommand's error output and technology.
     * Uses keyword matching across multiple technologies.
     */
    fun inferPhase(failed: FailedCommand): DiagnosticPhase

    /**
     * Extract the deepest meaningful cause from a stack trace or error output.
     * Prioritises: missing classes, missing modules, missing files, permission errors,
     * version mismatches, authentication failures.
     */
    fun extractRootCause(errorOutput: String): String

    /**
     * Generate a structured troubleshooting prompt in the standard output format.
     */
    fun generateTroubleshootingPrompt(state: TroubleshootingState, failedCommand: FailedCommand): String

    /**
     * Detect whether a phase transition occurred between the previous and current failure.
     * Returns a transition message if the phase changed, or null if it did not.
     */
    fun detectPhaseTransition(previousPhase: DiagnosticPhase, currentPhase: DiagnosticPhase): String?
}

/**
 * Default implementation of TroubleshootingService.
 * Technology-agnostic: works for Gradle, npm, pip, Docker, Terraform, Kubernetes, cloud CLIs, etc.
 */
class TroubleshootingServiceImpl : TroubleshootingService {

    override fun recordFailure(state: TroubleshootingState, failed: FailedCommand): TroubleshootingState {
        val phase = inferPhase(failed)
        val rootCause = extractRootCause(failed.errorOutput)
        return state.copy(
            failedCommands = state.failedCommands + failed,
            currentBlocker = rootCause.ifBlank { failed.command },
            currentPhase = phase,
            materialChanges = emptyList() // reset material changes for this failure
        )
    }

    override fun recordMaterialChange(state: TroubleshootingState, change: MaterialChange): TroubleshootingState {
        return state.copy(materialChanges = state.materialChanges + change)
    }

    override fun recordAttemptedFix(state: TroubleshootingState, fix: AttemptedFix): TroubleshootingState {
        return state.copy(attemptedFixes = state.attemptedFixes + fix)
    }

    override fun canRetryCommand(state: TroubleshootingState, command: String): Boolean {
        val normalised = command.trim()
        val hasFailed = state.failedCommands.any { it.command.trim() == normalised }
        if (!hasFailed) return true
        // A retry is only allowed when at least one material change has been recorded.
        return state.materialChanges.isNotEmpty()
    }

    override fun inferPhase(failed: FailedCommand): DiagnosticPhase {
        val text = (failed.errorOutput + " " + failed.command).lowercase()
        val tech = failed.technology.lowercase()

        // TOOLCHAIN: version, JDK, JVM, node, python, runtime engine issues
        if (text.containsAny(
                "could not find or load main class",
                "no such file or directory: java",
                "no such file or directory: node",
                "no such file or directory: python",
                "unsupported class file major version",
                "invalid source release",
                "jdk not found",
                "java_home",
                "command not found: gradle",
                "command not found: node",
                "command not found: python",
                "command not found: docker",
                "command not found: kubectl",
                "command not found: terraform",
                "nvm is not",
                "sdk not installed",
                "sdkman"
            )
        ) return DiagnosticPhase.TOOLCHAIN

        // DEPENDENCY: missing libraries, unresolved imports, artifact resolution
        if (text.containsAny(
                "could not resolve",
                "could not find artifact",
                "module not found",
                "cannot find module",
                "no module named",
                "package not found",
                "failed to download",
                "error downloading",
                "could not get unknown property",
                "peer dep"
            )
        ) return DiagnosticPhase.DEPENDENCY

        // CONFIGURATION: build files, config files, property files, env files
        if (text.containsAny(
                "build.gradle",
                "settings.gradle",
                "pom.xml",
                "package.json",
                "tsconfig",
                "pyproject.toml",
                "requirements.txt",
                "dockerfile",
                "docker-compose",
                "terraform.tfvars",
                ".env",
                "application.yml",
                "application.properties",
                "could not find property",
                "invalid configuration",
                "configuration cache"
            )
        ) return DiagnosticPhase.CONFIGURATION

        // COMPILATION: syntax errors, type errors, compile errors
        if (text.containsAny(
                "compilation failed",
                "error: unresolved reference",
                "syntaxerror",
                "typeerror",
                "cannot find symbol",
                "error: (",  // javac error prefix
                "kotlin: error",
                "tsc error",
                "build failed",
                "compilation error"
            )
        ) return DiagnosticPhase.COMPILATION

        // TESTING: test failures, assertion errors
        if (text.containsAny(
                "test failed",
                "tests failed",
                "assertionerror",
                "expected:",
                "junit",
                "pytest",
                "jest",
                "spec failed",
                "failure:",
                "failures:"
            )
        ) return DiagnosticPhase.TESTING

        // PACKAGING: Docker build, jar/zip packaging
        if (text.containsAny(
                "docker build",
                "failed to build image",
                "error building image",
                "invalid dockerfile",
                "jar:",
                "zip:",
                "packaging failed"
            )
        ) return DiagnosticPhase.PACKAGING

        // DEPLOYMENT: kubectl, terraform, cloud CLI, helm, serverless
        if (text.containsAny(
                "kubectl",
                "terraform apply",
                "terraform plan",
                "helm",
                "deployment failed",
                "failed to deploy",
                "azure cli",
                "aws cli",
                "gcloud",
                "serverless deploy",
                "invalid kubeconfig"
            )
        ) return DiagnosticPhase.DEPLOYMENT

        // RUNTIME: app crashes at startup, connection errors, NPEs
        if (text.containsAny(
                "nullpointerexception",
                "segmentation fault",
                "process exited with code",
                "exit code 1",
                "connection refused",
                "address already in use",
                "out of memory",
                "java.lang.runtimeexception",
                "uncaught exception",
                "fatal error"
            )
        ) return DiagnosticPhase.RUNTIME

        // ENVIRONMENT: missing env vars, OS/platform mismatches
        if (text.containsAny(
                "environment variable",
                "env var",
                "permission denied",
                "eacces",
                "no such file or directory",
                "path not found"
            )
        ) return DiagnosticPhase.ENVIRONMENT

        return DiagnosticPhase.UNKNOWN
    }

    override fun extractRootCause(errorOutput: String): String {
        val lines = errorOutput.lines()

        // Priority 1: missing class / main class not found
        lines.firstOrNull { it.lowercase().contains("could not find or load main class") }
            ?.let { return it.trim() }

        // Priority 2: missing module or file
        lines.firstOrNull {
            it.lowercase().containsAny("no module named", "cannot find module", "no such file or directory")
        }?.let { return it.trim() }

        // Priority 3: permission errors
        lines.firstOrNull { it.lowercase().containsAny("permission denied", "eacces") }
            ?.let { return it.trim() }

        // Priority 4: version mismatch
        lines.firstOrNull {
            it.lowercase().containsAny(
                "unsupported class file major version",
                "invalid source release",
                "version mismatch"
            )
        }?.let { return it.trim() }

        // Priority 5: authentication failure
        lines.firstOrNull {
            it.lowercase().containsAny(
                "authentication failed",
                "unauthorized",
                "403 forbidden",
                "401 unauthorized"
            )
        }?.let { return it.trim() }

        // Priority 6: go deeper into stack traces — find the last "Caused by:" line
        val causedByLines = lines.filter { it.trim().startsWith("Caused by:") }
        if (causedByLines.isNotEmpty()) {
            return causedByLines.last().trim()
        }

        // Priority 7: first "error" or "Error" or "ERROR" line
        lines.firstOrNull { line ->
            line.trim().let { t ->
                t.startsWith("Error") || t.startsWith("ERROR") || t.startsWith("error")
            }
        }?.let { return it.trim() }

        // Fallback: first non-blank line
        return lines.firstOrNull { it.isNotBlank() }?.trim() ?: "Unknown error"
    }

    override fun generateTroubleshootingPrompt(
        state: TroubleshootingState,
        failedCommand: FailedCommand
    ): String {
        val rootCause = extractRootCause(failedCommand.errorOutput)
        val phase = inferPhase(failedCommand)
        val canRetry = canRetryCommand(state, failedCommand.command)
        val noRepeatNote = if (!canRetry) {
            "⛔ This command has already failed and nothing material has changed. " +
                    "We need a different diagnostic step before retrying."
        } else {
            "✅ A material change has been recorded — retry may be appropriate."
        }

        val whatNotToRepeat = if (state.failedCommands.isNotEmpty()) {
            state.failedCommands.joinToString("\n") { "  - `${it.command}` (failed: ${it.phase})" }
        } else {
            "  (none yet)"
        }

        val diagnosticCommand = suggestDiagnosticCommand(phase, failedCommand)
        val whyCommand = explainDiagnosticCommand(phase, diagnosticCommand)
        val successLooksLike = describeSuccess(phase, failedCommand)
        val whatToPasteBack = "Paste the complete terminal output of the diagnostic command above, " +
                "including any error lines, stack traces, or version info."

        val safetyService = ShellProfileSafetyServiceImpl()
        val safetySection = safetyService.generateSafetySection(ShellSafetyState())

        return """
## 🔍 Troubleshooting Mode — Prompt Whisperer

**Status:** Active — evidence-based debugging in progress
**Current phase:** ${phase.name}
**Technology:** ${failedCommand.technology.ifBlank { "General" }}

---

### Observed symptom
```
${failedCommand.command}
```
produced:
```
${failedCommand.errorOutput.lines().take(20).joinToString("\n")}
```

### Likely root cause
$rootCause

### Evidence
${if (state.attemptedFixes.isEmpty()) "No fixes attempted yet." else state.attemptedFixes.joinToString("\n") { "- ${it.description}" }}

### Confidence
${confidenceLevel(rootCause)} — based on error keywords and phase classification

### What not to repeat
$whatNotToRepeat

$noRepeatNote

---

### ⚠️ User-run command mode
> Do not ask Copilot Agent to run commands in a loop.
> Run the next command yourself, paste the output back, and Prompt Whisperer will analyse it.
> Use Copilot Agent mode **only** if you want it to inspect files or propose targeted patches.

---

### Next diagnostic command
```
$diagnosticCommand
```

### Why this command
$whyCommand

### What success looks like
$successLooksLike

### What to paste back
$whatToPasteBack

---

$safetySection

---

*One action at a time. Do not proceed to a bigger command until this diagnostic passes.*
""".trimIndent()
    }

    override fun detectPhaseTransition(
        previousPhase: DiagnosticPhase,
        currentPhase: DiagnosticPhase
    ): String? {
        if (previousPhase == currentPhase) return null
        return "✅ The previous issue (${previousPhase.name}) appears resolved. " +
                "The current blocker is now in phase: ${currentPhase.name}."
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun suggestDiagnosticCommand(phase: DiagnosticPhase, failed: FailedCommand): String {
        val tech = failed.technology.lowercase()
        return when (phase) {
            DiagnosticPhase.TOOLCHAIN -> when {
                tech.contains("gradle") || tech.contains("java") -> "java -version && echo \"JAVA_HOME=\$JAVA_HOME\""
                tech.contains("node") || tech.contains("npm") -> "node --version && npm --version"
                tech.contains("python") || tech.contains("pip") -> "python --version && pip --version"
                tech.contains("docker") -> "docker --version && docker info"
                tech.contains("terraform") -> "terraform version"
                tech.contains("kubectl") || tech.contains("kubernetes") -> "kubectl version --client"
                else -> "Check your toolchain: run `<tool> --version` to confirm the correct version is installed and on PATH"
            }
            DiagnosticPhase.DEPENDENCY -> when {
                tech.contains("gradle") -> "./gradlew dependencies --configuration runtimeClasspath"
                tech.contains("npm") -> "npm ls --depth=1"
                tech.contains("python") || tech.contains("pip") -> "pip list"
                tech.contains("docker") -> "docker images"
                else -> "List your dependencies: run your package manager's list/tree command"
            }
            DiagnosticPhase.CONFIGURATION -> when {
                tech.contains("gradle") -> "cat build.gradle.kts && cat settings.gradle.kts && cat gradle.properties"
                tech.contains("npm") -> "cat package.json"
                tech.contains("python") -> "cat pyproject.toml || cat requirements.txt"
                tech.contains("docker") -> "cat Dockerfile"
                tech.contains("terraform") -> "terraform validate"
                tech.contains("kubernetes") || tech.contains("kubectl") -> "kubectl config view"
                else -> "Inspect your configuration files and confirm settings are correct"
            }
            DiagnosticPhase.COMPILATION -> when {
                tech.contains("gradle") -> "./gradlew compileKotlin --info 2>&1 | tail -40"
                tech.contains("npm") || tech.contains("node") -> "npx tsc --noEmit 2>&1 | head -30"
                tech.contains("python") -> "python -m py_compile <your-file>.py"
                else -> "Run the compile step in isolation with verbose output and scroll to the first error"
            }
            DiagnosticPhase.TESTING -> when {
                tech.contains("gradle") -> "./gradlew test --info --no-daemon 2>&1 | tail -60"
                tech.contains("npm") -> "npm test -- --verbose 2>&1 | tail -60"
                tech.contains("python") -> "pytest -v 2>&1 | tail -60"
                else -> "Run the failing test in isolation with verbose output"
            }
            DiagnosticPhase.RUNTIME -> "Check application startup logs: look for the first exception or error line, not the wrapper exception"
            DiagnosticPhase.PACKAGING -> when {
                tech.contains("docker") -> "docker build . --no-cache --progress=plain 2>&1 | tail -60"
                else -> "Re-run the packaging step with verbose output and check for the first error"
            }
            DiagnosticPhase.DEPLOYMENT -> when {
                tech.contains("terraform") -> "terraform plan 2>&1 | tail -60"
                tech.contains("kubectl") || tech.contains("kubernetes") -> "kubectl describe pod <pod-name> 2>&1 | tail -60"
                else -> "Re-run the deployment step with verbose output and check for the first error"
            }
            DiagnosticPhase.ENVIRONMENT -> "printenv | sort | grep -i -E 'java|path|home|node|python|docker' | head -30"
            DiagnosticPhase.UNKNOWN -> "Paste the complete error output including any stack traces so the root cause can be identified"
        }
    }

    private fun explainDiagnosticCommand(phase: DiagnosticPhase, command: String): String {
        return when (phase) {
            DiagnosticPhase.TOOLCHAIN -> "Confirms the exact runtime version and PATH — version mismatches are the most common toolchain failure."
            DiagnosticPhase.DEPENDENCY -> "Shows which dependencies are resolved and at what version — useful to spot missing or conflicting packages."
            DiagnosticPhase.CONFIGURATION -> "Displays the current configuration files so we can spot incorrect settings, missing keys, or syntax problems."
            DiagnosticPhase.COMPILATION -> "Runs only the compile step with verbose output so we can see the first real error without noise from downstream failures."
            DiagnosticPhase.TESTING -> "Runs tests with verbose output so we can see the exact assertion failure and its location."
            DiagnosticPhase.RUNTIME -> "Application startup logs contain the original exception before it is wrapped by the framework."
            DiagnosticPhase.PACKAGING -> "Re-builds the package from scratch (no cache) so we get a clean error without cached layers masking the problem."
            DiagnosticPhase.DEPLOYMENT -> "A dry-run or describe command reveals what the platform sees without applying risky changes."
            DiagnosticPhase.ENVIRONMENT -> "Shows all environment variables relevant to the toolchain so we can confirm PATH and HOME values."
            DiagnosticPhase.UNKNOWN -> "More information is needed to classify the failure before suggesting a targeted fix."
        }
    }

    private fun describeSuccess(phase: DiagnosticPhase, failed: FailedCommand): String {
        return when (phase) {
            DiagnosticPhase.TOOLCHAIN -> "The version command prints the expected version with no errors."
            DiagnosticPhase.DEPENDENCY -> "All required dependencies are listed as resolved with no errors or missing entries."
            DiagnosticPhase.CONFIGURATION -> "Configuration files are syntactically valid and contain the expected keys."
            DiagnosticPhase.COMPILATION -> "The compile step finishes with BUILD SUCCESSFUL or zero errors."
            DiagnosticPhase.TESTING -> "All targeted tests pass with no failures or errors."
            DiagnosticPhase.RUNTIME -> "Application starts without exceptions and responds as expected."
            DiagnosticPhase.PACKAGING -> "Package is built successfully with no layer errors."
            DiagnosticPhase.DEPLOYMENT -> "The deployment or plan command completes without errors and shows the expected resource state."
            DiagnosticPhase.ENVIRONMENT -> "All expected environment variables are present with correct values."
            DiagnosticPhase.UNKNOWN -> "The error output is fully understood and a targeted fix can be applied."
        }
    }

    private fun confidenceLevel(rootCause: String): String {
        return when {
            rootCause.lowercase().containsAny(
                "could not find or load",
                "permission denied",
                "no module named",
                "cannot find module",
                "unsupported class file"
            ) -> "High"
            rootCause.lowercase().containsAny(
                "caused by:",
                "error:",
                "exception:"
            ) -> "Medium"
            else -> "Low"
        }
    }

    // Extension helpers to keep the code readable
    private fun String.containsAny(vararg terms: String): Boolean =
        terms.any { this.contains(it, ignoreCase = true) }
}
