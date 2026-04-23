package dev.mikepenz.a2cui.actions

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypedArgsAccumulatorTest {

    @Serializable
    data class Person(val name: String, val age: Int = 0)

    @Test fun incomplete_partial_stays_in_progress() {
        val a = TypedArgsAccumulator(Person.serializer())
        val s = a.onDelta("""{"name":"Al""")
        assertTrue(s is ToolCallStatus.InProgress, "expected InProgress but got $s")
    }

    @Test fun full_decodable_partial_emits_executing() {
        val a = TypedArgsAccumulator(Person.serializer())
        a.onDelta("""{"name":"Alice","age":30}""")
        val s = a.status
        assertTrue(s is ToolCallStatus.Executing, "expected Executing but got $s")
        assertEquals(Person("Alice", 30), (s as ToolCallStatus.Executing).args)
    }

    @Test fun executing_then_end_transitions_to_complete() {
        val a = TypedArgsAccumulator(Person.serializer())
        a.onDelta("""{"name":"Alice","age":30}""")
        val end = a.onEnd(JsonPrimitive("ok"))
        assertTrue(end is ToolCallStatus.Complete)
        assertEquals(Person("Alice", 30), (end as ToolCallStatus.Complete).args)
        assertEquals(JsonPrimitive("ok"), end.result)
    }

    @Test fun progressive_deltas_emit_executing_only_when_decodable() {
        val a = TypedArgsAccumulator(Person.serializer())
        val s1 = a.onDelta("""{"name":"Al""")
        assertTrue(s1 is ToolCallStatus.InProgress)
        val s2 = a.onDelta("""ice","age":30""")
        // Still not a complete JSON object.
        assertTrue(s2 is ToolCallStatus.InProgress, "mid-stream without closing brace should stay InProgress, got $s2")
        val s3 = a.onDelta("}")
        assertTrue(s3 is ToolCallStatus.Executing, "after closing brace expected Executing, got $s3")
        assertEquals(Person("Alice", 30), (s3 as ToolCallStatus.Executing).args)
    }

    @Test fun end_with_invalid_buffer_emits_failed() {
        val a = TypedArgsAccumulator(Person.serializer())
        a.onDelta("""{"nam""")
        val end = a.onEnd(JsonNull)
        assertTrue(end is ToolCallStatus.Failed, "expected Failed, got $end")
    }
}
