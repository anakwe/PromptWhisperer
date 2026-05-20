# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Added
- Two-stage workflow: request analysis followed by clarification answers before final prompt generation.
- Dominant prompt workspace with metadata banner and generation state indicators.
- Raw markdown + rendered markdown preview modes.
- Prompt copy and markdown export actions from output workspace.
- Dynamic clarification panel with inline answer controls.
- Guardrail UX panel grouped by Architecture/Security/Code Quality/Operational Safety.
- Reset-to-recommended-defaults guardrail action.
- New `PromptMode` model for mode-aware generation behavior.
- Composable prompt block architecture via `PromptBlock` implementations.
- Lightweight rule-based `InferenceEngine` for synthesis quality.
- New reasoning sections in generated prompts:
  - `Implementation Considerations`
  - `Recommended Architecture`
  - `Planning Balance`
  - `Engineering Trade-Offs`
  - `Suggested Delivery Priorities`
- `docs/SECURITY_MODEL.md` for explicit security architecture documentation.
- OSS community templates:
  - bug report
  - feature request
  - behaviour profile suggestion
  - security issue report
  - pull request template

### Changed
- Repositioned product identity from template generator to implementation planning tool.
- README rewritten with philosophy, trust model, roadmap, and screenshot placeholders.
- `docs/QUICK_START.md`, `docs/ARCHITECTURE.md`, and `CONTRIBUTING.md` upgraded for production OSS quality.
- Prompt generation now integrates profile/depth/mode/clarification context more explicitly.
- Clarification answers are synthesized into engineering implications instead of being treated as plain key-value metadata.
- Behaviour profiles now produce stronger reasoning voice differences (e.g., Security Engineer threat-model-first, Rapid Prototype MVP-coach).

### Security
- Strengthened visible trust boundaries in docs and workflow messaging (local-first, no hidden network/telemetry).

