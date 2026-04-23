package dev.mikepenz.a2cui.compose.catalog

import dev.mikepenz.a2cui.compose.Catalog
import dev.mikepenz.a2cui.compose.ComponentRegistry

/**
 * Implementation of the A2UI v0.9 "basic catalog" atop Material 3.
 *
 * Covers all 15 component types from the spec: Text, Image, Icon, Row, Column, List, Card,
 * Button, CheckBox, TextField, DateTimeInput, ChoicePicker, Slider, Modal, Tabs.
 *
 * Registration is additive: apps may `register("Text") { ... }` after `registerAll(this)` to
 * override any default. The catalog id matches the URI advertised by the A2UI spec so
 * agent-side prompts can pin to this vocabulary.
 */
public object Material3BasicCatalog : Catalog {

    public override val id: String =
        "https://a2ui.org/specification/v0_9/basic_catalog.json"

    public override fun install(registry: ComponentRegistry) {
        registry.register("Text", TextFactory)
        registry.register("Column", ColumnFactory)
        registry.register("Row", RowFactory)
        registry.register("Card", CardFactory)
        registry.register("Button", ButtonFactory)
        registry.register("TextField", TextFieldFactory)
        registry.register("CheckBox", CheckBoxFactory)
        registry.register("Slider", SliderFactory)
        registry.register("ChoicePicker", ChoicePickerFactory)
        registry.register("List", ListFactory)
        registry.register("Tabs", TabsFactory)
        registry.register("Modal", ModalFactory)
        registry.register("DateTimeInput", DateTimeInputFactory)
        registry.register("Image", ImageFactory)
        registry.register("Icon", IconFactory)
    }
}
