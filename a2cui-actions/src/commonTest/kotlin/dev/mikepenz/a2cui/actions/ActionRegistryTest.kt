package dev.mikepenz.a2cui.actions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionRegistryTest {

    @Test fun register_and_unregister_round_trip() {
        val r = ActionRegistry()
        val reg = ActionRegistration(
            descriptor = ActionDescriptor("foo", "desc"),
            render = { _ -> },
        )
        r.register(reg)
        assertEquals(reg, r["foo"])
        assertEquals(listOf("foo"), r.descriptors().map { it.name })
        r.unregister("foo")
        assertNull(r["foo"])
        assertTrue(r.descriptors().isEmpty())
    }

    @Test fun entries_stream_reflects_changes() {
        val r = ActionRegistry()
        assertTrue(r.entries.value.isEmpty())
        r.register(
            ActionRegistration(
                descriptor = ActionDescriptor("a", "x"),
                render = { _ -> },
            ),
        )
        assertEquals(setOf("a"), r.entries.value.keys)
    }
}
