# Prompt Whisperer User Guide

## Overview

Prompt Whisperer helps you produce better prompts for AI coding assistants by structuring your request with engineering constraints, profile behavior, and depth settings.

## Core Workflow

1. Choose a mode.
2. Choose a behavior profile.
3. Choose prompt depth.
4. Enter task description.
5. Analyse request.
6. Generate prompt.
7. Copy to clipboard.
8. (Optional) Save prompt artefact.

## Modes

### Standard Mode
General implementation prompts for day-to-day engineering tasks.

### Architecture Mode
Emphasizes design-first planning, module boundaries, and trade-offs.

### Debugging Mode
Focuses on root-cause-first diagnosis and verification steps.

### Security Review Mode
Adds explicit security checks and constraints.

### Test Generation Mode
Optimizes prompt guidance for test creation and coverage.

### Troubleshooting Mode
Captures failing command context and generates evidence-based debugging prompts.

## Behaviour Profiles

- **Balanced Engineer**: practical defaults and maintainable outcomes
- **Senior Architect**: architecture-first decisions and modularity
- **Security Engineer**: defensive posture and least-privilege mindset
- **DevOps / SRE**: operational safety and rollback awareness
- **Rapid Prototype**: speed-first, minimal ceremony
- **Enterprise Consultant**: governance-heavy and documentation-rich
- **Troubleshooter**: diagnostic rigor and incremental fixes
- **Teaching Mode**: explanation-heavy prompts with rationale
- **Minimalist**: smallest safe implementation

## Prompt Depth

- **Minimal**: concise and direct
- **Standard**: balanced detail
- **Detailed**: expanded guidance and constraints
- **Enterprise**: comprehensive architecture/ops/governance detail

## Prompt Artefacts

When using **Save Artefact**, Prompt Whisperer writes:

- `.prompt-whisperer/prompts/<timestamp>-<slug>.prompt.md`
- `.prompt-whisperer/index.json`

Each entry includes a SHA-256 hash for integrity tracking.

## Security and Privacy

Prompt Whisperer is local-first:

- no external prompt generation API calls
- no telemetry
- no hidden network submission
- no automatic code changes

## Tips for Better Output

- Be specific in your task description.
- Include target language/framework where known.
- Use Architecture or Security mode for high-risk changes.
- Use Detailed/Enterprise depth for larger initiatives.
- Save artefacts to build a reusable prompt library.

