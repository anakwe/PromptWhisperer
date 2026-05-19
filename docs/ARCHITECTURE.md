# Prompt Whisperer Architecture

## High-Level Design

Prompt Whisperer is an IntelliJ plugin with a local-only prompt generation pipeline.

### UI Layer

- `com.promptwhisperer.ui.PromptWhispererToolWindowFactory`
- `com.promptwhisperer.ui.PromptWhispererPanelV3`

Responsibilities:
- collect user inputs (mode, profile, depth, task)
- orchestrate actions (analyse, generate, copy, save, reset)
- present generated markdown prompt
- show activity log

### Service Layer

- `PromptBuilderImpl` — composable prompt assembly
- `BehaviourProfileServiceImpl` — profile defaults and guardrail behavior
- `ClarificationServiceImpl` — request analysis and clarification generation
- `TroubleshootingServiceImpl` — evidence-based troubleshooting state logic
- `ArtefactPersistenceServiceImpl` — local artefact persistence and index updates
- `HashingServiceImpl` and `SlugUtil` — utilities

### Model Layer

- `BehaviourProfile`, `PromptDepth`, `Guardrail`
- `ClarificationQuestion`, `RequestAnalysis`, `PromptSessionConfig`
- troubleshooting models (`TroubleshootingState`, `FailedCommand`, etc.)

## Prompt Generation Flow

1. User enters task details.
2. UI calls clarification analysis.
3. Session config resolves profile + depth + enabled guardrails.
4. UI calls `PromptBuilderImpl`.
5. Builder composes markdown blocks into final prompt.
6. Prompt is shown, copied, and optionally saved.

## Artefact Persistence Flow

1. User clicks **Save Artefact**.
2. `ArtefactPersistenceServiceImpl` writes markdown file under `.prompt-whisperer/prompts/`.
3. Service computes SHA-256 hash.
4. Service appends entry to `.prompt-whisperer/index.json`.

## Security Posture

- No external prompt-generation service calls.
- No automatic source code modification.
- Secret-like files blocked from prompt context collection by `SecurityFilterService`.
- Troubleshooting mode enforces no-loop logic and environment safety guidance.

## Extension Points

Recommended future additions:

- Markdown preview renderer (raw/preview toggle)
- Guardrail toggle UI component
- Clarification answers UI form and final merge into generated prompt
- Additional mode-specific prompt blocks

