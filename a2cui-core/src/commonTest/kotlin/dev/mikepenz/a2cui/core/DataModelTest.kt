package dev.mikepenz.a2cui.core

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DataModelTest {

    @Test fun read_write_roundtrip() {
        val model = DataModel()
        model.write("/form/email", JsonPrimitive("u@x"))
        assertEquals("u@x", model.read("/form/email")?.jsonPrimitive?.content)
    }

    @Test fun read_missing_returns_null() {
        val model = DataModel()
        assertNull(model.read("/nope"))
    }

    @Test fun observe_fires_on_write() = runTest {
        val model = DataModel(buildJsonObject { put("count", JsonPrimitive(0)) })
        model.observe("/count").test {
            assertEquals("0", (awaitItem() as JsonPrimitive).content)
            model.write("/count", JsonPrimitive(1))
            assertEquals("1", (awaitItem() as JsonPrimitive).content)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun observe_deduplicates_unchanged_writes() = runTest {
        val model = DataModel(buildJsonObject { put("n", JsonPrimitive("hi")) })
        model.observe("/n").test {
            assertEquals("hi", (awaitItem() as JsonPrimitive).content)
            model.write("/n", JsonPrimitive("hi"))            // no-op
            model.write("/other", JsonPrimitive("zzz"))        // unrelated
            expectNoEvents()
            model.write("/n", JsonPrimitive("bye"))
            assertEquals("bye", (awaitItem() as JsonPrimitive).content)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun observe_missing_path_emits_null_then_value_on_create() = runTest {
        val model = DataModel()
        model.observe("/lazy/field").test {
            assertNull(awaitItem())
            model.write("/lazy/field", JsonPrimitive(42))
            assertEquals("42", (awaitItem() as JsonPrimitive).content)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun snapshot_reflects_latest_writes() {
        val model = DataModel()
        model.write("/a", JsonPrimitive(1))
        val snap = assertIs<JsonObject>(model.snapshot())
        assertEquals("1", snap["a"]?.jsonPrimitive?.content)
    }
}
