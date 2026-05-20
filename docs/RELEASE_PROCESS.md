# Release Process

This document describes the lightweight release workflow for Prompt Whisperer.

## Goals

Each release should:

- use an intentional version number
- include an updated `CHANGELOG.md`
- pass tests and packaging checks
- produce a plugin ZIP for installation
- publish a checksum for the release artefact
- create a matching Git tag and GitHub release
- avoid committing generated build artefacts

## Important Warning

Do **not** commit build artefacts.

In particular, do not commit generated files under:

- `build/`
- `build/distributions/`
- packaged plugin ZIPs
- generated checksum output files unless there is an explicit reason to version them

Release from source, not from committed binaries.

## 1. Prepare the Version Bump

Update the version in:

- `gradle.properties`

Current version property example:

```ini
version=0.1.0
```

For a new release:

1. choose the next version number
2. update `gradle.properties`
3. confirm any release references in documentation are still accurate

## 2. Update the Changelog

Before cutting the release:

1. add a new top entry to `CHANGELOG.md`
2. summarize notable user-facing changes
3. include important fixes, documentation updates, and any release caveats
4. keep wording factual and avoid overstating impact

Recommended changelog structure:

- Added
- Changed
- Fixed
- Docs

## 3. Run Validation and Build

Run the main verification flow from the repository root:

```bash
./gradlew clean build
./gradlew buildPlugin
```

If you want to run the test step explicitly as part of the release checklist:

```bash
./gradlew test
```

## 4. Locate the Plugin ZIP

The packaged plugin ZIP is generated under:

```text
build/distributions/
```

Expected artefact pattern:

```text
build/distributions/prompt-whisperer-<version>.zip
```

## 5. Generate a SHA-256 Checksum

Generate a checksum for the release ZIP:

```bash
shasum -a 256 build/distributions/*.zip
```

Record the checksum in the GitHub release notes or release verification notes.

## 6. Commit Release Metadata

Before tagging, ensure the release-related source changes are committed:

- version bump
- changelog update
- any final documentation updates

Example:

```bash
git status
git add gradle.properties CHANGELOG.md README.md docs/
git commit -m "release: prepare v0.1.0"
```

Adjust the file list as needed for the actual release contents.

## 7. Create and Push the Git Tag

Create the release tag from the committed release state:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Use the version that matches `gradle.properties` and the changelog entry.

## 8. Create the GitHub Release

Create a GitHub release for the matching tag and attach:

- the plugin ZIP from `build/distributions/`
- the SHA-256 checksum value
- release notes summarizing the release

Recommended steps:

1. open the repository releases page on GitHub
2. create a new release from tag `v0.1.0`
3. set the release title to `Prompt Whisperer v0.1.0`
4. paste the release notes
5. upload the ZIP artefact
6. include the checksum in the release body

## 9. Release Checklist

- [ ] Version updated in `gradle.properties`
- [ ] `CHANGELOG.md` updated
- [ ] `./gradlew clean build` completed successfully
- [ ] `./gradlew buildPlugin` completed successfully
- [ ] `./gradlew test` completed successfully
- [ ] ZIP artefact verified under `build/distributions/`
- [ ] SHA-256 checksum generated
- [ ] Release commit created
- [ ] Git tag created
- [ ] Git tag pushed
- [ ] GitHub release created
- [ ] No build artefacts committed

## Release Notes Template

Use this as a short starting point for `v0.1.0`.

```markdown
## Prompt Whisperer v0.1.0

Initial public release of Prompt Whisperer, a local-first engineering implementation planning tool for AI coding assistants.

### Highlights
- Two-stage clarification workflow
- Behaviour profiles and prompt depth controls
- Guardrail-driven prompt generation
- Prompt synthesis with implementation guidance, conflicts, and trade-off framing
- Local-first design with no hidden telemetry or automatic prompt submission

### Artefacts
- Plugin ZIP: `prompt-whisperer-0.1.0.zip`
- SHA-256: `<paste checksum here>`

### Notes
- Install from disk in IntelliJ using the ZIP from the release artefacts.
- See `CHANGELOG.md` for the detailed change history.
```

