# Security Policy

## Security Principles

Prompt Whisperer is designed with a local-first, least-surprise security model:

- No external prompt generation API calls
- No telemetry or hidden tracking
- No hidden network access for prompt generation
- No automatic source-code modification
- No automatic Copilot submission

## Data Handling

- Prompt text is generated locally in the IntelliJ process.
- Optional artefacts are stored locally in `.prompt-whisperer/` inside the project.
- Prompt artefact index includes SHA-256 hash values for integrity checks.

## Sensitive File Safety

`SecurityFilterService` excludes secret-like files from context inclusion.

Examples include:
- `.env` and environment files
- private keys and certs
- secret config files
- credential-like paths/patterns

## Reporting a Vulnerability

Please report security issues privately to:

- `opensource@anakwe.org`

Do not open public issues for unpatched vulnerabilities.

Include:
- affected version
- reproduction steps
- expected vs actual behavior
- impact assessment

