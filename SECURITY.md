# Security Policy

This document describes Prompt Whisperer's security posture, reporting process, and trust model for public open-source use.

Prompt Whisperer is designed as a **local-first developer tool** for implementation planning with AI coding assistants.

## Supported Versions

| Version | Supported |
| --- | --- |
| `0.1.x` | Yes |
| `< 0.1.0` | No |

Security fixes are applied to the latest supported release line.

## Security Model Summary

Prompt Whisperer is built with explicit trust boundaries:

- **No telemetry**: the plugin does not collect usage analytics or hidden tracking data.
- **No hidden network access for prompt generation**: prompt synthesis and inference are local.
- **No automatic prompt submission**: prompts are only copied/exported/saved through explicit user action.
- **No automatic code modification**: the plugin does not silently edit project source files.

## Secret Exclusion Policy

Prompt Whisperer applies secret-like file filtering before context is included in prompt generation.

Examples of blocked patterns include:

- `.env`, `.env.*`
- `secrets.*`
- `credentials.*`
- key/cert material (`*.pem`, `*.key`, `*.p12`, `*.jks`)
- SSH private key names (`id_rsa`, `id_ed25519`)
- Terraform state/secret-like files (`*.tfvars`, `terraform.tfstate*`)

This filtering reduces accidental inclusion of sensitive files in generated prompts.

## Important User Warning

Even with file filtering enabled:

- **Do not type secrets into task descriptions or clarification answers.**
- **Do not paste tokens, passwords, API keys, or private credentials into the UI.**

Generated prompt text may include user-provided content. If sensitive text is entered manually, it may appear in:

- prompt preview
- copied prompt text
- exported markdown
- saved artefacts under `.prompt-whisperer/`

Users are responsible for what they enter into the prompt workflow.

## Vulnerability Reporting

Please report security issues privately to:

- `opensource@anakwe.org`

Include where possible:

- affected version
- reproduction steps
- expected vs actual behavior
- security impact
- suggested mitigation (optional)

## Responsible Disclosure Expectations

To protect users and maintainers, please:

- report vulnerabilities privately first
- avoid posting exploit details publicly before a fix is available
- allow reasonable time for triage and remediation
- coordinate publication timing for high-impact issues

We will acknowledge reports, assess severity, and provide update status as remediation progresses.

## Dependency Update Policy

Prompt Whisperer maintainers periodically review and update dependencies.

General approach:

- prioritize known vulnerability fixes
- keep build and plugin compatibility stable
- avoid unnecessary dependency growth
- release fixes in supported version lines where feasible

## Verifying Local-Only Behavior (High-Level)

Developers can independently validate behavior at a high level by:

1. Running the plugin in sandbox mode (`./gradlew runIde`).
2. Generating prompts while monitoring network activity with local tooling.
3. Reviewing source for prompt generation and persistence paths.
4. Confirming prompt output changes only through explicit UI actions.

This is not a formal audit process, but it supports practical trust verification.

## Scope of Guarantees and Limitations

### What this policy covers

- plugin behavior implemented in this repository
- local prompt generation flow
- documented exclusion/filtering behavior

### What this policy does not guarantee

- protection against a compromised host machine or malicious third-party software
- prevention of user-introduced sensitive text in prompt inputs
- zero-risk operation in all environments or integrations

Prompt Whisperer provides guardrails and local-first defaults, but secure usage still depends on user and environment practices.
