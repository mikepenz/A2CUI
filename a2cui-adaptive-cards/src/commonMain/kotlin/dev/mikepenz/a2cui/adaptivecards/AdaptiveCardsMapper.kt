package dev.mikepenz.a2cui.adaptivecards

import dev.mikepenz.a2cui.core.A2uiFrame
import dev.mikepenz.a2cui.core.ComponentNode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mapper from Adaptive Cards v1.6 JSON to an A2UI v0.9 `UpdateComponents` frame.
 *
 * Why: Copilot Studio, Microsoft 365 Agents, and Bot Framework all emit Adaptive Cards as
 * their structured-UI format. Accepting Adaptive Cards verbatim lets those agents render
 * through A2CUI without additional work on the agent side. The trade-off is lossy: Adaptive
 * Cards' richer action layer (`Action.OpenUrl`, `Action.ShowCard`, `Action.ToggleVisibility`)
 * is flattened into A2UI's simpler event model, and styling hints beyond Material's palette
 * are dropped on the floor.
 *
 * The mapping table:
 *  - `Container` → `Column`
 *  - `ColumnSet` → `Row` (sibling Columns collapse into children; widths ignored)
 *  - `TextBlock` → `Text` (wrap / weight / size mapped to our `variant`)
 *  - `Image` → `Image` (url → src)
 *  - `Input.Text` → `TextField` (id → path pointer `/inputs/<id>`)
 *  - `Input.Number` → `TextField` (number variant; bound same way)
 *  - `Input.Toggle` → `CheckBox`
 *  - `Input.ChoiceSet` → `ChoicePicker`
 *  - `Input.Date` / `Input.Time` → `DateTimeInput`
 *  - `ActionSet` / top-level `actions` → `Row` of `Button`s
 *  - `Action.Submit` → button `action.event.name = "submit"`; submit data bundled as context.
 *  - `Action.OpenUrl` → button event `name = "openUrl"`, context `{url}`.
 *  - `Action.ShowCard` / `Action.ToggleVisibility` → dropped with a warning (returned in [result.warnings]).
 *
 * Unknown element types are preserved as A2UI components with the same name, so hosts can
 * register custom factories to handle them if desired.
 */
public object AdaptiveCardsMapper {

    /** Result of a mapping — either the A2UI frame or a list of reasons why it failed. */
    public data class Result(
        val frame: A2uiFrame.UpdateComponents?,
        val warnings: List<String>,
    )

    /**
     * Map an Adaptive Cards v1.x `AdaptiveCard` JSON document to an A2UI `UpdateComponents`
     * frame. The resulting tree has a single `root` node of type `Column`.
     */
    public fun map(card: JsonObject, surfaceId: String = "adaptive-card"): Result {
        if (card["type"]?.jsonPrimitive?.contentOrNull != "AdaptiveCard") {
            return Result(null, listOf("Top-level object is not an AdaptiveCard."))
        }
        val scope = MappingScope()
        val bodyIds = (card["body"] as? JsonArray).orEmptyArray().mapNotNull { scope.visit(it) }
        val actionIds = buildActions(card["actions"] as? JsonArray, scope)

        val rootChildren = bodyIds + actionIds
        scope.nodes.addFirst(
            ComponentNode(
                id = "root",
                component = "Column",
                children = rootChildren,
                properties = buildJsonObject {
                    put("spacing", JsonPrimitive(8))
                },
            ),
        )
        val frame = A2uiFrame.UpdateComponents(
            version = "v0.9",
            updateComponents = A2uiFrame.UpdateComponents.Body(
                surfaceId = surfaceId,
                components = scope.nodes.toList(),
            ),
        )
        return Result(frame, scope.warnings.toList())
    }

    private fun buildActions(actions: JsonArray?, scope: MappingScope): List<String> {
        if (actions.isNullOrEmpty()) return emptyList()
        val buttonIds = actions.mapNotNull { scope.visitAction(it) }
        if (buttonIds.isEmpty()) return emptyList()
        val id = scope.freshId("actions")
        scope.nodes += ComponentNode(
            id = id,
            component = "Row",
            children = buttonIds,
            properties = buildJsonObject { put("spacing", JsonPrimitive(8)) },
        )
        return listOf(id)
    }
}

private class MappingScope {
    val nodes: ArrayDeque<ComponentNode> = ArrayDeque()
    val warnings: MutableList<String> = mutableListOf()
    private var counter = 0

    fun freshId(prefix: String): String {
        counter += 1
        return "$prefix-$counter"
    }

    fun visit(element: JsonElement): String? {
        if (element !is JsonObject) return null
        val type = element["type"]?.jsonPrimitive?.contentOrNull ?: return null
        return when (type) {
            "Container" -> mapContainer(element)
            "ColumnSet" -> mapColumnSet(element)
            "Column" -> mapContainer(element)
            "TextBlock" -> mapTextBlock(element)
            "Image" -> mapImage(element)
            "Input.Text" -> mapInputText(element)
            "Input.Number" -> mapInputText(element, numeric = true)
            "Input.Toggle" -> mapInputToggle(element)
            "Input.ChoiceSet" -> mapInputChoiceSet(element)
            "Input.Date", "Input.Time" -> mapInputDateTime(element, type)
            "ActionSet" -> mapActionSet(element)
            "FactSet" -> mapFactSet(element)
            else -> {
                warnings += "Unsupported element type '$type' preserved as placeholder"
                mapUnknown(element, type)
            }
        }
    }

