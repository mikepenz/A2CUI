package dev.mikepenz.a2cui.codegen

/**
 * Renders a compact markdown description of the registered catalog suitable for embedding in
 * an agent's system prompt. The LLM uses this to avoid inventing component types outside the
 * closed vocabulary.
 */
internal object PromptGenerator {

    fun generate(catalogId: String, specs: List<ComponentSpec>): String = buildString {
        appendLine("# A2UI Catalog")
        appendLine()
        appendLine("Catalog id: `$catalogId`")
        appendLine()
        appendLine("You may only emit components of the following types. Every component node")
        appendLine("must have `id` (unique string) and `component` (one of the types below).")
        appendLine("Nest children by listing child node ids in the `children` field.")
        appendLine()
        specs.sortedBy { it.name }.forEach { spec -> appendComponent(spec) }
    }

    private fun StringBuilder.appendComponent(spec: ComponentSpec) {
        appendLine("## ${spec.name}")
        if (spec.description.isNotEmpty()) {
            appendLine()
            appendLine(spec.description)
        }
        if (spec.props.isNotEmpty()) {
            appendLine()
            appendLine("**Props:**")
            spec.props.forEach { prop ->
                val required = if (prop.required) " (required)" else ""
                val default = if (prop.defaultValue.isNotEmpty()) ", default `${prop.defaultValue}`" else ""
                val enum = if (prop.enumValues.isNotEmpty()) ", one of ${prop.enumValues.joinToString(", ") { "`$it`" }}" else ""
                val desc = if (prop.description.isNotEmpty()) " — ${prop.description}" else ""
                appendLine("- `${prop.name}` (${prop.type.schema}$default$enum)$required$desc")
            }
        }
        if (spec.events.isNotEmpty()) {
            appendLine()
            appendLine("**Events:**")
            spec.events.forEach { event ->
                val desc = if (event.description.isNotEmpty()) " — ${event.description}" else ""
                appendLine("- `${event.name}`$desc")
                event.context.forEach { ctx ->
                    appendLine("  - context `${ctx.name}` (${ctx.type.schema}): ${ctx.description}")
                }
            }
        }
        if (spec.slots.isNotEmpty()) {
            appendLine()
            appendLine("**Slots:**")
            spec.slots.forEach { slot ->
                val many = if (slot.multiple) "list" else "single"
                val desc = if (slot.description.isNotEmpty()) " — ${slot.description}" else ""
                appendLine("- `${slot.name}` ($many)$desc")
            }
        }
        appendLine()
    }
}
