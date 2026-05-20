# Prompt Whisperer Security Model

## Security Objectives

Prompt Whisperer is designed for trust-preserving AI-assisted engineering workflows.

Objectives:
- keep prompt generation local
- keep user intent explicit
- avoid hidden outbound behavior
- prevent accidental exposure of sensitive project context

## Threat Model (Practical)

### In scope
- accidental prompt leakage via hidden network calls
- hidden telemetry/tracking collection
- unsafe automatic code or shell modifications
- unintentional inclusion of sensitive files in prompt context

### Out of scope
- compromised host machine
- malicious third-party plugins outside this project
- user-initiated intentional data exfiltration

## Security Guarantees

- No hidden telemetry.
- No hidden network activity for prompt generation.
- No automatic prompt submission.
- No automatic code modification.
- Prompt artefact persistence is explicit user action.

## Data Flow and Storage

- Input request stays in IDE process.
- Clarification answers stay in memory for the active session.
- Generated prompt is visible/editable before copy/export.
- Optional artefacts are stored locally under `.prompt-whisperer/`.

## Sensitive Context Handling

`SecurityFilterService` excludes secret-like files and patterns from context expansion.

Examples:
- `.env` files
- key/cert material
- credential-like filenames

## Human-in-Control Boundaries

Prompt Whisperer does not take action beyond user intent:
- user must click Analyse, Generate, Save, Copy, or Export
- no background submission pipeline
- no autonomous code writing into project files

## Security Reporting

Report vulnerabilities privately to:
- `opensource@anakwe.org`

Please include:
- affected version
- reproduction details
- impact assessment
- remediation suggestion (if known)

