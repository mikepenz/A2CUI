package dev.mikepenz.a2cui.core

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class BindingResolverTest {

    @Test fun literal_passes_through() {
        val r = BindingResolver(DataModel())
        assertEquals(
            "hi",
            (r.resolve(JsonPrimitive("hi")) as JsonPrimitive).content,
        )
    }

    @Test fun path_reads_from_data_model() {
        val model = DataModel()
        model.write("/user/name", JsonPrimitive("Ada"))
        val r = BindingResolver(model)
        val raw = buildJsonObject { put("path", JsonPrimitive("/user/name")) }
        assertEquals("Ada", (r.resolve(raw) as JsonPrimitive).content)
    }

    @Test fun path_missing_resolves_to_null() {
        val r = BindingResolver(DataModel())
        val raw = buildJsonObject { put("path", JsonPrimitive("/missing")) }
        assertEquals(JsonNull, r.resolve(raw))
    }

    @Test fun call_uses_dispatcher() {
        val r = BindingResolver(
            DataModel(),
            callDispatcher = { fn, args ->
                JsonPrimitive("$fn:${args["x"]?.jsonPrimitive?.content}")
            },
        )
        val raw = buildJsonObject {
            put("call", JsonPrimitive("fmt"))
            put("args", buildJsonObject { put("x", JsonPrimitive("y")) })
        }
        assertEquals("fmt:y", (r.resolve(raw) as JsonPrimitive).content)
    }

    @Test fun resolve_property_returns_null_for_missing_key() {
        val r = BindingResolver(DataModel())
        assertEquals(JsonNull, r.resolveProperty(buildJsonObject {}, "missing"))
    }

    @Test fun scoped_resolver_reads_absolute_path_relative_to_scope_root() {
        val model = DataModel()
        model.write("/users/0/name", JsonPrimitive("Ada"))
        model.write("/users/1/name", JsonPrimitive("Grace"))
        val scoped = BindingResolver(model).withScope("/users/0")
        val raw = buildJsonObject { put("path", JsonPrimitive("/name")) }
        assertEquals("Ada", (scoped.resolve(raw) as JsonPrimitive).content)
    }

    @Test fun scoped_resolver_supports_walk_up() {
        val model = DataModel()
        model.write("/title", JsonPrimitive("Hello"))
        model.write("/users/0/name", JsonPrimitive("Ada"))
        val scoped = BindingResolver(model).withScope("/users/0")
        val raw = buildJsonObject { put("path", JsonPrimitive("../../title")) }
        assertEquals("Hello", (scoped.resolve(raw) as JsonPrimitive).content)
    }

    @Test fun scoped_pointer_composes_root_and_absolute_path() {
        val r = BindingResolver(DataModel()).withScope("/users/0")
        assertEquals("/users/0/name", r.scopedPointer("/name"))
        assertEquals("/users/0", r.scopedPointer(""))
        assertEquals("/users", r.scopedPointer("/.."))
        assertEquals("/title", r.scopedPointer("/../../title"))
    }

    @Test fun scoped_resolver_missing_path_still_returns_null() {
        val r = BindingResolver(DataModel()).withScope("/users/0")
        val raw = buildJsonObject { put("path", JsonPrimitive("/name")) }
        assertEquals(JsonNull, r.resolve(raw))
    }

    @OptIn(ExperimentalA2uiDraft::class)
    @Test fun experimental_conditional_picks_branch_on_boolean_pointer() {
        val model = DataModel()
        model.write("/user/isAdmin", JsonPrimitive(true))
        val r = BindingResolver(model)
        val raw = buildJsonObject {
            put("if", JsonPrimitive("/user/isAdmin"))
            put("then", JsonPrimitive("Admin Panel"))
            put("else", JsonPrimitive("User Home"))
        }
        assertEquals("Admin Panel", (r.resolve(raw) as JsonPrimitive).content)

        model.write("/user/isAdmin", JsonPrimitive(false))
        assertEquals("User Home", (r.resolve(raw) as JsonPrimitive).content)
    }

    @OptIn(ExperimentalA2uiDraft::class)
    @Test fun experimental_conditional_missing_pointer_is_falsy() {
        val r = BindingResolver(DataModel())
        val raw = buildJsonObject {
            put("if", JsonPrimitive("/missing"))
            put("then", JsonPrimitive("yes"))
            put("else", JsonPrimitive("no"))
        }
        assertEquals("no", (r.resolve(raw) as JsonPrimitive).content)
    }

    @OptIn(ExperimentalA2uiDraft::class)
    @Test fun experimental_conditional_branch_resolves_nested_path() {
        val model = DataModel()
        model.write("/flag", JsonPrimitive(true))
        model.write("/userName", JsonPrimitive("Ada"))
        val r = BindingResolver(model)
        val raw = buildJsonObject {
            put("if", JsonPrimitive("/flag"))
            put("then", buildJsonObject { put("path", JsonPrimitive("/userName")) })
            put("else", JsonPrimitive("anonymous"))
        }
        assertEquals("Ada", (r.resolve(raw) as JsonPrimitive).content)
    }
}
