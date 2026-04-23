package dev.mikepenz.a2cui.agui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AguiEventParserTest {

    private val parser = AguiEventParser()

    @Test fun decodes_run_started() {
        val raw = """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}"""
        val event = (parser.parseOne(raw) as AguiStreamEvent.Event).event
        val started = assertIs<AguiEvent.RunStarted>(event)
        assertEquals("t1", started.threadId)
        assertEquals("r1", started.runId)
    }

    @Test fun decodes_text_message_content() {
        val raw = """{"type":"TEXT_MESSAGE_CONTENT","messageId":"m1","delta":"hi"}"""
        val event = (parser.parseOne(raw) as AguiStreamEvent.Event).event
        val c = assertIs<AguiEvent.TextMessageContent>(event)
        assertEquals("m1", c.messageId)
        assertEquals("hi", c.delta)
    }

    @Test fun decodes_tool_call_triplet() {
        val start = parser.parseOne(
            """{"type":"TOOL_CALL_START","toolCallId":"tc1","toolCallName":"fetch"}"""
        )
        val args = parser.parseOne(
            """{"type":"TOOL_CALL_ARGS","toolCallId":"tc1","delta":"{\"x\":1}"}"""
        )
        val end = parser.parseOne(
            """{"type":"TOOL_CALL_END","toolCallId":"tc1"}"""
        )
        assertIs<AguiStreamEvent.Event>(start).event.let { assertIs<AguiEvent.ToolCallStart>(it) }
        assertIs<AguiStreamEvent.Event>(args).event.let { assertIs<AguiEvent.ToolCallArgs>(it) }
        assertIs<AguiStreamEvent.Event>(end).event.let { assertIs<AguiEvent.ToolCallEnd>(it) }
    }

    @Test fun decodes_custom_event_with_a2ui_payload() {
        val raw = """{"type":"CUSTOM","name":"a2ui","value":{"hello":"world"}}"""
        val event = (parser.parseOne(raw) as AguiStreamEvent.Event).event
        val custom = assertIs<AguiEvent.Custom>(event)
        assertEquals("a2ui", custom.name)
    }

    @Test fun decodes_run_finished_with_interrupt() {
        val raw = """
            {"type":"RUN_FINISHED","threadId":"t","runId":"r","outcome":"interrupt",
             "interrupts":[{"id":"i1","reason":"tool_call","toolCallId":"tc1",
                            "message":"Approve?"}]}
        """.trimIndent()
        val event = (parser.parseOne(raw) as AguiStreamEvent.Event).event
        val fin = assertIs<AguiEvent.RunFinished>(event)
        assertEquals("interrupt", fin.outcome)
        assertEquals(1, fin.interrupts.size)
        assertEquals("tool_call", fin.interrupts[0].reason)
    }

    @Test fun bad_json_materialises_parse_error() {
        val out = parser.parseOne("not json")
        assertIs<AguiStreamEvent.ParseError>(out)
    }

    @Test fun unknown_type_is_parse_error() {
        val out = parser.parseOne("""{"type":"MYSTERY_EVENT","x":1}""")
        assertIs<AguiStreamEvent.ParseError>(out)
    }
}
