# Prompt Whisperer Quick Start

This guide gets you from clone to first high-quality implementation prompt quickly.

## Prerequisites

- JDK 21+
- IntelliJ IDEA
- Git

## 1) Clone and launch sandbox

```bash
git clone https://github.com/anakwe/PromptWhisperer.git
cd PromptWhisperer
./gradlew clean test
./gradlew runIde
```

## 2) Open the tool window

In sandbox IntelliJ:
- `View` -> `Tool Windows` -> `Prompt Whisperer`

## 3) Generate a final prompt (recommended flow)

1. Select mode, behaviour profile, and depth.
2. Enter your implementation request.
3. Click `Analyse Request`.
4. Answer follow-up questions in the `Clarification Questions` section.
5. Optionally adjust guardrails.
6. Click `Generate Prompt`.

If you skip analysis, generation still works and the tool logs that it used the initial request only.

The generated prompt now synthesizes your answers into implementation reasoning, architecture recommendations, trade-offs, and delivery priorities.

## 4) Review output

The bottom workspace is the main product surface:
- metadata bar (profile/depth/guardrails/word count)
- raw markdown mode (editable)
- markdown preview mode
- copy and export actions
- `Hide Prompt` / `Show Prompt` toggle when you need more space to review clarifications or guardrails

Generated prompts may include sections such as:
- `Clarification Answers`
- `Clarification-Driven Guidance`
- `Conflict Detected`
- `Implementation Considerations`
- `Recommended Architecture`
- `Engineering Trade-Offs`
- `Suggested Delivery Priorities`

## 5) Save local artefact (optional)

Use `Save Artefact` to persist prompt output under:
- `.prompt-whisperer/prompts/`
- `.prompt-whisperer/index.json`

## 6) Build installable plugin package

```bash
./gradlew clean buildPlugin
```

Install from IntelliJ:
- `Settings` -> `Plugins` -> gear icon -> `Install Plugin from Disk...`
- choose zip from `build/distributions/`

## Troubleshooting startup

```bash
./gradlew --stop
./gradlew clean runIde --stacktrace
```

See `docs/USER_GUIDE.md` for full usage details.
