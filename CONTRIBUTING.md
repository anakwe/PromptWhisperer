# Contributing to Prompt Whisperer

Thanks for helping improve Prompt Whisperer.

This project prioritizes engineering quality, local-first trust, and explicit control in AI-assisted workflows.

## Development Setup

```bash
git clone https://github.com/anakwe/PromptWhisperer.git
cd PromptWhisperer
./gradlew clean test
./gradlew runIde
```

## Build, Lint, and Format

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck
./gradlew clean test buildPlugin
```

## Contribution Principles

- Keep changes incremental and reviewable.
- Preserve local-first, no-telemetry architecture.
- Do not add hidden network calls.
- Do not add automatic prompt submission.
- Do not add automatic code modification behavior.
- Update docs for user-visible changes.

## Engineering Quality Expectations

- Production-quality Kotlin naming and structure.
- KDoc on non-trivial public classes/functions.
- Avoid giant classes and hard-to-extend template logic.
- Prefer composable units over monolithic branches.
- Preserve behaviour profile and guardrail semantics.

## UX Quality Expectations

When changing UI:

- keep generated prompt output visually dominant
- preserve two-stage clarification workflow
- keep profile/depth/guardrail explanations clear
- avoid novelty UI effects that conflict with JetBrains-native UX

## Pull Request Process

1. Create a focused branch.
2. Implement change with tests/docs updates as needed.
3. Run local validation.
4. Open an issue or discussion first when the change needs product/UX direction.
5. Open PR using template.
6. Address review feedback.

Use the GitHub templates under `.github/ISSUE_TEMPLATE/` for:

- bug reports
- feature requests
- security concerns
- behaviour profile suggestions

## Pull Request Checklist

- [ ] Builds and tests pass locally
- [ ] Formatting and lint checks pass (`ktlintFormat`, `ktlintCheck`)
- [ ] Documentation updated
- [ ] No unrelated file churn
- [ ] No machine-local artefacts committed
- [ ] Security and local-first constraints preserved

## Commit Message Style

Examples:

- `feat: add clarification answer panel and final prompt stage`
- `feat: refactor prompt builder into composable blocks`
- `docs: reposition README around implementation planning`
- `fix: prevent generation before clarification stage`

## Security Reporting

Do not open public issues for active vulnerabilities.

Report privately via `opensource@anakwe.org`.

## Release Process

If you are preparing a release, follow `docs/RELEASE_PROCESS.md` for version bumping, changelog updates, packaging, checksum generation, tagging, and GitHub release steps.

