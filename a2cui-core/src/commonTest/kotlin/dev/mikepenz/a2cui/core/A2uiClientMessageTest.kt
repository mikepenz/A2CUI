package dev.mikepenz.a2cui.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class A2uiClientMessageTest {

    @Test
    fun decodes_action() {
        val json = """
            {
              "version": "v0.9",
              "action": {
                "surfaceId": "booking",
                "name": "submit_form",
                "sourceComponentId": "submit",
                "context": { "email": "u@example.com" },
                "timestamp": 1713806400000
              }
            }
        """.trimIndent()

        val msg = A2uiJson.decodeFromString<A2uiClientMessage>(json)
        val action = assertIs<A2uiClientMessage.Action>(msg).action
        assertEquals("submit_form", action.name)
        assertEquals("submit", action.sourceComponentId)
        assertEquals("u@example.com", action.context["email"]?.jsonPrimitive?.content)
    }

    @Test
    fun encodes_error() {
        val err = A2uiClientMessage.Error(
            version = "v0.9",
            error = A2uiClientMessage.Error.Body(
                code = "VALIDATION_FAILED",
                message = "unknown component",
                path = "/updateComponents/components/0",
                surfaceId = "booking",
            ),
        )
        val encoded = A2uiJson.encodeToString<A2uiClientMessage>(err)
        val decoded = A2uiJson.decodeFromString<A2uiClientMessage>(encoded)
        assertEquals(err, decoded)
    }
}
