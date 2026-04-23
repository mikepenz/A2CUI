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
}
