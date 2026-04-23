package dev.mikepenz.a2cui.actions

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreamingArgsMergerTest {

    @Test fun full_object_in_one_delta_parses() {
        val m = StreamingArgsMerger()
        val out = m.append("""{"city":"San Francisco"}""")
        assertEquals("San Francisco", out["city"]?.jsonPrimitive?.content)
    }

    @Test fun partial_string_returns_tolerant_object() {
        val m = StreamingArgsMerger()
        val first = m.append("""{"city":"San""")
        // Tolerant: closers `"` + `}` applied; produces `{"city":"San"}` best-effort.
        assertEquals("San", first["city"]?.jsonPrimitive?.content)
        val second = m.append(""" Francisco"}""")
        assertEquals("San Francisco", second["city"]?.jsonPrimitive?.content)
    }

    @Test fun balances_nested_braces_and_brackets() {
        val m = StreamingArgsMerger()
        val out = m.append("""{"rows":[{"cells":["Apr","$12k"]""")
        val rows = out["rows"] as? JsonArray
        assertTrue(rows != null && rows.isNotEmpty())
        val firstRow = rows[0].jsonObject
        val cells = firstRow["cells"]?.jsonArray
        assertEquals(2, cells?.size)
        assertEquals("Apr", cells?.get(0)?.jsonPrimitive?.content)
    }

    @Test fun falls_back_to_last_good_on_broken_input() {
        val m = StreamingArgsMerger()
        val good = m.append("""{"a":1}""")
        assertEquals("1", good["a"]?.jsonPrimitive?.content)
        // Append trailing garbage that no number of closers can salvage
        val after = m.append(",,,,,,")
        assertEquals("1", after["a"]?.jsonPrimitive?.content)
    }

    @Test fun buffered_returns_raw_concatenation() {
        val m = StreamingArgsMerger()
        m.append("""{"x":""")
        m.append("1}")
        assertEquals("""{"x":1}""", m.buffered())
        assertEquals("1", m.finalParse()["x"]?.jsonPrimitive?.content)
    }
}
