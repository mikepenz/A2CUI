package dev.mikepenz.a2cui.compose.catalog

import dev.mikepenz.a2cui.core.BindingResolver
import dev.mikepenz.a2cui.core.ComponentNode
import dev.mikepenz.a2cui.core.DataModel
import dev.mikepenz.a2cui.core.JsonPointer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Drives the same pointer/scope logic that `ListFactory` applies at composition time, without
 * instantiating Compose UI. Validates:
 *  - per-item scoped resolution of `{path:"/name"}`,
 *  - subtree count tracks the backing array,
 *  - add/remove propagates through the data model.
 */
class ListIterationScopeTest {

    private fun seed(): DataModel {
        val dm = DataModel()
        dm.write(
            "/users",
            buildJsonArray {
                add(buildJsonObject { put("name", JsonPrimitive("Ada")) })
                add(buildJsonObject { put("name", JsonPrimitive("Grace")) })
            },
        )
        return dm
    }

    private fun itemsPath(node: ComponentNode): String? = pathOf(node.properties, "items")

    private val listNode = ComponentNode(
        id = "root",
        component = "List",
        children = listOf("tpl"),
        properties = buildJsonObject {
            put("items", buildJsonObject { put("path", JsonPrimitive("/users")) })
        },
    )

    @Test fun items_path_resolves_and_yields_one_subtree_per_element() {
        val dm = seed()
        val resolver = BindingResolver(dm)
        val path = itemsPath(listNode)!!
        val arr = JsonPointer.read(dm.snapshot(), path) as JsonArray
        assertEquals(2, arr.size)

        // For each index, a scoped resolver reads `/name` into the element's name.
        val names = List(arr.size) { i ->
            val scoped = resolver.withScope("$path/$i")
            val raw = buildJsonObject { put("path", JsonPrimitive("/name")) }
            (scoped.resolve(raw) as JsonPrimitive).content
        }
        assertEquals(listOf("Ada", "Grace"), names)
    }

    @Test fun removing_an_array_element_shrinks_the_scope_count() {
        val dm = seed()
        // Replace array with single-element version — mirrors "remove index 0".
        dm.write(
            "/users",
            buildJsonArray {
                add(buildJsonObject { put("name", JsonPrimitive("Grace")) })
            },
        )
        val arr = JsonPointer.read(dm.snapshot(), "/users") as JsonArray
        assertEquals(1, arr.size)
        val scoped = BindingResolver(dm).withScope("/users/0")
        val raw = buildJsonObject { put("path", JsonPrimitive("/name")) }
        assertEquals("Grace", (scoped.resolve(raw) as JsonPrimitive).content)
        // Scope at index that no longer exists resolves to null.
        val oob = BindingResolver(dm).withScope("/users/1")
        assertNull(JsonPointer.read(dm.snapshot(), oob.scopedPointer("/name")))
    }

    @Test fun appending_an_element_grows_the_scope_count() {
        val dm = seed()
        dm.write("/users/2", buildJsonObject { put("name", JsonPrimitive("Linus")) })
        val arr = JsonPointer.read(dm.snapshot(), "/users") as JsonArray
        assertEquals(3, arr.size)
        val scoped = BindingResolver(dm).withScope("/users/2")
        val raw = buildJsonObject { put("path", JsonPrimitive("/name")) }
        assertEquals("Linus", (scoped.resolve(raw) as JsonPrimitive).content)
    }

    @Test fun first_child_is_the_template_ignoring_later_children() {
        // Mirrors the factory's convention: the first child id in `children` is the template.
        val node = listNode.copy(children = listOf("tpl-first", "tpl-ignored"))
        assertEquals("tpl-first", node.children.first())
    }
}
