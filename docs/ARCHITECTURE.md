# Prompt Whisperer Architecture

## Architectural Intent

Prompt Whisperer is designed as a local-first implementation planning layer between developers and AI coding assistants.

Primary goals:
- reduce ambiguity before generation
- preserve engineering quality through explicit controls
- keep trust boundaries visible (no hidden network/telemetry)

## Layered Design

### UI Layer

Main classes:
- `com.promptwhisperer.ui.PromptWhispererToolWindowFactory`
- `com.promptwhisperer.ui.PromptWhispererPanelV3`
- `com.promptwhisperer.ui.components.PromptOutputPanel`
- `com.promptwhisperer.ui.components.ClarificationAnswersPanel`
- `com.promptwhisperer.ui.components.GuardrailSelectionPanel`

Responsibilities:
- collect request + mode/profile/depth
- drive two-stage workflow (`Analyse` -> `Generate Final Prompt`)
- present dominant prompt workspace (raw/preview)
- display prompt metadata and activity state
- manage guardrail UX and defaults

### Service Layer

Core services:
- `PromptBuilderImpl`
- `InferenceEngine`
- `ClarificationServiceImpl`
- `BehaviourProfileServiceImpl`
- `TroubleshootingServiceImpl`
- `ArtefactPersistenceServiceImpl`

#### Composable prompt block architecture

`PromptBuilderImpl` composes reusable `PromptBlock` implementations:

- `CorePromptBlock`
- `BehaviourProfileBlock`
- `PromptDepthBlock`
- `ClarificationAnswersBlock`
- `PlanningBalanceBlock`
- `ImplementationConsiderationsBlock`
- `RecommendedArchitectureBlock`
- `EngineeringTradeOffsBlock`
- `SuggestedDeliveryPrioritiesBlock`
- `ArchitectureBlock`
- `SecurityBlock`
- `TestingBlock`
- `DocumentationBlock`
- `OperationalBlock`
- `ModeSpecificBlock`
- `ConstraintsBlock`
- `OutputExpectationsBlock`

This block-based design avoids giant template strings and supports extension without rewriting the builder.

`InferenceEngine` provides lightweight, local heuristics for synthesis quality:
- conflict detection (profile vs depth tension)
- architecture/operational/security inference
- trade-off analysis
- delivery priority recommendations
- profile-specific reasoning voice

### Model Layer

- `BehaviourProfile`, `PromptDepth`, `Guardrail`, `GuardrailCategory`
- `PromptMode`
- `ClarificationQuestion`, `RequestAnalysis`, `PromptSessionConfig`
- Troubleshooting models (`FailedCommand`, `TroubleshootingState`, etc.)

## End-to-End Flow

1. User chooses mode, profile, depth.
2. User enters request and clicks `Analyse Request`.
3. Clarification service generates context-aware questions.
4. User answers clarifications and adjusts guardrails.
5. UI assembles `PromptSessionConfig`.
6. `PromptBuilderImpl` runs composable blocks.
7. Prompt is shown in raw markdown + preview with metadata.
8. User copies, exports, or saves artefact.

## Security and Trust Boundaries

- Prompt generation stays inside IDE process.
- No hidden telemetry.
- No hidden prompt submission.
- No automatic code modification.
- Prompt artefacts are explicit user action.

See:
- `SECURITY.md`
- `docs/SECURITY_MODEL.md`

## Extensibility

Current extension points:
- new `PromptMode` values + mode-specific block behavior
- profile-aware clarification expansion
- additional guardrail categories
- richer markdown rendering pipeline
- optional local LLM adapters (explicit opt-in)
