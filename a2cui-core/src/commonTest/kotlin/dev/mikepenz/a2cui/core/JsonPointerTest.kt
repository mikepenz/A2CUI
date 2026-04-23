package dev.mikepenz.a2cui.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JsonPointerTest {

    private val root = buildJsonObject {
        put("user", buildJsonObject {
            put("name", JsonPrimitive("Ada"))
            put("emails", buildJsonArray {
                add(JsonPrimitive("a@x"))
                add(JsonPrimitive("b@x"))
            })
        })
    }

    @Test fun parse_root_is_empty() = assertEquals(emptyList(), JsonPointer.parse(""))

    @Test fun parse_simple() = assertEquals(listOf("a", "b", "c"), JsonPointer.parse("/a/b/c"))

    @Test fun parse_escapes() =
        assertEquals(listOf("a/b", "c~d"), JsonPointer.parse("/a~1b/c~0d"))

    @Test fun encode_roundtrip() {
        val ptr = "/user/emails/0"
        assertEquals(ptr, JsonPointer.encode(JsonPointer.parse(ptr)))
    }

    @Test fun read_object_key() {
        assertEquals("Ada", JsonPointer.read(root, "/user/name")?.jsonPrimitive?.content)
    }

    @Test fun read_array_index() {
        assertEquals("b@x", JsonPointer.read(root, "/user/emails/1")?.jsonPrimitive?.content)
    }

    @Test fun read_missing_is_null() {
        assertNull(JsonPointer.read(root, "/user/age"))
        assertNull(JsonPointer.read(root, "/user/emails/99"))
        assertNull(JsonPointer.read(root, "/user/name/deeper"))
    }

    @Test fun read_root() {
        assertEquals(root, JsonPointer.read(root, ""))
    }

    @Test fun write_creates_intermediate_objects() {
        val updated = JsonPointer.write(JsonObject(emptyMap()), "/form/email", JsonPrimitive("u@x"))
        assertEquals(
            "u@x",
            updated.jsonObject["form"]?.jsonObject?.get("email")?.jsonPrimitive?.content,
        )
    }

    @Test fun write_replaces_existing_scalar() {
        val updated = JsonPointer.write(root, "/user/name", JsonPrimitive("Grace"))
        assertEquals("Grace", JsonPointer.read(updated, "/user/name")?.jsonPrimitive?.content)
        // Original is untouched (structural sharing via new trees).
        assertEquals("Ada", JsonPointer.read(root, "/user/name")?.jsonPrimitive?.content)
    }

    @Test fun write_appends_to_array() {
        val updated = JsonPointer.write(root, "/user/emails/2", JsonPrimitive("c@x"))
        val arr = JsonPointer.read(updated, "/user/emails") as JsonArray
        assertEquals(3, arr.size)
        assertEquals("c@x", arr[2].jsonPrimitive.content)
    }

    @Test fun write_rejects_out_of_bounds_array() {
        assertFailsWith<IllegalArgumentException> {
            JsonPointer.write(root, "/user/emails/99", JsonPrimitive("x"))
        }
    }

    @Test fun write_rejects_non_numeric_index_on_array() {
        assertFailsWith<IllegalArgumentException> {
            JsonPointer.write(root, "/user/emails/foo", JsonPrimitive("x"))
        }
    }
}
