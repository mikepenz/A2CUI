package dev.mikepenz.a2cui.codegen

import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

class CatalogGeneratorTest {

    @Test
    fun emitsPackageClassAndRegistrations() {
        val spec = ComponentSpec(
            name = "Rating",
            description = "5-star",
            factoryReference = "com.example.RatingFactory",
            props = emptyList(),
            events = emptyList(),
            slots = emptyList(),
        )
        val code = CatalogGenerator.generate(
            packageName = "a2cui.generated",
            className = "GeneratedA2cuiCatalog",
            catalogId = "my-catalog",
            specs = listOf(spec),
            schema = buildJsonObject { },
            promptFragment = "# Prompt\n\nuse it",
        )
        assertTrue("package a2cui.generated" in code)
        assertTrue("public object GeneratedA2cuiCatalog : Catalog" in code)
        assertTrue("override val id: String = \"my-catalog\"" in code)
        assertTrue("registry.register(\"Rating\", com.example.RatingFactory)" in code)
        assertTrue("override fun toJsonSchema()" in code)
        assertTrue("ToolPromptFragment" in code)
    }

    @Test
    fun escapesEmbeddedTripleQuotes() {
        val spec = ComponentSpec("X", "", "a.X", emptyList(), emptyList(), emptyList())
        val code = CatalogGenerator.generate(
            packageName = "x",
            className = "X",
            catalogId = "c",
            specs = listOf(spec),
            schema = buildJsonObject { },
            promptFragment = "nasty: \"\"\" in text",
        )
        // Raw literal must survive without prematurely closing.
        assertTrue(code.contains("\"\"\${'\"'}"))
    }
}
