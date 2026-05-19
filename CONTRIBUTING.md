# Contributing to Prompt Whisperer

Thanks for your interest in contributing.

## Development Setup

```bash
git clone https://github.com/anakwe/PromptWhisperer.git
cd PromptWhisperer
./gradlew clean test
./gradlew runIde
```

## Build and Test

```bash
./gradlew clean test buildPlugin
```

## Contribution Guidelines

- Keep changes focused and reviewable.
- Preserve local-first and security-first principles.
- Do not add hidden network calls or telemetry.
- Add tests for new behavior when practical.
- Update docs for user-visible changes.

## Pull Request Checklist

- [ ] Code compiles
- [ ] Tests pass
- [ ] Docs updated
- [ ] No unrelated file changes
- [ ] No local machine artefacts committed

## Commit Style

Use clear commit messages, for example:

- `feat: add markdown preview toggle`
- `fix: block retry when no material change`
- `docs: update troubleshooting workflow`

