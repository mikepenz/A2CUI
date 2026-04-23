package dev.mikepenz.a2cui.sample.custom

import dev.mikepenz.a2cui.compose.ComponentRegistry
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomDemoCatalogTest {

    @Test
    fun catalogIdMatchesKspOption() {
        assertEquals("custom-demo", CustomDemoCatalog.id)
    }

    @Test
    fun installRegistersCustomFactories() {
        val registry = ComponentRegistry()
        CustomDemoCatalog.install(registry)
        assertTrue("Rating" in registry, "Rating must be registered")
        assertTrue("Badge" in registry, "Badge must be registered")
        assertNotNull(registry["Rating"])
        assertNotNull(registry["Badge"])
    }

    @Test
    fun schemaExposesRatingDefinition() {
        val schema = CustomDemoCatalog.toJsonSchema()
        val definitions = schema["definitions"] as? JsonObject
        assertNotNull(definitions, "definitions must be present on the generated schema")
        assertNotNull(definitions["Rating"], "definitions.Rating must exist")
    }

    @Test
    fun toolPromptFragmentMentionsRating() {
        assertTrue(
            "Rating" in CustomDemoCatalog.ToolPromptFragment,
            "ToolPromptFragment should mention Rating",
        )
    }
}
