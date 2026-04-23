# :a2cui-transport

Ktor-backed transports for streaming A2UI frames into an [A2cuiSurface](../a2cui-compose/README.md). Provides `A2uiTransport` as the core abstraction plus three implementations: `SseTransport`, `WebSocketTransport`, and `FakeTransport` (tests + scripted demos).

## When to use / when not to

- **Use** when you need a network-capable source of A2UI JSON frames for a `SurfaceController`.
- **Use** `FakeTransport` in unit tests and for demo playback.
- **Don't use directly** if you're consuming AG-UI — pair `SseTransport` with `:a2cui-agui` so the bridge can unwrap `CUSTOM(name="a2ui")` events and fan state/tool events out separately.

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.mikepenz.a2cui:a2cui-transport:0.1.0-a01")
    // Pick the Ktor engine appropriate for your platform:
    implementation("io.ktor:ktor-client-okhttp:3.4.3")   // android / jvm
    // implementation("io.ktor:ktor-client-darwin:3.4.3") // ios / macos
    // implementation("io.ktor:ktor-client-cio:3.4.3")    // jvm / desktop
}
```

Version catalog variant:

```toml
[versions]
a2cui = "0.1.0-a01"
ktor = "3.4.3"

[libraries]
a2cui-transport = { module = "dev.mikepenz.a2cui:a2cui-transport", version.ref = "a2cui" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
```

```kotlin
dependencies {
    implementation(libs.a2cui.transport)
    implementation(libs.ktor.client.okhttp)
}
```

## Targets supported

`android`, `jvm`, `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `macosX64`, `js`, `wasmJs`. Ktor **3.4.3** is the tested version; use the Ktor engine artifact matching each target (OkHttp for JVM/Android, Darwin for iOS/macOS, CIO for Desktop/JS as appropriate).

## Public API

| Symbol | Purpose | Source |
|---|---|---|
| `A2uiTransport` | `AutoCloseable` interface: `incoming(): Flow<String>` + `suspend send(A2uiClientMessage)` (default impl calls `sendRaw`). | [`A2uiTransport.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/transport/A2uiTransport.kt) |
| `SseTransport` | Ktor SSE client; optional `sendUrl` for POSTing outbound `A2uiClientMessage` JSON. | [`SseTransport.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/transport/SseTransport.kt) |
| `WebSocketTransport` | Ktor WebSocket client with headers. | [`WebSocketTransport.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/transport/WebSocketTransport.kt) |
| `FakeTransport` | Replay-buffered test double exposing `tryEmit` / `emit` / `emitAll` and a `sent: List<String>` recorder. | [`FakeTransport.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/transport/FakeTransport.kt) |
| `Headers` helpers | Extension applying `Map<String, String>` to Ktor requests (works around KMP `Map.forEach` inference). | [`Headers.kt`](src/commonMain/kotlin/dev/mikepenz/a2cui/transport/Headers.kt) |

## Minimal example

```kotlin
import dev.mikepenz.a2cui.transport.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.sse.SSE

val client = HttpClient(OkHttp) { install(SSE) }
val transport: A2uiTransport = SseTransport(
    httpClient = client,
    url = "https://agent.example/a2ui/events",
    sendUrl = "https://agent.example/a2ui/client",
    headers = mapOf("Authorization" to "Bearer $token"),
)

// Hand off to SurfaceController:
val controller = rememberSurfaceController(
    catalogs = listOf(Material3BasicCatalog),
    transport = transport,
)
```

Scripted test:

```kotlin
val fake = FakeTransport()
val controller = SurfaceController(registry, fake, scope)
controller.start()
fake.emit(createSurfaceJson)
fake.emit(updateComponentsJson)
```

## Integration

- `SurfaceController` in `:a2cui-compose` consumes `transport.incoming()` and sends outbound `A2uiClientMessage.Action` via `transport.send(...)`.
- `:a2cui-agui` wraps an incoming transport's frames and first passes them through `AguiEventParser` before handing A2UI payloads to a `SurfaceController`.

## Known limitations

- `FakeTransport` uses `tryEmit` on an unbounded replay buffer instead of `runBlocking` (unavailable in KMP common).
- WebSocket `close()`/`cancel()` is unreachable via ABI on some KMP targets — rely on structured-concurrency scope cancellation to tear down sessions.
- No built-in retry/backoff; wrap with your own `retryWhen` on the `Flow<String>` if needed.
- Ktor SSE API is stable on 3.4.x but expect cosmetic warnings on new platform targets until Ktor 3.5.

## Links

- Root: [../README.md](../README.md)
- Core types: [../a2cui-core/README.md](../a2cui-core/README.md)
- Compose consumer: [../a2cui-compose/README.md](../a2cui-compose/README.md)
- AG-UI bridge: [../a2cui-agui/README.md](../a2cui-agui/README.md)
