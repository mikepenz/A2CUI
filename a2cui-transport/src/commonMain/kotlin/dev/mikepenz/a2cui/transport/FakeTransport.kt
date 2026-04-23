package dev.mikepenz.a2cui.transport

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-memory [A2uiTransport] for tests. Captures every outbound raw JSON sent by the renderer
 * in [sent]; inbound frames are hand-fed via [emit] or seeded in the constructor.
 *
 * Usage:
 * ```
 * val transport = FakeTransport(listOf("""{"version":"v0.9","deleteSurface":{...}}"""))
 * // ...collect transport.incoming() in the test...
 * assertEquals(listOf("""{"version":"v0.9","action":{...}}"""), transport.sent)
 * ```
 */
public class FakeTransport(
    initial: List<String> = emptyList(),
) : A2uiTransport {

    private val buffer = MutableSharedFlow<String>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
    private val _sent: MutableList<String> = mutableListOf()
    private var closed = false

    init {
        // Replay buffer is unbounded, so tryEmit always succeeds on SUSPEND overflow mode.
        for (raw in initial) require(buffer.tryEmit(raw)) { "Failed to seed FakeTransport" }
    }

    /** Frames captured from [sendRaw] calls, in order. */
    public val sent: List<String> get() = _sent.toList()

    /** Emit an inbound JSON frame (as if the server sent it). */
    public suspend fun emit(raw: String) {
        check(!closed) { "FakeTransport is closed" }
        buffer.emit(raw)
    }

    /** Synchronous variant — safe because [buffer] is configured with an unbounded capacity. */
    public fun tryEmit(raw: String) {
        check(!closed) { "FakeTransport is closed" }
        require(buffer.tryEmit(raw)) { "Unexpected backpressure on FakeTransport" }
    }

    /** Emit multiple inbound frames in order. */
    public suspend fun emitAll(raws: Iterable<String>) {
        for (r in raws) emit(r)
    }

    override fun incoming(): Flow<String> = buffer.asSharedFlow()

    override suspend fun sendRaw(raw: String) {
        check(!closed) { "FakeTransport is closed" }
        _sent.add(raw)
    }

    override fun close() { closed = true }
}
