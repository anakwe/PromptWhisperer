# Prompt Whisperer Quick Start Guide

## What is Prompt Whisperer?

Prompt Whisperer is an IntelliJ plugin that turns your rough coding ideas into structured prompts for GitHub Copilot.

Instead of asking Copilot vague questions, you describe what you want, choose a prompt mode, and get a detailed, actionable prompt you can copy into Copilot Chat.

## Install the plugin

### Option 1: Run locally (for development/testing)

```bash
cd prompt-whisperer
./gradlew clean test
./gradlew runIde
```

This launches IntelliJ with the plugin pre-installed in a sandbox. Great for testing.

### Option 2: Install into your own IntelliJ

1. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```

2. In IntelliJ, go to **Settings > Plugins > ⚙️ (gear icon) > Install Plugin from Disk**.

3. Select `build/distributions/prompt-whisperer-0.1.0.jar` and restart IntelliJ.

## Open the plugin

1. **View > Tool Windows > Prompt Whisperer**  
   Or press `Shift` twice and search for "Prompt Whisperer".

2. The plugin panel opens on the right side of the editor.

## Basic workflow: Generate a prompt

### Step 1: Describe what you want

In the **"Describe what you want Copilot to build"** text area, type your request:

```
Build a simple demo website with an embedded video, responsive layout, 
and comprehensive README instructions including installation steps.
```

### Step 2: Choose a mode

Pick from the dropdown:

| Mode | Use When |
|------|----------|
| **Standard Mode** | You want Copilot to implement a feature or make a change. |
| **Architecture Mode** | The change affects multiple files/modules; you want design discussion first. |
| **Debugging Mode** | A build/test failed; you want diagnosis before trying fixes. |
| **Security Review Mode** | You want Copilot to audit code for secrets, unsafe patterns, or vulnerabilities. |
| **Test Generation Mode** | You want Copilot to add tests matching your project's style. |
| **Troubleshooting Mode** | A command failed; you want step-by-step evidence-based debugging. |

### Step 3: Generate the prompt

Click **▶ Generate Prompt**.

The complete prompt appears in the large preview area below. It includes:
- A clear restatement of your goal
- Best practices and constraints
- Step-by-step guidance
- Acceptance criteria
- Expected output format

### Step 4: Copy to Copilot

Click **⎘ Copy Prompt** — it's copied to your clipboard.

Switch to Copilot Chat and paste it in:

```
[Paste the full generated prompt here]
```

Copilot will follow the structure and give you a higher-quality response.

### Step 5: (Optional) Save the prompt

Click **💾 Save Artefact** to save the generated prompt locally under `.prompt-whisperer/prompts/`.

The file is named with a timestamp and slug: `20260519-142530-build-demo-website.prompt.md`.

Use saved prompts as reference, examples, or for documentation.

## Troubleshooting Mode: Evidence-based debugging

Use this when a command fails and you want Copilot to help diagnose it systematically.

### What you need

- The exact command that failed (e.g., `./gradlew test`, `npm install`, `docker build .`)
- The exact error output or stack trace (paste the full error)
- The technology/tool (gradle, npm, docker, terraform, kubernetes, python, etc.)
- Any material changes since the last failure (optional)

### Workflow

1. In IntelliJ, select **Troubleshooting Mode** from the dropdown.

2. Fill in the fields:
   - **Failed command:** e.g., `./gradlew clean test`
   - **Error output:** Paste the full error message or stack trace.
   - **Technology:** e.g., `gradle`, `npm`, `docker`
   - **Material change (optional):** What changed since the last failure? (e.g., "Added JUnit dependency", "Updated Java to 21", "Cleared cache")

3. Click **🔍 Analyse Failure**.

4. The plugin generates a troubleshooting prompt that includes:
   - **Current phase:** What part of the build/deploy is failing (TOOLCHAIN, DEPENDENCY, COMPILATION, TESTING, RUNTIME, etc.)
   - **Likely root cause:** The deepest meaningful cause (not just the top-level error)
   - **Evidence:** What you've already tried
   - **Next diagnostic command:** ONE specific command to run next
   - **Why this command:** What it will tell you
   - **What success looks like:** What the output should show if fixed

5. Copy the prompt to Copilot.

6. After running the next diagnostic command, paste the output back and click **🔍 Analyse Failure** again.

7. Repeat until the issue is resolved.

### Key principles

- **One command at a time:** Don't run 10 commands. Run one, get a result, then decide the next step.
- **Material change requirement:** If a command failed before and nothing changed, Troubleshooting Mode blocks you from retrying it without a good reason.
- **Evidence first:** Diagnose before guessing and changing code.
- **No shell mutation:** Session-only environment changes first; only permanent shell changes with explicit user approval.

## All six modes explained

### Standard Mode

**Purpose:** Build a feature or make a change.

**Generated prompt includes:**
- Clear goal restatement
- Best practices (simplicity, maintainability, follow existing conventions)
- Step-by-step implementation plan
- Acceptance criteria (feature works, code compiles, tests pass)
- Constraints (don't touch unrelated files, no telemetry, no secrets)
- Expected output format

**Example use case:**
```
Add a new "Settings" menu to the main dashboard that lets users 
configure notification frequency and theme preference.
```

---

### Architecture Mode

**Purpose:** Plan the design BEFORE writing code (for larger changes).

**Generated prompt includes:**
- Explicit instruction to analyze the project structure first
- Request to propose design with component names and responsibilities
- Trade-off analysis (why this design over alternatives)
- Risk identification
- Scope estimation (small / medium / large)
- Wait-for-confirmation before implementing

**Example use case:**
```
Refactor the authentication system to support OAuth 2.0 while 
maintaining backward compatibility with existing API key auth.
```

---

### Debugging Mode

**Purpose:** Evidence-based debugging with no wild guesses.

**Generated prompt includes:**
- Clear instruction to diagnose before changing code
- Root cause analysis format (observed symptom, likely cause, evidence, proposed fix)
- Cite evidence (specific file, line, stack trace)
- Smallest safe, targeted fix only
- Verification command requirement
- Strict constraint: no broad dependency changes, no version upgrades unless proven necessary

**Example use case:**
```
./gradlew test is failing with ClassNotFoundException: org.junit.rules.TestRule
```

---

### Security Review Mode

**Purpose:** Audit code for security issues.

**Generated prompt includes:**
- Comprehensive checklist (hardcoded secrets, unsafe file reads, unvalidated input, 
  unnecessary network calls, permission risks, sensitive logging, prompt injection risks, 
  vulnerable dependencies, auth/authz issues, insecure defaults)
- Output format: for each finding, include risk, severity, location, recommended fix
- Summary of items checked and confirmed safe
- Constraint: report all findings first, do not auto-apply fixes

**Example use case:**
```
Review the new API endpoint for security risks before merging.
```

---

### Test Generation Mode

**Purpose:** Add tests matching your project's style and frameworks.

**Generated prompt includes:**
- Instruction to inspect existing tests first (framework, patterns, naming)
- Request for three test categories: happy path, edge cases, regression cases
- Constraint: match existing test style, deterministic tests, no new test frameworks, 
  no brittle timing-dependent tests
- Requirement to provide run commands (new tests only + full suite)

**Example use case:**
```
Add comprehensive tests to PromptWhispererPanel to cover all modes 
and edge cases, matching the existing test style in the project.
```

---

### Troubleshooting Mode

**Purpose:** Step-by-step debugging with protection against infinite loops.

**Core principle:** Fix the current problem without mutating your machine.

See **Troubleshooting Mode: Evidence-based debugging** above for full details.

---

## Reset your session

Click **↺ Reset** to clear:
- Task description
- Generated prompt
- Selected mode
- Activity log

This does NOT save or delete any files — it's just clearing the current working session.

---

## Save generated prompts

Click **💾 Save Artefact** to save the current prompt under:

```
.prompt-whisperer/prompts/YYYYMMDD-HHMMSS-task-slug.prompt.md
```

Each saved prompt includes:
- The full generated prompt
- Metadata (timestamp, task slug, SHA-256 hash of content)
- Entry in `.prompt-whisperer/index.json` for tracking

Use saved prompts as:
- Reference material
- Examples for future tasks
- Documentation of what was requested
- Audit trail of changes and decisions

---

## Activity log

The **Activity Log** at the bottom of the panel shows:
- When you generated a prompt
- Copy/save actions
- Errors or warnings
- Troubleshooting phase transitions

---

## FAQs

### Q: Can Prompt Whisperer modify my code automatically?
**A:** No. Prompt Whisperer only generates prompts. **You** are in control. You copy the prompt into Copilot Chat and decide what to do with Copilot's suggestions.

### Q: Does it collect telemetry or make network calls?
**A:** No. Prompt Whisperer is fully local. All generation happens in your IntelliJ process. No external APIs, no tracking, no telemetry.

### Q: Does it read my secrets?
**A:** No. The security filter blocks all files that might contain secrets (`.env`, `*.key`, `config/secrets.yml`, etc.) from being included in any prompt.

### Q: What if I generate a prompt, close IntelliJ, and come back the next day?
**A:** The task description and generated prompt are lost (they're session state). That's why the **💾 Save Artefact** button is useful — it saves the prompt to `.prompt-whisperer/prompts/` for future reference.

### Q: Can I edit the generated prompt?
**A:** Yes! The preview area is editable. You can adjust the generated prompt before copying it to Copilot.

### Q: What's the difference between Debugging Mode and Troubleshooting Mode?
**A:**
- **Debugging Mode:** Use when you want to FIX a failure. Copilot proposes a targeted fix and verification steps.
- **Troubleshooting Mode:** Use when you want to DIAGNOSE a failure step-by-step without immediate fixes. It's more hands-on and evidence-driven.

---

## Next steps

1. Open the plugin: **View > Tool Windows > Prompt Whisperer**
2. Try **Standard Mode:** describe a simple feature and generate a prompt.
3. Copy it into Copilot Chat and see the difference a structured prompt makes.
4. Experiment with other modes as you encounter different scenarios.

---

## Documentation

- **README.md** — Full documentation of all modes and safety principles (especially Troubleshooting Mode)
- **SECURITY.md** — Security practices and threat model
- **This file (QUICK_START.md)** — Getting started guide

---

**Good luck! Prompt Whisperer makes Copilot more useful. Happy prompting.**

