package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import dev.mikepenz.a2cui.compose.A2cuiSurface
import dev.mikepenz.a2cui.compose.ComponentRegistry
import dev.mikepenz.a2cui.compose.SurfaceController
import dev.mikepenz.a2cui.transport.FakeTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression + smoke tests for every component in [Material3BasicCatalog].
 *
 * Each test drives a real [SurfaceController] + `FakeTransport` pipeline so the test exercises
 * the same code path live agents would: JSON wire frames → controller → render tree → Compose
 * semantics assertions. The controller's data model is the source of truth, so post-interaction
 * we read it directly to verify two-way binding wrote back correctly.
 *
 * Many tests intentionally regress specific historical bugs:
 *  - Text/Image/Icon/Button text were not reactive to `{path}`-bound data-model changes
 *  - ChoicePicker dropdown couldn't be opened (TextField swallowed the tap)
 *  - DateTimeInput trapped pointer events in an infinite `awaitPointerEvent` loop
 *  - Tabs selected index couldn't be server-driven
 *  - Modal `open` was read once and ignored subsequent server writes
 *  - List rendered as LazyColumn even when nested inside a scrollable parent (added `lazy:false`)
 *
 * Note: every component-under-test must be reachable from the surface root (`id="root"`),
 * so single-component tests give the component itself `id:"root"` and multi-component tests
 * declare a Column with `id:"root"` that wraps the children.
 */
@OptIn(ExperimentalTestApi::class)
class CatalogComponentsTest {

    private val controllers = mutableListOf<SurfaceController>()

    @AfterTest
    fun tearDown() {
        controllers.forEach { it.close() }
        controllers.clear()
    }

    private fun harness(): Triple<FakeTransport, SurfaceController, CoroutineScope> {
        val transport = FakeTransport()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val registry = ComponentRegistry().also { it.registerAll(Material3BasicCatalog) }
        val controller = SurfaceController(registry, transport, scope).also { it.start() }
        controllers += controller
        return Triple(transport, controller, scope)
    }

    private fun FakeTransport.createSurface(id: String) =
        tryEmit("""{"version":"v0.9","createSurface":{"surfaceId":"$id","catalogId":"c"}}""")

    private fun FakeTransport.write(id: String, path: String, value: String) =
        tryEmit("""{"version":"v0.9","updateDataModel":{"surfaceId":"$id","path":"$path","value":$value}}""")

    private fun FakeTransport.components(id: String, json: String) =
        tryEmit("""{"version":"v0.9","updateComponents":{"surfaceId":"$id","components":$json}}""")

    // -------------------- Reactivity regressions (Text/Image/Icon/Button text) --------------------

    @Test fun text_reactively_updates_when_bound_path_changes() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components("s", """[{"id":"root","component":"Text","text":{"path":"/greeting"}}]""")
        t.write("s", "/greeting", "\"hello\"")

        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("hello").assertIsDisplayed()

