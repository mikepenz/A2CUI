package dev.mikepenz.a2cui.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PropertyValueTest {

    @Test
    fun primitive_is_literal() {
        val v = PropertyValue.classify(JsonPrimitive("hi"))
        assertIs<PropertyValue.Literal>(v)
        assertEquals("hi", (v.value as JsonPrimitive).content)
    }

    @Test
    fun array_is_literal() {
        val v = PropertyValue.classify(JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))))
        assertIs<PropertyValue.Literal>(v)
    }

    @Test
    fun single_path_key_becomes_path() {
        val v = PropertyValue.classify(buildJsonObject { put("path", JsonPrimitive("/user/name")) })
        val p = assertIs<PropertyValue.Path>(v)
        assertEquals("/user/name", p.path)
    }

    @Test
    fun call_with_args_becomes_call() {
        val v = PropertyValue.classify(
            buildJsonObject {
                put("call", JsonPrimitive("openUrl"))
                put("args", buildJsonObject { put("url", JsonPrimitive("https://x")) })
            }
        )
        val c = assertIs<PropertyValue.Call>(v)
        assertEquals("openUrl", c.function)
        assertEquals("https://x", (c.args["url"] as JsonPrimitive).content)
    }

    @Test
    fun object_with_extra_keys_is_literal() {
        val v = PropertyValue.classify(
            buildJsonObject {
                put("path", JsonPrimitive("/x"))
                put("other", JsonPrimitive("y"))
            }
        )
        assertIs<PropertyValue.Literal>(v)
    }
}
