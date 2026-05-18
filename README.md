# Prompt Whisperer

An IntelliJ IDEA plugin that generates structured, evidence-based prompts for AI coding assistants.

---

## Modes

### Standard Mode
Generate implementation prompts for new features, refactors or tasks.
Enter a task description and Prompt Whisperer generates a structured prompt you can copy into Copilot, Claude, or any other AI assistant.

### Troubleshooting Mode
Help Copilot (or any AI assistant) debug failures in a controlled, evidence-based way.
See [Troubleshooting Mode](#troubleshooting-mode) below.

---

## Troubleshooting Mode

### Hard safety principle

> **Fix the current problem without mutating the user's machine.**

### Shell Profile and Environment Safety

Prompt Whisperer enforces strict rules to prevent Copilot from damaging a working shell or build environment.

#### Protected files — never modified automatically

| File | Why protected |
|------|--------------|
| `~/.zshrc` | User shell startup — breaking it breaks every terminal session |
| `~/.bashrc` | Same |
| `~/.bash_profile` | Same |
| `~/.profile` | Same |
| `~/.config/*` | User-level app config |
| `~/.gradle/gradle.properties` | Global Gradle config |
| `~/.npmrc` | Global npm config |
| Global IDE settings | Shared across all projects |

#### Session-only first

For environment troubleshooting, always use a temporary session-only export. **Do not write to shell files automatically.**

```bash
# ✅ Correct — session only, not written to any file
export JAVA_HOME=/path/to/jdk

# ❌ Wrong — never run this automatically
echo 'export JAVA_HOME=/path/to/jdk' >> ~/.zshrc
```

#### Persistence requires explicit user approval

Before suggesting a permanent shell profile change, Prompt Whisperer must:
1. Explain why persistence is needed
2. Show the exact line to add
3. Explain the risk
4. Ask the **user** to apply it manually
5. Recommend taking a timestamped backup first

#### No automated shell profile cleanup

The following commands are **never** suggested or run automatically against shell profiles:

```bash
sed -i '' '/JAVA_HOME/d' ~/.zshrc        # ❌ forbidden
grep -v JAVA_HOME ~/.zshrc > ~/.zshrc    # ❌ forbidden
rm ~/.zshrc                              # ❌ forbidden
echo 'export ...' >> ~/.zshrc            # ❌ forbidden without approval
cp backup ~/.zshrc                       # ❌ forbidden without diff review
```

#### Known-good checkpoint protection

Once a build/test command succeeds, Prompt Whisperer records it as a **known-good checkpoint**. After that:

- Do not change `JAVA_HOME`
- Do not change `PATH`
- Do not change JVM/toolchain versions
- Do not change Gradle wrapper versions
- Do not edit shell startup files

…unless the current failure **directly proves** those are the cause.

#### Environment change justification

Before proposing any environment or version change, Prompt Whisperer requires:

```
Current value:                    <what it is now>
Target value:                     <what you want to change it to>
Evidence current value is wrong:  <exact error proving this>
Why change is necessary:          <reason>
Risk:                             <what could break>
Rollback plan:                    <how to undo>
Verification command:             <how to confirm the fix worked>
```

#### Recovery Mode

If shell profile damage is detected, Prompt Whisperer switches to **Recovery Mode**:
- All automated edits stop immediately
- Backup files are inspected first
- Diff is shown before any restore
- Restore happens only with user confirmation
- Environment is verified with `echo $JAVA_HOME`, `which java`, `java -version`
- Project build is verified last

---

### Real incident: what went wrong and what Prompt Whisperer should do instead

**What happened:**
An agent attempted to fix a Java/Gradle `JAVA_HOME` issue by:
1. Appending `export JAVA_HOME=...` to `~/.zshrc` — twice (duplicate lines).
2. Running `sed -i '' '/JAVA_HOME/d' ~/.zshrc` to clean up — which removed working config.
3. This broke a previously working build.

**What Prompt Whisperer must do instead:**
1. Use a session-only `export JAVA_HOME=...` first — do not write to `~/.zshrc`.
2. Verify the build succeeds with the session-only export.
3. If persistence is needed, show the user the exact line and ask them to add it manually.
4. Never run `sed -i`, `echo >>`, or `rm` against shell profile files.
5. Record the known-good checkpoint once the build passes.
6. Never repeat a failed command without recording a material change.

---

### Why Copilot agent loops happen

When you ask Copilot Agent to fix a build or test failure, it can fall into a loop:
1. Run a command → fails.
2. Change a file or config → run the same command again → fails for the same reason.
3. Repeat.

This wastes time, pollutes your git history, and can make things worse.
The root cause is almost always that **nothing material changed** between attempts.

### How Prompt Whisperer prevents loops

Troubleshooting Mode enforces one rule:

> **Do not repeat a failed command unless something material has changed.**

A **material change** is one of:
- Configuration changed
- Dependency installed or updated
- Source code changed
- Environment variable changed
- PATH or toolchain changed
- Cache or daemon cleared
- Permissions changed
- Missing file restored
- Command arguments changed deliberately
- Test data changed

If you try to re-analyse the same failing command without recording a material change, Prompt Whisperer blocks the retry and explains why.

### Recommended workflow

```
1. Run the failing command yourself in Terminal.
2. Copy the exact command and its full output.
3. Open Prompt Whisperer → switch to Troubleshooting Mode.
4. Fill in: failed command, technology, error output.
5. Click Analyse Failure.
6. Read the generated prompt:
   - Current phase
   - Likely root cause
   - Next diagnostic command (ONE command only)
7. Run that ONE diagnostic command yourself.
8. Paste the output back into Prompt Whisperer (error output field).
9. Click Analyse Failure again.
10. Repeat until the phase resolves.
```

> **Use Copilot Agent mode only if you want it to inspect files or propose targeted patches.**
> For diagnostic commands (version checks, path checks, dependency trees, logs), run them yourself and paste the output.

### Output format

Every generated troubleshooting prompt follows this structure:

```
Status:                  Active — evidence-based debugging in progress
Current phase:           TOOLCHAIN / DEPENDENCY / COMPILATION / TESTING / etc.
Technology:              gradle / npm / python / docker / terraform / kubectl / etc.
Observed symptom:        The failing command and its output
Likely root cause:       Deepest extracted cause from stack trace or error
Evidence:                Fixes attempted so far
What not to repeat:      Previously failed commands (with phase)
No-loop protection:      Whether a retry is allowed and why
User-run command mode:   Warning to not let Copilot Agent loop
Next diagnostic command: ONE targeted command to run next
Why this command:        Explanation of what this command confirms
What success looks like: What the output should show if fixed
What to paste back:      Instruction to copy the full output back
```

### Diagnostic phases

| Phase         | Examples                                                      |
|---------------|---------------------------------------------------------------|
| ENVIRONMENT   | Missing env vars, permission denied, OS/platform mismatches   |
| TOOLCHAIN     | Wrong Java/Node/Python version, missing JDK, wrong JAVA_HOME  |
| DEPENDENCY    | Unresolved imports, missing packages, artifact resolution     |
| CONFIGURATION | Build file errors, missing properties, invalid config         |
| COMPILATION   | Syntax errors, type errors, unresolved references             |
| TESTING       | Failing tests, assertion errors                               |
| RUNTIME       | App crashes at startup, NPEs, connection refused              |
| PACKAGING     | Docker build failures, JAR packaging errors                   |
| DEPLOYMENT    | kubectl, Terraform, Helm, cloud CLI failures                  |
| UNKNOWN       | Not yet classified — more information needed                  |

### Technology support

Troubleshooting Mode works across:
- **Java / Gradle** — JDK version checks, dependency resolution, compilation, test failures
- **Node / npm** — version checks, module resolution, TypeScript compilation, Jest/Mocha failures
- **Python / pip / pytest** — version checks, missing modules, pytest failures
- **Docker** — image build failures, Dockerfile errors, layer caching issues
- **Terraform** — plan/apply failures, variable mismatches, provider errors
- **Kubernetes / kubectl** — deployment failures, kubeconfig issues, pod describe
- **Cloud CLIs** — Azure CLI, AWS CLI, gcloud authentication and region errors
- **General** — any runtime error with a stack trace or error output

### Examples

#### Gradle — wrong JAVA_HOME

```
Failed command:  ./gradlew test
Error output:    Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain

Phase detected:  TOOLCHAIN
Likely cause:    JAVA_HOME points to a JRE or browser plugin, not a JDK.
Next command:    java -version && echo "JAVA_HOME=$JAVA_HOME"
Success looks like: java -version prints a JDK version (not JRE or browser plugin)
```

#### npm — missing module

```
Failed command:  npm start
Error output:    Error: Cannot find module 'express'

Phase detected:  DEPENDENCY
Likely cause:    express package is not installed.
Next command:    npm ls --depth=1
Success looks like: express appears in the dependency tree with a version number
```

#### Python / pytest — test failure

```
Failed command:  pytest
Error output:    AssertionError: assert False

Phase detected:  TESTING
Likely cause:    An assertion in a test is failing.
Next command:    pytest -v 2>&1 | tail -60
Success looks like: All tests pass with 0 failures
```

#### Docker — image build failure

```
Failed command:  docker build .
Error output:    Error building image: failed to build: exit code 1

Phase detected:  PACKAGING
Likely cause:    A layer in the Dockerfile is failing to build.
Next command:    docker build . --no-cache --progress=plain 2>&1 | tail -60
Success looks like: Build finishes with "Successfully built <image id>"
```

---

## Running tests

```bash
# Run all tests
./gradlew clean test --info

# Run only the Troubleshooting Mode tests
./gradlew test --tests com.promptwhisperer.TroubleshootingModeTests --info

# Run only the Security Filter tests
./gradlew test --tests com.promptwhisperer.SecurityFilterTests --info
```

---

## Project structure

```
src/main/kotlin/com/promptwhisperer/
  services/
    TroubleshootingService.kt        # State model, phase inference, no-loop protection, prompt generation
    SecurityFilterService.kt         # File security filtering
    ContextScannerService.kt         # Project context scanning (IntelliJ VFS)
    PromptGenerationService.kt       # Standard prompt generation
    ArtefactPersistenceService.kt    # Prompt artefact persistence
    HashingService.kt                # SHA-256 hashing utility
    SlugUtil.kt                      # Slug generation utility
  ui/
    PromptWhispererPanel.kt          # Main UI panel (Standard + Troubleshooting modes)
    PromptWhispererToolWindowFactory.kt

src/test/kotlin/com/promptwhisperer/
  TroubleshootingModeTests.kt        # Troubleshooting Mode tests
  SecurityFilterTests.kt             # Security filter tests
  SlugAndHashTests.kt                # Slug and hash utility tests
```

---

## Security

Prompt Whisperer never reads secrets, key files, `.env` files, or credential files.
The `SecurityFilterService` blocks all such files before they can be included in any prompt.
See `SecurityFilterService.kt` for the full list of blocked patterns.
