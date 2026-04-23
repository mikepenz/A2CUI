# `:a2cui-agui-mock-server` — scripted AG-UI SSE server

Minimal Ktor CIO + SSE server that impersonates an AG-UI agent. Used two ways:

1. **Embedded** — host code constructs `MockAguiServer(port = 0)` and reads the
   resolved port. This is how [`:a2cui-sample-live`](../a2cui-sample-live)
   boots its default offline mode.
2. **Standalone** — run the `main()` in `MockAguiServer.kt` on a fixed port
   (`8765`) so you can curl it or point any AG-UI client at it.

## How to run standalone

The module is a plain `kotlinJvm` library and **does not wire the `application`
plugin or a `mainClass`** — so `./gradlew :a2cui-agui-mock-server:run` is not
available out of the box. Two easy alternatives:

### Option A — `-PmainClass` via the JVM

```bash
./gradlew :a2cui-agui-mock-server:classes
java -cp "$(./gradlew -q :a2cui-agui-mock-server:printRuntimeClasspath 2>/dev/null \
          || find ~/.gradle/caches -name '*.jar' | head -1)" \
     dev.mikepenz.a2cui.agui.mockserver.MockAguiServerKt
```

### Option B — add the `application` plugin locally

Append to `build.gradle.kts` while developing:

```kotlin
plugins {
    alias(baseLibs.plugins.kotlinJvm)
    application
}

application {
    mainClass = "dev.mikepenz.a2cui.agui.mockserver.MockAguiServerKt"
}
```

Then:

```bash
./gradlew :a2cui-agui-mock-server:run
```

Either way, logs show:

```
Mock AG-UI server starting on http://localhost:8765/events
```

### Option C (easiest) — just run `:a2cui-sample-live`

It boots the server in-process and wires a client automatically:

```bash
./gradlew :a2cui-sample-live:run
```

## Observing the event stream

```bash
curl -N http://localhost:8765/events
```

`-N` disables buffering so each SSE `data:` line prints as it arrives.

## Event sequence emitted per connection

Each GET to `/events` produces exactly this scripted AG-UI stream (one SSE
`data:` line per event, JSON-encoded):

1. `RUN_STARTED` — carries a random `threadId` / `runId`.
2. `TEXT_MESSAGE_START` — role `assistant`, `messageId = "msg-1"`.
3. `TEXT_MESSAGE_CONTENT` — delta `"Rendering booking form…"`.
4. `TEXT_MESSAGE_END` — closes `msg-1`.
5. `CUSTOM` (name = `"a2ui"`) — value = A2UI `createSurface` frame.
6. `CUSTOM` (name = `"a2ui"`) — value = A2UI `updateComponents` booking form.
7. `CUSTOM` (name = `"a2ui"`) — value = A2UI `updateDataModel` seeding `/form/email`.
8. `RUN_FINISHED` — outcome `success`.

Between each step the server sleeps `frameDelayMillis` (default `100ms`) so
clients see real streaming.

The A2UI frames themselves are defined in
[`SampleA2uiFrames.kt`](src/main/kotlin/dev/mikepenz/a2cui/agui/mockserver/SampleA2uiFrames.kt)
and mirror the shapes in [`:a2cui-sample-shared`](../a2cui-sample-shared).

## Embedded usage

```kotlin
val server = MockAguiServer(port = 0)     // 0 = ephemeral
val port = server.start(wait = false)     // non-blocking
val url = "http://localhost:$port/events"
// ... consume via SseTransport / AguiEventParser / AguiA2cuiBridge ...
server.stop()
```

Constructor knobs:

| Param | Default | Purpose |
|---|---|---|
| `port` | `0` | `0` = ephemeral; use `resolvedPort()` after `start()` |
| `surfaceId` | `"demo"` | embedded in the scripted A2UI frames |
| `frameDelayMillis` | `100` | pause between emitted events |

## Key files

| Concept | Path |
|---|---|
| Server + `main()` | [`src/main/kotlin/.../mockserver/MockAguiServer.kt`](src/main/kotlin/dev/mikepenz/a2cui/agui/mockserver/MockAguiServer.kt) |
| Scripted A2UI payloads | [`src/main/kotlin/.../mockserver/SampleA2uiFrames.kt`](src/main/kotlin/dev/mikepenz/a2cui/agui/mockserver/SampleA2uiFrames.kt) |
| Test — end-to-end SSE smoke | [`src/test/kotlin/.../mockserver/MockAguiServerTest.kt`](src/test/kotlin/dev/mikepenz/a2cui/agui/mockserver/MockAguiServerTest.kt) |
| Gradle wiring | [`build.gradle.kts`](build.gradle.kts) |

## How to adapt

- **Change the script.** Edit `SampleA2uiFrames.kt` or rewrite the `sse("/events")`
  block in `MockAguiServer.kt` — any sequence of AG-UI events will do; wrap A2UI
  frames in `CUSTOM` with `name = "a2ui"` to stay compatible with `AguiA2cuiBridge`.
- **Back with an LLM.** Replace the scripted `for (frame in listOf(...))` loop
  with a call into your real agent runtime that yields frames over time.
- **Different endpoint / port.** Construct `MockAguiServer(port = 9000)` or
  extend the `routing { }` block.

## See also

- Root: [`../README.md`](../README.md)
- Live client: [`:a2cui-sample-live`](../a2cui-sample-live/README.md)
- AG-UI ↔ A2UI bridge: [`:a2cui-agui`](../a2cui-agui)
- AG-UI spec: <https://docs.ag-ui.com/>
