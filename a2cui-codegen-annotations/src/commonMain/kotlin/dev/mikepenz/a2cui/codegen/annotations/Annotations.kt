package dev.mikepenz.a2cui.codegen.annotations

/**
 * Marks a property of type `ComponentFactory` (or a zero-arg factory function returning one) as
 * an A2UI component. The `:a2cui-codegen` KSP processor collects every annotated symbol and
 * emits (a) a [Catalog] implementation that registers each factory, (b) a JSON Schema fragment
 * per component derived from [props] / [events] / [slots], and (c) a markdown tool-prompt
 * fragment surfaced to the agent's system prompt so the LLM is constrained to this vocabulary.
 *
 * @property name The A2UI component name as it appears in `ComponentNode.component` on the wire.
 * @property description Human-readable description, rendered into the prompt fragment.
 * @property props Typed prop metadata; each one becomes a JSON Schema property.
 * @property events Outbound event metadata, one entry per declared `action.event.name`.
 * @property slots Named child slots; by default every component takes a single `children` slot.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class A2uiComponent(
    val name: String,
    val description: String = "",
    val props: Array<A2uiProp> = [],
    val events: Array<A2uiEvent> = [],
    val slots: Array<A2uiSlot> = [],
)

/**
 * Typed metadata for a single A2UI property on an [A2uiComponent]. Consumers declare props here
 * rather than on Kotlin parameters because a `ComponentFactory` reads untyped [JsonObject]
 * `properties` at runtime — the annotation is the one place where type information lives.
 *
 * @property name Wire JSON key.
 * @property type JSON Schema primitive.
 * @property description Human-readable description.
 * @property required If true, the agent must supply this field.
 * @property defaultValue Printed into the JSON Schema as the `default` field (string-encoded).
 * @property enumValues Optional closed set of allowed values; renders as `enum` in JSON Schema.
 */
@Target()
@Retention(AnnotationRetention.BINARY)
public annotation class A2uiProp(
    val name: String,
    val type: A2uiPropType = A2uiPropType.STRING,
    val description: String = "",
    val required: Boolean = false,
    val defaultValue: String = "",
    val enumValues: Array<String> = [],
)

/** JSON Schema primitive types supported by [A2uiProp]. */
public enum class A2uiPropType {
    STRING,
    INTEGER,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY,
}

/**
 * Outbound event declaration. Matches an `action.event.name` value emitted by the component at
 * runtime. [context] describes the shape of the event's payload so the agent can decode it.
 */
@Target()
@Retention(AnnotationRetention.BINARY)
public annotation class A2uiEvent(
    val name: String,
    val description: String = "",
    val context: Array<A2uiProp> = [],
)

/**
 * Named child slot on an [A2uiComponent]. Most components declare no slots and use the default
 * `children` list; components with multiple slots (e.g. `Tabs` with distinct tab + content
 * slots) declare one per slot.
 *
 * @property name Slot identifier — becomes a key in the generated schema.
 * @property description Human-readable description.
 * @property multiple If true, the slot accepts a list of children; otherwise a single child.
 */
@Target()
@Retention(AnnotationRetention.BINARY)
public annotation class A2uiSlot(
    val name: String,
    val description: String = "",
    val multiple: Boolean = true,
)