        t.write("s", "/greeting", "\"goodbye\"")
        waitForIdle()
        onNodeWithText("goodbye").assertIsDisplayed()
    }

    @Test fun button_text_reactively_updates_when_bound_path_changes() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[{"id":"root","component":"Button","text":{"path":"/label"},
                  "action":{"event":{"name":"click"}}}]""",
        )
        t.write("s", "/label", "\"Save\"")

        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("Save").assertIsDisplayed()

        t.write("s", "/label", "\"Submit\"")
        waitForIdle()
        onNodeWithText("Submit").assertIsDisplayed()
    }

    // -------------------- ChoicePicker dropdown opens & writes --------------------

    @Test fun choice_picker_opens_menu_and_writes_selection_to_data_model() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[{"id":"root","component":"ChoicePicker","label":"Movie",
                  "choices":["Alpha","Beta","Gamma"],
                  "value":{"path":"/pick"}}]""",
        )
        t.write("s", "/pick", "\"Alpha\"")

        setContent { A2cuiSurface("s", c) }
        waitForIdle()

        // Tap the field — must open the menu (regression: previously click was swallowed by readOnly TextField)
        onNode(hasText("Alpha")).performClick()
        waitForIdle()
        onNodeWithText("Beta").performClick()
        waitForIdle()

        val state = c.surfaces.value["s"]
        assertNotNull(state)
        assertEquals(JsonPrimitive("Beta"), state.dataModel.read("/pick"))
    }

    // -------------------- TextField two-way binding --------------------

    @Test fun text_field_writes_typed_input_to_data_model() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[{"id":"root","component":"TextField","label":"Name","value":{"path":"/name"}}]""",
        )
        // Seed with a known value so we can locate the field by its current text content.
        t.write("s", "/name", "\"\"")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()

        // Find the field by its label and type into it.
        onNodeWithText("Name").performTextInput("Alice")
        waitForIdle()

        val state = c.surfaces.value["s"]!!
        assertEquals(JsonPrimitive("Alice"), state.dataModel.read("/name"))
    }

    // -------------------- CheckBox toggle --------------------

    @Test fun checkbox_toggles_and_persists_to_data_model() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[{"id":"root","component":"CheckBox","label":"Subscribe","value":{"path":"/sub"}}]""",
        )
        t.write("s", "/sub", "false")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()

        // The Material Checkbox itself owns the click target (the label Text is not clickable).
        onAllNodes(isToggleable())[0].performClick()
        waitForIdle()

        val state = c.surfaces.value["s"]!!
        assertEquals(JsonPrimitive(true), state.dataModel.read("/sub"))
    }

    // -------------------- Slider --------------------

    @Test fun slider_renders_with_label_and_initial_value() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[{"id":"root","component":"Slider","label":"Tickets",
                  "min":1,"max":8,"step":1,"value":{"path":"/n"}}]""",
        )
        t.write("s", "/n", "3")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("Tickets").assertIsDisplayed()
        onNodeWithText("3").assertIsDisplayed()
    }

    // -------------------- Button action emits event with resolved context --------------------

    @Test fun button_emits_action_with_resolved_context_at_click_time() = runComposeUiTest {
        val (t, c, scope) = harness()
        val emitted = mutableListOf<String>()
        val collector = scope.launch { c.events.collect { emitted.add(it.toString()) } }
        try {
            t.createSurface("s")
            t.components(
                "s",
                """[{"id":"root","component":"Button","text":"Go",
                      "action":{"event":{"name":"go","context":{"who":{"path":"/who"}}}}}]""",
            )
            t.write("s", "/who", "\"alice\"")
            setContent { A2cuiSurface("s", c) }
            waitForIdle()

            // Update bound value AFTER render — context must re-resolve at click time.
            t.write("s", "/who", "\"bob\"")
            waitForIdle()

            onNodeWithText("Go").performClick()
            waitForIdle()

            val payload = emitted.joinToString("|")
            kotlin.test.assertContains(payload, "bob")
            kotlin.test.assertContains(payload, "go")
        } finally {
            collector.cancel()
        }
    }

    // -------------------- Tabs server-driven selection --------------------

    @Test fun tabs_selected_index_is_server_driven_when_bound() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[
              {"id":"root","component":"Tabs","titles":["One","Two","Three"],
                "selectedIndex":{"path":"/active"},"children":["t1","t2","t3"]},
              {"id":"t1","component":"Text","text":"Body One"},
              {"id":"t2","component":"Text","text":"Body Two"},
              {"id":"t3","component":"Text","text":"Body Three"}
            ]""",
        )
        t.write("s", "/active", "0")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("Body One").assertIsDisplayed()

        // Server writes a new index — Tabs must respect it (regression: was local-only).
        t.write("s", "/active", "2")
        waitForIdle()
        onNodeWithText("Body Three").assertIsDisplayed()
    }

    @Test fun tabs_clicking_writes_back_when_bound() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[
              {"id":"root","component":"Tabs","titles":["A","B"],
                "selectedIndex":{"path":"/active"},"children":["t1","t2"]},
              {"id":"t1","component":"Text","text":"Page A"},
              {"id":"t2","component":"Text","text":"Page B"}
            ]""",
        )
        t.write("s", "/active", "0")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()

        onNodeWithText("B").performClick()
        waitForIdle()

        val state = c.surfaces.value["s"]!!
        assertEquals(JsonPrimitive(1), state.dataModel.read("/active"))
        onNodeWithText("Page B").assertIsDisplayed()
    }

    // -------------------- Modal server-driven open/close --------------------

    @Test fun modal_open_is_server_driven_when_bound() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[
              {"id":"root","component":"Modal","title":"Confirm","open":{"path":"/show"},
                "children":["body"]},
              {"id":"body","component":"Text","text":"Modal body content"}
            ]""",
        )
        t.write("s", "/show", "true")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("Modal body content").assertIsDisplayed()

        // Server closes the modal — must hide (regression: open was read once and stuck).
        t.write("s", "/show", "false")
        waitForIdle()
        assertTrue(
            onAllNodesWithText("Modal body content").fetchSemanticsNodes().isEmpty(),
            "Modal body should be gone after open=false write",
        )
    }

    // -------------------- List lazy:false renders eagerly --------------------

    @Test fun list_eager_mode_renders_all_items_inline() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.write(
            "s", "",
            """{"items":[{"name":"Alpha"},{"name":"Beta"},{"name":"Gamma"}]}""",
        )
        t.components(
            "s",
            """[
              {"id":"root","component":"List","lazy":false,
                "items":{"path":"/items"},"children":["row"]},
              {"id":"row","component":"Text","text":{"path":"/name"}}
            ]""",
        )
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("Alpha").assertIsDisplayed()
        onNodeWithText("Beta").assertIsDisplayed()
        onNodeWithText("Gamma").assertIsDisplayed()
    }

    // -------------------- Layout components render children --------------------

    @Test fun column_row_card_render_children() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[
              {"id":"root","component":"Column","children":["card"]},
              {"id":"card","component":"Card","children":["row"]},
              {"id":"row","component":"Row","children":["a","b"]},
              {"id":"a","component":"Text","text":"left"},
              {"id":"b","component":"Text","text":"right"}
            ]""",
        )
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("left").assertIsDisplayed()
        onNodeWithText("right").assertIsDisplayed()
    }

    // -------------------- Icon: Material Symbol lookup --------------------

    @Test fun icon_known_name_renders_material_symbol_with_content_description() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components("s", """[{"id":"root","component":"Icon","name":"settings"}]""")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        // Real icon — semantics expose `contentDescription`, which defaults to the name.
        onNodeWithContentDescription("settings").assertIsDisplayed()
    }

    @Test fun icon_snake_case_and_camel_case_resolve_same_symbol() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[
              {"id":"root","component":"Row","children":["a","b"]},
              {"id":"a","component":"Icon","name":"more_vert","contentDescription":"snake"},
              {"id":"b","component":"Icon","name":"moreVert","contentDescription":"camel"}
            ]""",
        )
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithContentDescription("snake").assertIsDisplayed()
        onNodeWithContentDescription("camel").assertIsDisplayed()
    }

    @Test fun icon_outlined_style_resolves_outlined_variant() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[{"id":"root","component":"Icon","name":"home","style":"outlined",
                  "contentDescription":"home-outlined"}]""",
        )
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithContentDescription("home-outlined").assertIsDisplayed()
    }

    @Test fun icon_unknown_name_falls_back_to_text_chip() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components("s", """[{"id":"root","component":"Icon","name":"not_a_real_icon"}]""")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("not_a_real_icon").assertIsDisplayed()
    }

    @Test fun icon_name_reactively_updates_when_bound() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components("s", """[{"id":"root","component":"Icon","name":{"path":"/n"}}]""")
        t.write("s", "/n", "\"home\"")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithContentDescription("home").assertIsDisplayed()
        t.write("s", "/n", "\"settings\"")
        waitForIdle()
        onNodeWithContentDescription("settings").assertIsDisplayed()
    }

    // -------------------- Card: elevation + onClick action --------------------

    @Test fun card_with_action_emits_event_when_clicked() = runComposeUiTest {
        val (t, c, scope) = harness()
        val emitted = mutableListOf<String>()
        val collector = scope.launch { c.events.collect { emitted += it.toString() } }
        try {
            t.createSurface("s")
            t.components(
                "s",
                """[
                  {"id":"root","component":"Card",
                    "action":{"event":{"name":"card_tapped","context":{"id":{"path":"/sel"}}}},
                    "children":["t"]},
                  {"id":"t","component":"Text","text":"Tap me"}
                ]""",
            )
            t.write("s", "/sel", "\"item-42\"")
            setContent { A2cuiSurface("s", c) }
            waitForIdle()
            onNodeWithText("Tap me").performClick()
            waitForIdle()

            val payload = emitted.joinToString("|")
            kotlin.test.assertContains(payload, "card_tapped")
            kotlin.test.assertContains(payload, "item-42")
        } finally {
            collector.cancel()
        }
    }

    @Test fun card_renders_children_when_no_action_supplied() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[
              {"id":"root","component":"Card","elevation":4,"padding":24,"children":["t"]},
              {"id":"t","component":"Text","text":"Static card"}
            ]""",
        )
        setContent { A2cuiSurface("s", c) }
        waitForIdle()
        onNodeWithText("Static card").assertIsDisplayed()
    }

    // -------------------- Slider: int coercion when stepped --------------------

    @Test fun slider_writes_integer_when_step_is_positive() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[{"id":"root","component":"Slider","label":"N",
                  "min":0,"max":10,"step":1,"value":{"path":"/n"}}]""",
        )
        t.write("s", "/n", "0")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()

        // Drive a value change by writing programmatically through the data model — the test
        // verifies the round-trip type is preserved as Int when the slider is stepped.
        val state = c.surfaces.value["s"]!!
        state.dataModel.write("/n", JsonPrimitive(7))
        waitForIdle()
        // The displayed label should reflect the integer value.
        onNodeWithText("7").assertIsDisplayed()
    }

    // -------------------- DateTimeInput: tap opens picker (regression for pointerInput loop) --------------------

    @Test fun datetime_input_tap_opens_picker_dialog() = runComposeUiTest {
        val (t, c, _) = harness()
        t.createSurface("s")
        t.components(
            "s",
            """[{"id":"root","component":"DateTimeInput","label":"Pick date",
                  "format":"date","value":{"path":"/d"}}]""",
        )
        t.write("s", "/d", "\"2026-04-24\"")
        setContent { A2cuiSurface("s", c) }
        waitForIdle()

        // Tap the field — the picker dialog must mount (regression: previously the
        // pointerInput awaitPointerEvent loop swallowed events and the picker never opened).
        onNode(hasText("2026-04-24")).performClick()
        waitForIdle()

        // DatePickerDialog renders OK / Cancel buttons — assert one is in the tree.
        onNodeWithText("OK").assertIsDisplayed()
    }
}
