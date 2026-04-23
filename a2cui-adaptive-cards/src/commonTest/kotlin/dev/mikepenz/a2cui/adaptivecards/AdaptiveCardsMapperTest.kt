package dev.mikepenz.a2cui.adaptivecards

import dev.mikepenz.a2cui.core.ComponentNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptiveCardsMapperTest {

    private fun parse(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject
    private fun List<ComponentNode>.root(): ComponentNode = first { it.id == "root" }
    private fun List<ComponentNode>.byId(id: String): ComponentNode = first { it.id == id }

    @Test
    fun rejectsNonAdaptiveCard() {
        val result = AdaptiveCardsMapper.map(parse("""{"type":"Other"}"""))
        assertNull(result.frame)
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun emptyCardProducesRootColumn() {
        val result = AdaptiveCardsMapper.map(parse("""{"type":"AdaptiveCard","body":[]}"""))
        val components = assertNotNull(result.frame).updateComponents.components
        assertEquals(1, components.size)
        val root = components.root()
        assertEquals("Column", root.component)
        assertTrue(root.children.isEmpty())
    }

    @Test
    fun textBlockMapsToTextWithVariant() {
        val card = """
            {
              "type":"AdaptiveCard",
              "body":[
                {"type":"TextBlock","text":"Hello","size":"ExtraLarge"},
                {"type":"TextBlock","text":"sub","size":"Small"}
              ]
            }
        """.trimIndent()
        val components = assertNotNull(AdaptiveCardsMapper.map(parse(card)).frame).updateComponents.components
        val textNodes = components.filter { it.component == "Text" }
        assertEquals(2, textNodes.size)
        assertEquals("h1", textNodes[0].properties["variant"]?.jsonPrimitive?.contentOrNull)
        assertEquals("caption", textNodes[1].properties["variant"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun inputTextProducesTextFieldBoundToInputsPointer() {
        val card = """
            {
              "type":"AdaptiveCard",
              "body":[{"type":"Input.Text","id":"email","label":"Email"}]
            }
        """.trimIndent()
        val components = assertNotNull(AdaptiveCardsMapper.map(parse(card)).frame).updateComponents.components
        val field = components.first { it.component == "TextField" }
        val text = field.properties["text"] as JsonObject
        assertEquals("/inputs/email", text["path"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Email", field.properties["label"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun submitActionBecomesButtonWithSubmitEvent() {
        val card = """
            {
              "type":"AdaptiveCard",
              "body":[{"type":"TextBlock","text":"hi"}],
              "actions":[{"type":"Action.Submit","title":"Send","data":{"flow":"book"}}]
            }
        """.trimIndent()
        val components = assertNotNull(AdaptiveCardsMapper.map(parse(card)).frame).updateComponents.components
        val button = components.first { it.component == "Button" }
        assertEquals("Send", button.properties["text"]?.jsonPrimitive?.contentOrNull)
        val action = button.properties["action"] as JsonObject
        val event = action["event"] as JsonObject
        assertEquals("submit", event["name"]?.jsonPrimitive?.contentOrNull)
        val context = event["context"] as JsonObject
        assertEquals("book", context["flow"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun openUrlActionBecomesButtonWithOpenUrlEvent() {
        val card = """
            {
              "type":"AdaptiveCard",
              "actions":[{"type":"Action.OpenUrl","title":"Docs","url":"https://example.com"}]
            }
        """.trimIndent()
        val components = assertNotNull(AdaptiveCardsMapper.map(parse(card)).frame).updateComponents.components
        val button = components.first { it.component == "Button" }
        val event = (button.properties["action"] as JsonObject)["event"] as JsonObject
        assertEquals("openUrl", event["name"]?.jsonPrimitive?.contentOrNull)
        val ctx = event["context"] as JsonObject
        assertEquals("https://example.com", ctx["url"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun containerWrapsChildrenAsColumn() {
        val card = """
            {
              "type":"AdaptiveCard",
              "body":[{"type":"Container","items":[
                {"type":"TextBlock","text":"a"},
                {"type":"TextBlock","text":"b"}
              ]}]
            }
        """.trimIndent()
        val components = assertNotNull(AdaptiveCardsMapper.map(parse(card)).frame).updateComponents.components
        val columns = components.filter { it.component == "Column" }
        // root + container column
        assertEquals(2, columns.size)
        val nested = columns.first { it.id != "root" }
        assertEquals(2, nested.children.size)
    }

    @Test
    fun unsupportedActionWarnsButKeepsCard() {
        val card = """
            {
              "type":"AdaptiveCard",
              "actions":[{"type":"Action.ShowCard","title":"More"}]
            }
        """.trimIndent()
        val result = AdaptiveCardsMapper.map(parse(card))
        assertNotNull(result.frame)
        assertTrue(result.warnings.any { "ShowCard" in it })
    }

    @Test
    fun choiceSetProducesChoicePicker() {
        val card = """
            {
              "type":"AdaptiveCard",
              "body":[{"type":"Input.ChoiceSet","id":"size","label":"Size","choices":[
                {"title":"Small","value":"s"},
                {"title":"Large","value":"l"}
              ]}]
            }
        """.trimIndent()
        val components = assertNotNull(AdaptiveCardsMapper.map(parse(card)).frame).updateComponents.components
        val picker = components.first { it.component == "ChoicePicker" }
        val selected = picker.properties["selected"] as JsonObject
        assertEquals("/inputs/size", selected["path"]?.jsonPrimitive?.contentOrNull)
    }
}
