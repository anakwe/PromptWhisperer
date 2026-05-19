# Prompt Whisperer

Prompt Whisperer is an IntelliJ IDEA plugin for generating high-quality, structured prompts for AI coding assistants.

It is designed as a **local-first, security-conscious prompt design assistant** that helps you produce better implementation instructions for GitHub Copilot and similar tools.

## What Prompt Whisperer Does

- Converts rough task ideas into structured implementation prompts
- Supports multiple generation modes (implementation, architecture, debugging, security, testing)
- Supports profile-based prompt behavior (engineering cognition profiles)
- Supports depth levels for concise vs detailed prompt output
- Provides troubleshooting workflow inputs for evidence-based debugging prompts
- Lets you copy prompts directly to clipboard
- Saves prompt artefacts locally under `.prompt-whisperer/prompts/`

## Current Modes

1. **Standard Mode**
2. **Architecture Mode**
3. **Debugging Mode**
4. **Security Review Mode**
5. **Test Generation Mode**
6. **Troubleshooting Mode**

## Engineering Behaviour Profiles

Prompt generation supports nine engineering-focused profiles:

- Balanced Engineer
- Senior Architect
- Security Engineer
- DevOps / SRE
- Rapid Prototype
- Enterprise Consultant
- Troubleshooter
- Teaching Mode
- Minimalist

Profiles influence prompt wording, constraints, and emphasis.

## Prompt Depth Levels

- **Minimal**
- **Standard**
- **Detailed**
- **Enterprise**

Depth controls prompt verbosity and engineering detail.

## Local-First and Security-First Principles

Prompt Whisperer is built with strict safety constraints:

- No external API calls for prompt generation
- No telemetry or hidden tracking
- No automatic code modification
- No automatic Copilot submission
- No shell profile mutation (`~/.zshrc`, `~/.bashrc`, etc.)

## Installation

### Option A — Run in IntelliJ sandbox (recommended for development)

```bash
cd PromptWhisperer
./gradlew clean test
./gradlew runIde
```

### Option B — Build plugin and install from disk

```bash
cd PromptWhisperer
./gradlew buildPlugin
```

Then in IntelliJ:

`Settings` → `Plugins` → gear icon → `Install Plugin from Disk...`

Select the plugin ZIP/JAR from `build/distributions/`.

## Usage

1. Open **View > Tool Windows > Prompt Whisperer**
2. Choose mode, profile, and depth
3. Enter task description
4. Click **Analyse Request** (or **Analyse Failure** in Troubleshooting mode)
5. Click **Generate Prompt**
6. Review and edit generated markdown
7. Click **Copy Prompt** and paste into Copilot Chat
8. Optionally click **Save Artefact**

## Prompt Artefacts

Saved prompts are stored in:

- `.prompt-whisperer/prompts/`
- `.prompt-whisperer/index.json`

Filenames follow:

- `YYYYMMDD-HHMMSS-task-slug.prompt.md`

Index entries include SHA-256 hash for integrity.

## Build and Test

```bash
cd PromptWhisperer
./gradlew clean test buildPlugin
```

## Documentation

- `docs/QUICK_START.md`
- `docs/USER_GUIDE.md`
- `docs/ARCHITECTURE.md`
- `SECURITY.md`
- `CONTRIBUTING.md`

## Contributing

Please see `CONTRIBUTING.md`.

## Code of Conduct

Please see `CODE_OF_CONDUCT.md`.

## License

This project is licensed under the Apache License 2.0.
See `LICENSE`.