    fun visitAction(element: JsonElement): String? {
        if (element !is JsonObject) return null
        val type = element["type"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = element["title"]?.jsonPrimitive?.contentOrNull ?: type
        val id = freshId(idBase(element, "button"))
        val action = when (type) {
            "Action.Submit" -> buildJsonObject {
                put("event", buildJsonObject {
                    put("name", JsonPrimitive("submit"))
                    put("context", element["data"] ?: JsonObject(emptyMap()))
                })
            }
            "Action.OpenUrl" -> {
                val url = element["url"]?.jsonPrimitive?.contentOrNull ?: ""
                buildJsonObject {
                    put("event", buildJsonObject {
                        put("name", JsonPrimitive("openUrl"))
                        put("context", buildJsonObject { put("url", JsonPrimitive(url)) })
                    })
                }
            }
            "Action.ShowCard", "Action.ToggleVisibility" -> {
                warnings += "Dropped unsupported action '$type'"
                return null
            }
            else -> {
                warnings += "Dropped unknown action '$type'"
                return null
            }
        }
        nodes += ComponentNode(
            id = id,
            component = "Button",
            children = emptyList(),
            properties = buildJsonObject {
                put("text", JsonPrimitive(title))
                put("action", action)
            },
        )
        return id
    }

    private fun mapContainer(obj: JsonObject): String {
        val items = (obj["items"] as? JsonArray).orEmptyArray()
        val childIds = items.mapNotNull { visit(it) }
        val id = freshId(idBase(obj, "container"))
        nodes += ComponentNode(
            id = id,
            component = "Column",
            children = childIds,
            properties = buildJsonObject {
                val spacing = spacingValue(obj["spacing"]?.jsonPrimitive?.contentOrNull)
                put("spacing", JsonPrimitive(spacing))
            },
        )
        return id
    }

    private fun mapColumnSet(obj: JsonObject): String {
        val columns = (obj["columns"] as? JsonArray).orEmptyArray()
        val childIds = columns.mapNotNull { visit(it) }
        val id = freshId(idBase(obj, "row"))
        nodes += ComponentNode(
            id = id,
            component = "Row",
            children = childIds,
            properties = buildJsonObject { put("spacing", JsonPrimitive(8)) },
        )
        return id
    }

    private fun mapTextBlock(obj: JsonObject): String {
        val id = freshId(idBase(obj, "text"))
        val size = obj["size"]?.jsonPrimitive?.contentOrNull
        val weight = obj["weight"]?.jsonPrimitive?.contentOrNull
        val variant = when {
            size == "ExtraLarge" -> "h1"
            size == "Large" -> "h2"
            size == "Medium" -> "h3"
            weight == "Bolder" -> "title"
            size == "Small" -> "caption"
            else -> "body"
        }
        nodes += ComponentNode(
            id = id,
            component = "Text",
            children = emptyList(),
            properties = buildJsonObject {
                put("text", JsonPrimitive(obj["text"]?.jsonPrimitive?.contentOrNull ?: ""))
                put("variant", JsonPrimitive(variant))
            },
        )
        return id
    }

    private fun mapImage(obj: JsonObject): String {
        val id = freshId(idBase(obj, "image"))
        nodes += ComponentNode(
            id = id,
            component = "Image",
            children = emptyList(),
            properties = buildJsonObject {
                put("src", JsonPrimitive(obj["url"]?.jsonPrimitive?.contentOrNull ?: ""))
                obj["altText"]?.jsonPrimitive?.contentOrNull?.let { put("contentDescription", JsonPrimitive(it)) }
            },
        )
        return id
    }

    private fun mapInputText(obj: JsonObject, numeric: Boolean = false): String {
        val inputId = obj["id"]?.jsonPrimitive?.contentOrNull ?: "input"
        val pointer = "/inputs/$inputId"
        val id = freshId(idBase(obj, "input"))
        nodes += ComponentNode(
            id = id,
            component = "TextField",
            children = emptyList(),
            properties = buildJsonObject {
                put("label", JsonPrimitive(obj["label"]?.jsonPrimitive?.contentOrNull ?: inputId))
                put("text", buildJsonObject { put("path", JsonPrimitive(pointer)) })
                if (numeric) put("numeric", JsonPrimitive(true))
                obj["placeholder"]?.jsonPrimitive?.contentOrNull?.let { put("placeholder", JsonPrimitive(it)) }
            },
        )
        return id
    }

    private fun mapInputToggle(obj: JsonObject): String {
        val inputId = obj["id"]?.jsonPrimitive?.contentOrNull ?: "toggle"
        val pointer = "/inputs/$inputId"
        val id = freshId(idBase(obj, "toggle"))
        nodes += ComponentNode(
            id = id,
            component = "CheckBox",
            children = emptyList(),
            properties = buildJsonObject {
                put("label", JsonPrimitive(obj["title"]?.jsonPrimitive?.contentOrNull ?: inputId))
                put("checked", buildJsonObject { put("path", JsonPrimitive(pointer)) })
            },
        )
        return id
    }

    private fun mapInputChoiceSet(obj: JsonObject): String {
        val inputId = obj["id"]?.jsonPrimitive?.contentOrNull ?: "choice"
        val pointer = "/inputs/$inputId"
        val id = freshId(idBase(obj, "choice"))
        val choices = (obj["choices"] as? JsonArray).orEmptyArray().mapNotNull {
            (it as? JsonObject)?.let { c ->
                buildJsonObject {
                    put("value", c["value"] ?: JsonPrimitive(""))
                    put("label", c["title"] ?: c["value"] ?: JsonPrimitive(""))
                }
            }
        }
        nodes += ComponentNode(
            id = id,
            component = "ChoicePicker",
            children = emptyList(),
            properties = buildJsonObject {
                put("label", JsonPrimitive(obj["label"]?.jsonPrimitive?.contentOrNull ?: inputId))
                put("selected", buildJsonObject { put("path", JsonPrimitive(pointer)) })
                put("choices", JsonArray(choices))
            },
        )
        return id
    }

    private fun mapInputDateTime(obj: JsonObject, type: String): String {
        val inputId = obj["id"]?.jsonPrimitive?.contentOrNull ?: "datetime"
        val pointer = "/inputs/$inputId"
        val id = freshId(idBase(obj, "datetime"))
        val format = if (type == "Input.Time") "time" else "date"
        nodes += ComponentNode(
            id = id,
            component = "DateTimeInput",
            children = emptyList(),
            properties = buildJsonObject {
                put("label", JsonPrimitive(obj["label"]?.jsonPrimitive?.contentOrNull ?: inputId))
                put("value", buildJsonObject { put("path", JsonPrimitive(pointer)) })
                put("format", JsonPrimitive(format))
            },
        )
        return id
    }

    private fun mapActionSet(obj: JsonObject): String {
        val actions = (obj["actions"] as? JsonArray).orEmptyArray()
        val childIds = actions.mapNotNull { visitAction(it) }
        val id = freshId(idBase(obj, "actionset"))
        nodes += ComponentNode(
            id = id,
            component = "Row",
            children = childIds,
            properties = buildJsonObject { put("spacing", JsonPrimitive(8)) },
        )
        return id
    }

    private fun mapFactSet(obj: JsonObject): String {
        val facts = (obj["facts"] as? JsonArray).orEmptyArray()
        val childIds = facts.mapNotNull { raw ->
            val fact = raw as? JsonObject ?: return@mapNotNull null
            val title = fact["title"]?.jsonPrimitive?.contentOrNull ?: ""
            val value = fact["value"]?.jsonPrimitive?.contentOrNull ?: ""
            val rowId = freshId("fact-row")
            val labelId = freshId("fact-label")
            val valueId = freshId("fact-value")
            nodes += ComponentNode(
                id = labelId,
                component = "Text",
                children = emptyList(),
                properties = buildJsonObject {
                    put("text", JsonPrimitive(title))
                    put("variant", JsonPrimitive("label"))
                },
            )
            nodes += ComponentNode(
                id = valueId,
                component = "Text",
                children = emptyList(),
                properties = buildJsonObject { put("text", JsonPrimitive(value)) },
            )
            nodes += ComponentNode(
                id = rowId,
                component = "Row",
                children = listOf(labelId, valueId),
                properties = buildJsonObject { put("spacing", JsonPrimitive(12)) },
            )
            rowId
        }
        val id = freshId(idBase(obj, "factset"))
        nodes += ComponentNode(
            id = id,
            component = "Column",
            children = childIds,
            properties = buildJsonObject { put("spacing", JsonPrimitive(4)) },
        )
        return id
    }

    private fun mapUnknown(obj: JsonObject, type: String): String {
        val id = freshId(idBase(obj, "unknown"))
        // Copy all properties minus `type`/`items`/`children`; unknown components can still
        // be rendered if the host registers a factory for `type`.
        val props = buildJsonObject {
            obj.forEach { (k, v) -> if (k !in reservedKeys) put(k, v) }
        }
        nodes += ComponentNode(id = id, component = type, children = emptyList(), properties = props)
        return id
    }

    private fun idBase(obj: JsonObject, fallback: String): String =
        obj["id"]?.jsonPrimitive?.contentOrNull ?: fallback

    private fun spacingValue(raw: String?): Int = when (raw) {
        "None" -> 0
        "Small" -> 4
        "Medium" -> 8
        "Large" -> 16
        "ExtraLarge" -> 24
        else -> 8
    }

    companion object {
        private val reservedKeys = setOf("type", "id", "items", "columns", "actions")
    }
}

private fun JsonArray?.orEmptyArray(): JsonArray = this ?: JsonArray(emptyList())
