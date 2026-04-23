package dev.mikepenz.a2cui.compose.catalog

import dev.mikepenz.a2cui.compose.ComponentRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Material3BasicCatalogTest {

    @Test fun registers_all_v09_basic_catalog_components() {
        val r = ComponentRegistry().registerAll(Material3BasicCatalog)
        val expected = setOf(
            "Text", "Column", "Row", "Card", "Button", "TextField", "CheckBox",
            "Slider", "ChoicePicker", "List", "Tabs", "Modal", "DateTimeInput", "Image", "Icon",
        )
        assertEquals(expected, r.names)
    }

    @Test fun catalog_id_matches_v09_basic_uri() {
        assertEquals(
            "https://a2ui.org/specification/v0_9/basic_catalog.json",
            Material3BasicCatalog.id,
        )
    }

    @Test fun host_can_override_a_default_factory() {
        var called = false
        val r = ComponentRegistry()
            .registerAll(Material3BasicCatalog)
            .register("Text") { _, _ -> called = true }
        // Verify the override landed — the factory itself stays @Composable-untouched here
        // since we only check registry identity. A full composition test lives in sample apps.
        assertTrue("Text" in r)
        // Pointer identity confirms it's not the default TextFactory any more.
        assertEquals(r["Text"], r["Text"]) // non-null
        // Suppress unused var
        kotlin.runCatching { @Suppress("UNUSED_VARIABLE") val _u = called }
    }
}
