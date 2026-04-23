package dev.mikepenz.a2cui.core

/**
 * One outcome of attempting to parse a single JSON frame from the A2UI server stream.
 *
 * Parse failures are materialised as [ParseError] instead of being thrown so the transport
 * layer can forward an [A2uiClientMessage.Error] response upstream without tearing down the
 * whole agent run.
 */
public sealed interface A2uiStreamEvent {

    /** A successfully parsed server → client frame. */
    public data class Frame(val frame: A2uiFrame) : A2uiStreamEvent

    /** A frame the parser could not decode. Includes the original raw string for diagnostics. */
    public data class ParseError(
        val raw: String,
        val error: A2uiClientMessage.Error.Body,
    ) : A2uiStreamEvent
}
