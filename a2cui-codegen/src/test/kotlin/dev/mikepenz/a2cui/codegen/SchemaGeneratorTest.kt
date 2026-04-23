package dev.mikepenz.a2cui.codegen

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SchemaGeneratorTest {

    private val rating = ComponentSpec(
        name = "Rating",
        description = "5-star rating",
        factoryReference = "com.example.RatingFactory",
        props = listOf(
            PropSpec("value", PropType.INTEGER, "Current 0-5", required = true, defaultValue = "0", enumValues = emptyList()),
            PropSpec("size", PropType.STRING, "Size variant", required = false, defaultValue = "", enumValues = listOf("small", "medium", "large")),
        ),
        events = listOf(EventSpec("onChange", "Rating changed", emptyList())),
        slots = emptyList(),
    )

    @Test
    fun producesOneOfUnionOverDefinitions() {
        val schema = SchemaGenerator.generate("my-catalog", listOf(rating))
        val oneOf = schema["oneOf"] as? JsonArray
        assertNotNull(oneOf)
        assertEquals(1, oneOf.size)
        val firstRef = (oneOf[0] as JsonObject)["\$ref"]
        assertEquals(JsonPrimitive("#/definitions/Rating"), firstRef)
    }

    @Test
    fun emitsPropsWithTypesAndEnumAndDefault() {
        val schema = SchemaGenerator.generate("c", listOf(rating))
        val defs = schema["definitions"] as JsonObject
        val ratingSchema = defs["Rating"] as JsonObject
        val props = ratingSchema["properties"] as JsonObject

        val value = props["value"] as JsonObject
        assertEquals(JsonPrimitive("integer"), value["type"])
        assertEquals(JsonPrimitive(0L), value["default"])

        val size = props["size"] as JsonObject
        assertEquals(JsonPrimitive("string"), size["type"])
        val enum = size["enum"] as JsonArray
        assertEquals(3, enum.size)

        val required = ratingSchema["required"] as JsonArray
        val requiredNames = required.map { (it as JsonPrimitive).content }.toSet()
        assertTrue("value" in requiredNames)
        assertTrue("id" in requiredNames)
        assertTrue("component" in requiredNames)
    }

    @Test
    fun emitsActionSchemaWhenEventsDeclared() {
        val schema = SchemaGenerator.generate("c", listOf(rating))
        val defs = schema["definitions"] as JsonObject
        val ratingSchema = defs["Rating"] as JsonObject
        val props = ratingSchema["properties"] as JsonObject
        assertNotNull(props["action"])
    }

    @Test
    fun emitsChildrenArrayWhenNoSlotsDeclared() {
        val schema = SchemaGenerator.generate("c", listOf(rating))
        val props = ((schema["definitions"] as JsonObject)["Rating"] as JsonObject)["properties"] as JsonObject
        val children = props["children"] as JsonObject
        assertEquals(JsonPrimitive("array"), children["type"])
    }

    @Test
    fun emitsSlotsWhenDeclared() {
        val tabs = rating.copy(
            name = "Tabs",
            slots = listOf(
                SlotSpec("headers", "Tab headers", multiple = true),
                SlotSpec("active", "Active tab id", multiple = false),
            ),
        )
        val schema = SchemaGenerator.generate("c", listOf(tabs))
        val props = ((schema["definitions"] as JsonObject)["Tabs"] as JsonObject)["properties"] as JsonObject
        assertNotNull(props["headers"])
        assertNotNull(props["active"])
        assertEquals(null, props["children"])
    }
}
