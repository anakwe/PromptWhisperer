# Prompt Whisperer Quick Start

Use this guide to run Prompt Whisperer locally in under 5 minutes.

## Prerequisites

- macOS, Linux, or Windows
- JDK 21+
- IntelliJ IDEA
- Git

## 1) Clone and run in IntelliJ sandbox

```bash
git clone https://github.com/anakwe/PromptWhisperer.git
cd PromptWhisperer
./gradlew clean test
./gradlew runIde
```

This starts a sandbox IntelliJ with Prompt Whisperer pre-installed.

## 2) Open Prompt Whisperer

In sandbox IntelliJ:

- `View` → `Tool Windows` → `Prompt Whisperer`

## 3) Generate a prompt

1. Select mode, profile, and depth.
2. Enter your task description.
3. Click **Analyse Request**.
4. Click **Generate Prompt**.
5. Edit prompt if needed.
6. Click **Copy Prompt**.

Paste the prompt into GitHub Copilot Chat.

## 4) Save a prompt artefact (optional)

Click **Save Artefact** to persist generated prompt files under:

- `.prompt-whisperer/prompts/`
- `.prompt-whisperer/index.json`

## 5) Install into your own IntelliJ (optional)

```bash
./gradlew buildPlugin
```

Then install from disk in IntelliJ:

- `Settings` → `Plugins` → gear icon → `Install Plugin from Disk...`

## Troubleshooting startup

If sandbox does not open:

```bash
./gradlew --stop
./gradlew clean runIde --stacktrace
```

If tests fail:

```bash
./gradlew clean test --info
```

See `docs/USER_GUIDE.md` for full feature details.
