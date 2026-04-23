package dev.mikepenz.a2cui.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class A2uiFrameTest {

    @Test
    fun decodes_createSurface() {
        val json = """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "booking",
                "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json",
                "theme": { "primaryColor": "#00BFFF" },
                "sendDataModel": true
              }
            }
        """.trimIndent()

        val frame = A2uiJson.decodeFromString<A2uiFrame>(json)
        val createSurface = assertIs<A2uiFrame.CreateSurface>(frame)
        assertEquals("v0.9", createSurface.version)
        assertEquals("booking", createSurface.createSurface.surfaceId)
        assertEquals(
            "https://a2ui.org/specification/v0_9/basic_catalog.json",
            createSurface.createSurface.catalogId,
        )
        assertEquals(true, createSurface.createSurface.sendDataModel)
        assertEquals(
            "#00BFFF",
            createSurface.createSurface.theme?.get("primaryColor")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun decodes_updateComponents_preserving_component_properties() {
        val json = """
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "booking",
                "components": [
                  { "id": "root", "component": "Column", "children": ["title", "email"] },
                  { "id": "title", "component": "Text", "text": "Book Your Table", "variant": "h1" },
                  { "id": "email", "component": "TextField",
                    "value": { "path": "/form/email" },
                    "label": "Email" }
                ]
              }
            }
        """.trimIndent()

        val frame = A2uiJson.decodeFromString<A2uiFrame>(json)
        val body = assertIs<A2uiFrame.UpdateComponents>(frame).updateComponents
        assertEquals("booking", body.surfaceId)
        assertEquals(3, body.components.size)

        val root = body.components[0]
        assertEquals("root", root.id)
        assertEquals("Column", root.component)
        assertEquals(listOf("title", "email"), root.children)
        assertTrue(root.properties.isEmpty())

        val title = body.components[1]
        assertEquals("Text", title.component)
        assertEquals("Book Your Table", title.properties["text"]?.jsonPrimitive?.content)
        assertEquals("h1", title.properties["variant"]?.jsonPrimitive?.content)

        val email = body.components[2]
        assertEquals("TextField", email.component)
        val valueProp = email.properties["value"]
        val classified = PropertyValue.classify(valueProp!!)
        assertIs<PropertyValue.Path>(classified)
        assertEquals("/form/email", classified.path)
    }

    @Test
    fun decodes_updateDataModel() {
        val json = """
            { "version": "v0.9",
              "updateDataModel": {
                "surfaceId": "booking",
                "path": "/form/email",
                "value": "user@example.com" } }
        """.trimIndent()

        val frame = A2uiJson.decodeFromString<A2uiFrame>(json)
        val body = assertIs<A2uiFrame.UpdateDataModel>(frame).updateDataModel
        assertEquals("/form/email", body.path)
        assertEquals("user@example.com", (body.value as JsonPrimitive).content)
    }

    @Test
    fun decodes_deleteSurface() {
        val frame = A2uiJson.decodeFromString<A2uiFrame>(
            """{ "version": "v0.9", "deleteSurface": { "surfaceId": "booking" } }"""
        )
        assertEquals("booking", assertIs<A2uiFrame.DeleteSurface>(frame).deleteSurface.surfaceId)
    }

    @Test
    fun rejects_unknown_frame() {
        assertFailsWith<kotlinx.serialization.SerializationException> {
            A2uiJson.decodeFromString<A2uiFrame>("""{"version":"v0.9","mystery":{}}""")
        }
    }

    @OptIn(ExperimentalA2uiV010::class)
    @Test
    fun v010_dataModelPatch_roundtrip_requires_opt_in() {
        val json = """
            {
              "version": "v0.10",
              "dataModelPatch": {
                "surfaceId": "s",
                "operations": [
                  { "op": "add", "path": "/users/-", "value": { "name": "Ada" } },
                  { "op": "remove", "path": "/users/0" }
                ]
              }
            }
        """.trimIndent()

        val frame = A2uiJson.decodeFromString<A2uiFrame>(json)
        val patch = assertIs<A2uiFrame.DataModelPatch>(frame)
        assertEquals("v0.10", patch.version)
        assertEquals("s", patch.dataModelPatch.surfaceId)
        assertEquals(2, patch.dataModelPatch.operations.size)
        assertEquals("add", patch.dataModelPatch.operations[0].op)
        assertEquals("/users/-", patch.dataModelPatch.operations[0].path)

        // Round-trip back through JSON preserves the payload.
        val encoded = A2uiJson.encodeToString<A2uiFrame>(patch)
        val decoded = A2uiJson.decodeFromString<A2uiFrame>(encoded)
        assertEquals(patch, decoded)
    }

    @Test
    fun roundtrip_updateComponents_preserves_properties() {
        val original = A2uiFrame.UpdateComponents(
            version = "v0.9",
            updateComponents = A2uiFrame.UpdateComponents.Body(
                surfaceId = "s1",
                components = listOf(
                    ComponentNode(
                        id = "t",
                        component = "Text",
                        properties = kotlinx.serialization.json.buildJsonObject {
                            put("text", JsonPrimitive("Hello"))
                            put("variant", JsonPrimitive("h2"))
                        },
                    ),
                ),
            ),
        )

        val encoded = A2uiJson.encodeToString<A2uiFrame>(original)
        val decoded = A2uiJson.decodeFromString<A2uiFrame>(encoded)
        assertEquals(original, decoded)

        // Ensure wire payload inlines `text` at the node level, not inside a `properties` object.
        val parsed = A2uiJson.parseToJsonElement(encoded).jsonObject
        val node = parsed["updateComponents"]!!
            .jsonObject["components"]!!
            .let { it as kotlinx.serialization.json.JsonArray }[0]
            .jsonObject
        assertEquals("Hello", node["text"]?.jsonPrimitive?.content)
        assertEquals("h2", node["variant"]?.jsonPrimitive?.content)
        assertTrue("properties" !in node)
    }
}
