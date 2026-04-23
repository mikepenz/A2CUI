package dev.mikepenz.a2cui.agui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parse a stream of JSON strings (one AG-UI event per element) into typed [AguiEvent]s.
 *
 * Failures are materialised as [AguiStreamEvent.ParseError] so a malformed or unknown-type
 * event does not tear down the run.
 */
public class AguiEventParser(private val json: Json = AguiJson) {

    public fun parse(frames: Flow<String>): Flow<AguiStreamEvent> = frames.map(::parseOne)

    public fun parseOne(raw: String): AguiStreamEvent = try {
        AguiStreamEvent.Event(json.decodeFromString<AguiEvent>(raw))
    } catch (e: SerializationException) {
        AguiStreamEvent.ParseError(raw, e.message ?: "Invalid AG-UI event")
    } catch (e: IllegalArgumentException) {
        AguiStreamEvent.ParseError(raw, e.message ?: "Invalid AG-UI event")
    }
}

/** Outcome of parsing a single AG-UI event frame. */
public sealed interface AguiStreamEvent {
    public data class Event(val event: AguiEvent) : AguiStreamEvent
    public data class ParseError(val raw: String, val message: String) : AguiStreamEvent
}
