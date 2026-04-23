# Publication guide

A2CUI publishes to Maven Central via the `com.mikepenz.convention.publishing` plugin.

## Coordinates

- Group: `dev.mikepenz.a2cui`
- Modules:
  - `dev.mikepenz.a2cui:a2cui-core`
  - `dev.mikepenz.a2cui:a2cui-transport`
  - `dev.mikepenz.a2cui:a2cui-compose`
  - `dev.mikepenz.a2cui:a2cui-agui`
  - `dev.mikepenz.a2cui:a2cui-actions`
  - `dev.mikepenz.a2cui:a2cui-codegen`
  - `dev.mikepenz.a2cui:a2cui-adaptive-cards`

`:a2cui-sample` is **not** published.

## Local snapshot

```bash
./gradlew publishToMavenLocal
```

## Release

Update `VERSION_NAME` + `app.version` in `gradle.properties`. Tag the release commit. CI (to be wired up in a follow-up) runs `publishToMavenCentral` on tag push.

## Required environment variables

- `ORG_GRADLE_PROJECT_mavenCentralUsername`
- `ORG_GRADLE_PROJECT_mavenCentralPassword`
- `ORG_GRADLE_PROJECT_signingInMemoryKey`
- `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`

## Binary compatibility

`com.mikepenz.binary-compatibility-validator.enabled=true` in `gradle.properties` produces `api/` dumps per module. `./gradlew apiCheck` verifies no breaking changes; `./gradlew apiDump` updates them when changes are intentional.
