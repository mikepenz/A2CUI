package dev.mikepenz.a2cui.codegen

/**
 * In-memory model of one `@A2uiComponent`-annotated symbol. Pure data — no KSP types — so the
 * generators ([SchemaGenerator], [PromptGenerator], [CatalogGenerator]) can be unit-tested by
 * constructing specs directly.
 */
internal data class ComponentSpec(
    val name: String,
    val description: String,
    /** FQN of the `ComponentFactory`-typed property to reference from the generated catalog. */
    val factoryReference: String,
    val props: List<PropSpec>,
    val events: List<EventSpec>,
    val slots: List<SlotSpec>,
)

internal data class PropSpec(
    val name: String,
    val type: PropType,
    val description: String,
    val required: Boolean,
    val defaultValue: String,
    val enumValues: List<String>,
)

internal enum class PropType(val schema: String) {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    OBJECT("object"),
    ARRAY("array"),
}

internal data class EventSpec(
    val name: String,
    val description: String,
    val context: List<PropSpec>,
)

internal data class SlotSpec(
    val name: String,
    val description: String,
    val multiple: Boolean,
)
