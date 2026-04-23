package dev.mikepenz.a2cui.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single node in an A2UI surface. Nodes form a flat adjacency list; parent/child relationships
 * are expressed through [children] (a list of node ids), not by nesting. Exactly one node in a
 * surface has `id == "root"`.
 *
 * Component-specific properties (e.g. `text`, `variant` for `Text`) are preserved verbatim in
 * [properties] so catalog implementations can decode them per-component.
 *
 * @see <a href="https://a2ui.org/specification/v0.9-a2ui/">A2UI v0.9 specification</a>
 */
@Serializable(with = ComponentNodeSerializer::class)
public data class ComponentNode(
    val id: String,
    val component: String,
    val children: List<String> = emptyList(),
    /** All remaining JSON fields beyond `id` / `component` / `children`. */
    val properties: JsonObject = JsonObject(emptyMap()),
)

internal object ComponentNodeSerializer : KSerializer<ComponentNode> {
    private val reservedKeys = setOf("id", "component", "children")

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("A2uiComponentNode") {
        element<String>("id")
        element<String>("component")
        element("children", kotlinx.serialization.builtins.ListSerializer(String.serializer()).descriptor, isOptional = true)
        element("properties", JsonObject.serializer().descriptor, isOptional = true)
    }

    override fun deserialize(decoder: Decoder): ComponentNode {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("ComponentNode can only be deserialized from JSON")
        val obj = jsonDecoder.decodeJsonElement().jsonObject

        val id = obj["id"]?.jsonPrimitive?.content
            ?: error("ComponentNode requires 'id'")
        val component = obj["component"]?.jsonPrimitive?.content
            ?: error("ComponentNode requires 'component'")
        val children = obj["children"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val props = buildJsonObject {
            obj.forEach { (k, v) -> if (k !in reservedKeys) put(k, v) }
        }
        return ComponentNode(id = id, component = component, children = children, properties = props)
    }

    override fun serialize(encoder: Encoder, value: ComponentNode) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("ComponentNode can only be serialized to JSON")
        val merged = buildJsonObject {
            put("id", JsonPrimitive(value.id))
            put("component", JsonPrimitive(value.component))
            if (value.children.isNotEmpty()) {
                put("children", kotlinx.serialization.json.JsonArray(value.children.map(::JsonPrimitive)))
            }
            value.properties.forEach { (k, v) -> if (k !in reservedKeys) put(k, v) }
        }
        jsonEncoder.encodeJsonElement(merged as JsonElement)
    }
}
