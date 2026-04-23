package dev.mikepenz.a2cui.actions

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadableRegistryTest {

    @Test fun put_and_remove() {
        val r = ReadableRegistry()
        r.put(ReadableEntry("u", "user", JsonPrimitive("Ada")))
        assertEquals("Ada", r.asPromptObject()["u"]?.jsonPrimitive?.content)
        r.remove("u")
        assertNull(r.asPromptObject()["u"])
    }

    @Test fun asPromptObject_flattens_entries() {
        val r = ReadableRegistry()
        r.put(ReadableEntry("a", "", JsonPrimitive(1)))
        r.put(ReadableEntry("b", "", JsonPrimitive(2)))
        val prompt = r.asPromptObject()
        assertEquals("1", prompt["a"]?.jsonPrimitive?.content)
        assertEquals("2", prompt["b"]?.jsonPrimitive?.content)
    }
}
