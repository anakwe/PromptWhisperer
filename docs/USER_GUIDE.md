# Prompt Whisperer User Guide

## Overview

Prompt Whisperer is a local-first implementation planning tool for AI coding assistants.

It helps you move from a rough request to a high-quality, context-aware engineering prompt using:
- behaviour profiles
- prompt depth
- clarification questions
- explicit guardrails

## Core Workflow (Two-Stage)

1. Select mode, behaviour profile, and depth.
2. Describe your request.
3. Click `Analyse Request`.
4. Answer clarification questions inline.
5. Review guardrails (or reset to recommended defaults).
6. Click `Generate Prompt`.
7. Review/edit prompt in raw markdown or preview mode.
8. Copy, export, or save artefact.

## Modes

### Standard Mode
Balanced implementation planning for most engineering tasks.

### Architecture Mode
Emphasizes boundaries, component responsibilities, and trade-offs.

### Debugging Mode
Prioritizes diagnosis-first guidance and verification checkpoints.

### Security Review Mode
Focuses on secure defaults, trust boundaries, and risk-aware implementation.

### Test Generation Mode
Increases test planning detail and scenario coverage expectations.

### Troubleshooting Mode
Generates evidence-driven troubleshooting prompts from failed command data.

## Behaviour Profiles

- **Balanced Engineer**: sensible defaults, maintainability-first
- **Senior Architect**: architecture-first, extensibility-focused
- **Security Engineer**: least privilege, validation, secure defaults
- **DevOps / SRE**: operability, rollback, reliability
- **Rapid Prototype**: speed-first with scoped risk
- **Enterprise Consultant**: governance and documentation-heavy
- **Troubleshooter**: root-cause and safe incremental fixes
- **Teaching Mode**: explanation-rich with trade-offs
- **Minimalist**: smallest safe implementation

Each profile changes prompt structure, tone, constraints, and clarification emphasis.

## Prompt Depth

- **Minimal**: short implementation-focused prompts.
- **Standard**: balanced engineering detail and guidance.
- **Detailed**: senior-engineer-level implementation planning.
- **Enterprise**: architecture, governance, operational, documentation heavy prompts.

## Guardrails

Guardrails are grouped under:
- Architecture
- Security
- Code Quality
- Operational Safety

Each guardrail has a tooltip description and can be toggled on/off.

Use `Reset to Recommended Defaults` to re-apply profile defaults.

## Prompt Workspace

The lower panel is the main product surface:
- metadata bar (profile, depth, guardrails enabled, prompt length)
- editable raw markdown mode
- markdown preview mode
- copy and markdown export actions

Generated prompts now include synthesis reasoning sections such as:
- `## Implementation Considerations`
- `## Recommended Architecture`
- `## Planning Balance`
- `## Engineering Trade-Offs`
- `## Suggested Delivery Priorities`

Empty state message appears until first generation:
- `No prompt generated yet. Describe a task and click Analyse Request.`

## Prompt Artefacts

`Save Artefact` writes:
- `.prompt-whisperer/prompts/<timestamp>-<slug>.prompt.md`
- `.prompt-whisperer/index.json`

Index entries include SHA-256 hash for integrity tracking.

## Security and Privacy

Prompt Whisperer is local-first:
- no hidden telemetry
- no hidden network prompt submission
- no automatic code changes
- no automatic prompt submission

See:
- `SECURITY.md`
- `docs/SECURITY_MODEL.md`

## Tips for Better Prompt Quality

- Use specific outcomes, not generic goals.
- Answer clarification questions with concrete constraints.
- Prefer `Detailed` depth for non-trivial features.
- Use `Security Engineer` profile for sensitive flows.
- Keep guardrails explicit for team consistency.
