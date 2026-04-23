package dev.mikepenz.a2cui.core

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class A2uiParserTest {

    private val parser = A2uiParser()

    @Test
    fun parses_stream_of_valid_frames() = runTest {
        val frames = flowOf(
            """{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""",
            """{"version":"v0.9","deleteSurface":{"surfaceId":"s"}}""",
        )
        parser.parse(frames).test {
            val a = assertIs<A2uiStreamEvent.Frame>(awaitItem())
            assertIs<A2uiFrame.CreateSurface>(a.frame)
            val b = assertIs<A2uiStreamEvent.Frame>(awaitItem())
            assertIs<A2uiFrame.DeleteSurface>(b.frame)
            awaitComplete()
        }
    }

    @Test
    fun materialises_parse_error_for_invalid_json() = runTest {
        val events = flowOf("not-json-at-all")
        parser.parse(events).test {
            val err = assertIs<A2uiStreamEvent.ParseError>(awaitItem())
            assertEquals(ErrorCode.PARSE_ERROR, err.error.code)
            assertEquals("not-json-at-all", err.raw)
            awaitComplete()
        }
    }

    @Test
    fun materialises_parse_error_for_unknown_frame_type() = runTest {
        val events = flowOf("""{"version":"v0.9","surprise":{}}""")
        parser.parse(events).test {
            val err = assertIs<A2uiStreamEvent.ParseError>(awaitItem())
            assertEquals(ErrorCode.PARSE_ERROR, err.error.code)
            awaitComplete()
        }
    }

    @Test
    fun stream_survives_one_bad_frame() = runTest {
        val events = flowOf(
            """{"version":"v0.9","createSurface":{"surfaceId":"s","catalogId":"c"}}""",
            "garbage",
            """{"version":"v0.9","deleteSurface":{"surfaceId":"s"}}""",
        )
        parser.parse(events).test {
            assertIs<A2uiStreamEvent.Frame>(awaitItem())
            assertIs<A2uiStreamEvent.ParseError>(awaitItem())
            assertIs<A2uiStreamEvent.Frame>(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun parse_one_produces_same_result_as_streaming() {
        val raw = """{"version":"v0.9","deleteSurface":{"surfaceId":"main"}}"""
        val single = parser.parseOne(raw) as A2uiStreamEvent.Frame
        assertIs<A2uiFrame.DeleteSurface>(single.frame)
    }

    @Test
    fun encode_roundtrip_frame() {
        val original: A2uiFrame = A2uiFrame.CreateSurface(
            version = "v0.9",
            createSurface = A2uiFrame.CreateSurface.Body(
                surfaceId = "booking",
                catalogId = "https://a2ui.org/specification/v0_9/basic_catalog.json",
            ),
        )
        val encoded = parser.encodeFrame(original)
        val decoded = (parser.parseOne(encoded) as A2uiStreamEvent.Frame).frame
        assertEquals(original, decoded)
    }

    @Test
    fun encode_outbound_action() {
        val msg: A2uiClientMessage = A2uiClientMessage.Action(
            version = A2UI_PROTOCOL_VERSION,
            action = A2uiClientMessage.Action.Body(
                surfaceId = "booking",
                name = "submit_form",
                sourceComponentId = "submit",
            ),
        )
        val encoded = parser.encode(msg)
        val decoded = A2uiJson.decodeFromString<A2uiClientMessage>(encoded)
        assertEquals(msg, decoded)
    }

    @Test
    fun wrap_error_attaches_protocol_version() {
        val wrapped = parser.wrapError(
            A2uiClientMessage.Error.Body(code = "X", message = "oops"),
        )
        assertEquals(A2UI_PROTOCOL_VERSION, wrapped.version)
        assertEquals("X", wrapped.error.code)
    }
}
