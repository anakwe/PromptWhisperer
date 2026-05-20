# Prompt Whisperer

Prompt Whisperer is a **local-first engineering implementation planning tool** for AI coding assistants.

It acts like a senior engineering consultant between the developer and AI coding tools: reducing ambiguity, surfacing architecture and security concerns, and turning rough requests into execution-ready implementation prompts.

## Why This Exists

Many AI coding failures are not model failures. They are specification failures.

Prompt Whisperer helps teams move from:
- vague request -> brittle output

to:
- clarified request -> scoped plan -> guardrailed implementation prompt

## Project Philosophy

Core principles:
- Think before prompting
- Architecture before implementation
- Security-aware AI-assisted coding
- Explicit engineering guardrails
- Human-in-control workflows
- Local-first trust model
- No hidden telemetry
- No hidden network activity

## Product Positioning

Prompt Whisperer is **not** a prompt template toy.

Prompt Whisperer **is**:
- an implementation planning workbench
- an ambiguity-reduction assistant
- a profile-driven engineering guidance layer
- a guardrailed bridge to AI coding assistants

## Main Workflow

1. Describe the engineering goal
2. Analyse request
3. Answer visible clarification questions in the dedicated Clarification Questions section
4. Generate prompt with profile + depth + guardrails + clarification answers
5. Review/edit in raw markdown or preview mode
6. Copy/export/save prompt artefact

Prompt Whisperer synthesizes clarification answers into engineering reasoning (not simple answer repetition).

### Clarification Example

Initial request:

`Build a small Flappy Bird style web game.`

Clarification answer:

`Use plain HTML, CSS and JavaScript. Include keyboard and mobile touch controls.`

Generated prompt behavior:
- keeps implementation browser-native
- avoids unnecessary framework introduction
- includes explicit control/input requirements
- includes trade-offs and delivery priorities

## Screenshots

### 1) Main Workflow
![Main Workflow](docs/images/main-workflow.png)

### 2) Behaviour Profiles
![Behaviour Profiles](docs/images/behaviour-profiles.png)

### 3) Guardrails
![Guardrails](docs/images/guardrails.png)

### 4) Generated Prompt Preview
![Generated Prompt Preview](docs/images/generated-prompt-preview.png)

### 5) Clarification Workflow
![Clarification Workflow](docs/images/clarification-workflow.png)

## Feature Breakdown

### Dominant Prompt Workspace
- Prompt output intentionally occupies most of the UI
- Editable markdown output
- Raw markdown and rendered preview modes
- Prompt metadata banner (profile, depth, guardrails, word count)
- One-click copy and markdown export

### Behaviour Profiles (9)
- Balanced Engineer
- Senior Architect
- Security Engineer
- DevOps / SRE
- Rapid Prototype
- Enterprise Consultant
- Troubleshooter
- Teaching Mode
- Minimalist

Each profile changes prompt wording, constraints, clarification emphasis, and implementation philosophy.

### Guardrails
Grouped and configurable under:
- Architecture
- Security
- Code Quality
- Operational Safety

Guardrails are visible, explainable, and resettable to profile-recommended defaults.

### Prompt Depth
- Minimal: Short implementation-focused prompts.
- Standard: Balanced engineering detail and guidance.
- Detailed: Senior-engineer-level implementation planning.
- Enterprise: Architecture, governance, operational, and documentation heavy prompts.

### Clarification Workflow
Before final prompt generation, Prompt Whisperer asks dynamic context-aware questions (framework, responsiveness, testing, deployment, etc.) to materially improve prompt quality.

### Inference-Driven Prompt Synthesis
A local rule-based inference layer converts user intent into implementation planning guidance.

Generated prompts now include reasoning sections:
- `## Implementation Considerations`
- `## Recommended Architecture`
- `## Planning Balance`
- `## Engineering Trade-Offs`
- `## Suggested Delivery Priorities`

## Local-First Trust Model

Prompt Whisperer is designed to be explicit and trustworthy:
- prompt generation is local
- no hidden telemetry
- no hidden network activity for generation
- no automatic prompt submission
- no automatic code modifications

See also:
- `SECURITY.md`
- `docs/SECURITY_MODEL.md`

## Installation

### Run in IntelliJ sandbox
```bash
git clone https://github.com/anakwe/PromptWhisperer.git
cd PromptWhisperer
./gradlew clean test
./gradlew runIde
```

### Build plugin package
```bash
./gradlew clean buildPlugin
```

Install from disk in IntelliJ:
- `Settings` -> `Plugins` -> gear icon -> `Install Plugin from Disk...`
- Select package from `build/distributions/`

## Development Setup

```bash
./gradlew clean test
./gradlew runIde
```

## Formatting and Build

```bash
./gradlew ktlintFormat
./gradlew build
```

## Plugin Packaging

```bash
./gradlew clean buildPlugin
```

Output:
- `build/distributions/prompt-whisperer-<version>.zip`

## Documentation

- `docs/QUICK_START.md`
- `docs/USER_GUIDE.md`
- `docs/ARCHITECTURE.md`
- `docs/SECURITY_MODEL.md`
- `SECURITY.md`
- `CONTRIBUTING.md`
- `CHANGELOG.md`

## Roadmap

Planned enhancements:
- richer clarification engine
- reusable prompt presets
- organizational engineering standards packs
- prompt comparison view
- prompt history search
- VS Code support
- export/import prompt configurations
- optional local LLM integration
- prompt refinement workflow

## Contributing

Contributions are welcome. Please start with `CONTRIBUTING.md`.

## License

Apache License 2.0. See `LICENSE`.
