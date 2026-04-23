package dev.mikepenz.a2cui.codegen

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Converts a list of [ComponentSpec]s into a JSON Schema document the agent uses to constrain
 * its component output. The resulting object has a `definitions` entry per component and a
 * top-level `oneOf` union over all of them.
 */
internal object SchemaGenerator {

    fun generate(catalogId: String, specs: List<ComponentSpec>): JsonObject = buildJsonObject {
        put("\$schema", JsonPrimitive("http://json-schema.org/draft-07/schema#"))
        put("\$id", JsonPrimitive(catalogId))
        put("title", JsonPrimitive("A2UI catalog"))
        put("description", JsonPrimitive("Components this client can render."))
        put("definitions", buildJsonObject {
            specs.forEach { spec -> put(spec.name, componentSchema(spec)) }
        })
        put("oneOf", buildJsonArray {
            specs.forEach { spec ->
                add(buildJsonObject { put("\$ref", JsonPrimitive("#/definitions/${spec.name}")) })
            }
        })
    }

    private fun componentSchema(spec: ComponentSpec): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        if (spec.description.isNotEmpty()) put("description", JsonPrimitive(spec.description))

        val required = mutableListOf<String>("id", "component")
        spec.props.filter { it.required }.forEach { required += it.name }

        put("properties", buildJsonObject {
            put("id", buildJsonObject { put("type", JsonPrimitive("string")) })
            put("component", buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("const", JsonPrimitive(spec.name))
            })
            if (spec.slots.isEmpty()) {
                put("children", buildJsonObject {
                    put("type", JsonPrimitive("array"))
                    put("items", buildJsonObject { put("type", JsonPrimitive("string")) })
                    put("description", JsonPrimitive("Child node ids."))
                })
            } else {
                spec.slots.forEach { slot -> put(slot.name, slotSchema(slot)) }
            }
            spec.props.forEach { prop -> put(prop.name, propSchema(prop)) }
            if (spec.events.isNotEmpty()) {
                put("action", buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("description", JsonPrimitive("Outbound event bound to this component."))
                    put("properties", buildJsonObject {
                        put("event", buildJsonObject {
                            put("type", JsonPrimitive("object"))
                            put("properties", buildJsonObject {
                                put("name", buildJsonObject {
                                    put("type", JsonPrimitive("string"))
                                    put("enum", buildJsonArray {
                                        spec.events.forEach { add(JsonPrimitive(it.name)) }
                                    })
                                })
                                put("context", buildJsonObject { put("type", JsonPrimitive("object")) })
                            })
                        })
                    })
                })
            }
        })
        put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun propSchema(prop: PropSpec): JsonObject = buildJsonObject {
        put("type", JsonPrimitive(prop.type.schema))
        if (prop.description.isNotEmpty()) put("description", JsonPrimitive(prop.description))
        if (prop.defaultValue.isNotEmpty()) {
            put("default", defaultLiteral(prop.type, prop.defaultValue))
        }
        if (prop.enumValues.isNotEmpty()) {
            put("enum", buildJsonArray { prop.enumValues.forEach { add(JsonPrimitive(it)) } })
        }
    }

    private fun slotSchema(slot: SlotSpec): JsonObject = buildJsonObject {
        if (slot.multiple) {
            put("type", JsonPrimitive("array"))
            put("items", buildJsonObject { put("type", JsonPrimitive("string")) })
        } else {
            put("type", JsonPrimitive("string"))
        }
        if (slot.description.isNotEmpty()) put("description", JsonPrimitive(slot.description))
    }

    private fun defaultLiteral(type: PropType, raw: String): JsonElement = when (type) {
        PropType.INTEGER -> raw.toLongOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(raw)
        PropType.NUMBER -> raw.toDoubleOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(raw)
        PropType.BOOLEAN -> raw.toBooleanStrictOrNull()?.let { JsonPrimitive(it) } ?: JsonPrimitive(raw)
        PropType.ARRAY -> JsonArray(emptyList())
        PropType.OBJECT -> JsonObject(emptyMap())
        PropType.STRING -> JsonPrimitive(raw)
    }
}
