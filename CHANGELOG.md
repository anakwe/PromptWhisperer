# Changelog

All notable changes to this project are documented here.

## [Unreleased]

## [0.2.0] - 2026-05-23

### Added
- Two-stage workflow: request analysis followed by clarification answers before final prompt generation.
- Dominant prompt workspace with metadata banner and generation state indicators.
- Collapsible prompt output pane to reveal the full clarification and guardrail configuration area on demand.
- Raw markdown + rendered markdown preview modes.
- Prompt copy and markdown export actions from output workspace.
- Dynamic clarification panel with inline answer controls.
- Guardrail UX panel grouped by Architecture/Security/Code Quality/Operational Safety.
- Reset-to-recommended-defaults guardrail action.
- New `PromptMode` model for mode-aware generation behavior.
- Composable prompt block architecture via `PromptBlock` implementations.
- Lightweight rule-based `InferenceEngine` for synthesis quality.
- Clarification-driven guidance synthesis that converts answers into implementation constraints and delivery guidance.
- Conflict detection section for mismatches between original request intent and clarification answers.
- New reasoning sections in generated prompts:
  - `Clarification-Driven Guidance`
  - `Conflict Detected`
  - `Implementation Considerations`
  - `Recommended Architecture`
  - `Engineering Trade-Offs`
  - `Suggested Delivery Priorities`
- Comprehensive security filtering tests for secret-like file exclusions and safe-file retention.
- Context scanner whitelist tests to validate small-file preview safety boundaries.
- `docs/SECURITY_MODEL.md` for explicit security architecture documentation.
- `docs/RELEASE_PROCESS.md` for versioning, packaging, checksum, and GitHub release steps.
- Example README screenshots under `docs/images/examples/flappy-facebook-game/`.
- OSS community templates:
  - bug report
  - feature request
  - behaviour profile suggestion
  - security concern report
  - pull request template

### Changed
- Repositioned product identity from template generator to implementation planning tool.
- README rewritten with philosophy, trust model, roadmap, polished screenshots, and release documentation links.
- `docs/QUICK_START.md`, `docs/ARCHITECTURE.md`, and `CONTRIBUTING.md` upgraded for production OSS quality.
- Prompt generation now integrates profile/depth/mode/clarification context more explicitly.
- Clarification answers are synthesized into engineering implications instead of being treated as plain key-value metadata.
- Behaviour profiles now produce stronger reasoning voice differences (e.g., Security Engineer threat-model-first, Rapid Prototype MVP-coach).
- GitHub community templates migrated to concise Markdown issue templates and a streamlined pull request template.

### Fixed
- Added strict domain-gated clarification generation so game-oriented questions are not emitted for non-game prompts.
- Added confidence-threshold handling for domain-specific clarification sets with neutral fallback questions when confidence is low.
- Fixed conflict detection so explicit negative auth requirements (for example, "Do not build authentication initially") override generic authentication inference.
- Fixed architecture recommendation precedence so explicit source stack choices override clarification answers like "No preference".
- Added explicit frontend/backend/storage extraction from source prompts when concrete technologies are already specified.
- Added regressions for context-isolation, auth-conflict suppression, and architecture merge precedence.

### Docs
- Updated release workflow examples to v0.2.0 in `docs/RELEASE_PROCESS.md`.
- Updated release metadata (`README.md` badge and `gradle.properties`) for v0.2.0.

### Security
- Strengthened visible trust boundaries in docs and workflow messaging (local-first, no hidden network/telemetry).
- Verified exclusion of secret-like files from prompt context across `.env*`, credential files, key material, and Terraform state artifacts.

