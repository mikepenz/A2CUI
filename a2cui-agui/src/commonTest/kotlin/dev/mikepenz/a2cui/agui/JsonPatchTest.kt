package dev.mikepenz.a2cui.agui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPatchTest {

    @Test fun add_replaces_value_at_path() {
        val root = buildJsonObject { put("a", JsonPrimitive(1)) }
        val patch = buildJsonArray {
            add(buildJsonObject {
                put("op", JsonPrimitive("add"))
                put("path", JsonPrimitive("/b"))
                put("value", JsonPrimitive(2))
            })
        }
        val out = JsonPatch.apply(root, patch).jsonObject
        assertEquals("1", out["a"]?.jsonPrimitive?.content)
        assertEquals("2", out["b"]?.jsonPrimitive?.content)
    }

    @Test fun replace_overwrites_existing() {
        val root = buildJsonObject { put("a", JsonPrimitive("old")) }
        val patch = buildJsonArray {
            add(buildJsonObject {
                put("op", JsonPrimitive("replace"))
                put("path", JsonPrimitive("/a"))
                put("value", JsonPrimitive("new"))
            })
        }
        val out = JsonPatch.apply(root, patch).jsonObject
        assertEquals("new", out["a"]?.jsonPrimitive?.content)
    }

    @Test fun remove_deletes_field() {
        val root = buildJsonObject {
            put("a", JsonPrimitive(1))
            put("b", JsonPrimitive(2))
        }
        val patch = buildJsonArray {
            add(buildJsonObject {
                put("op", JsonPrimitive("remove"))
                put("path", JsonPrimitive("/a"))
            })
        }
        val out = JsonPatch.apply(root, patch).jsonObject
        assertNull(out["a"])
        assertEquals("2", out["b"]?.jsonPrimitive?.content)
    }

    @Test fun move_relocates_value() {
        val root = buildJsonObject { put("a", JsonPrimitive(42)) }
        val patch = buildJsonArray {
            add(buildJsonObject {
                put("op", JsonPrimitive("move"))
                put("from", JsonPrimitive("/a"))
                put("path", JsonPrimitive("/b"))
            })
        }
        val out = JsonPatch.apply(root, patch).jsonObject
        assertNull(out["a"])
        assertEquals("42", out["b"]?.jsonPrimitive?.content)
    }

    @Test fun unknown_op_is_noop() {
        val root = buildJsonObject { put("a", JsonPrimitive(1)) }
        val patch = buildJsonArray {
            add(buildJsonObject {
                put("op", JsonPrimitive("test"))
                put("path", JsonPrimitive("/a"))
                put("value", JsonPrimitive(1))
            })
        }
        val out = JsonPatch.apply(root, patch).jsonObject
        assertEquals("1", out["a"]?.jsonPrimitive?.content)
    }
}
