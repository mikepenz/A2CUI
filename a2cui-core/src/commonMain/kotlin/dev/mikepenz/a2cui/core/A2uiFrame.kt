package dev.mikepenz.a2cui.core

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * One message in the A2UI server→client stream. Frames are discriminated by which of
 * `createSurface` / `updateComponents` / `updateDataModel` / `deleteSurface` appears as a
 * top-level key, not by a `type` field — this matches the v0.9 spec verbatim.
 *
 * @see <a href="https://a2ui.org/specification/v0.9-a2ui/">A2UI v0.9 specification</a>
 */
@Serializable(with = A2uiFrameSerializer::class)
public sealed interface A2uiFrame {
    /** The A2UI spec version this frame was authored against (e.g. `"v0.9"`). */
    public val version: String

    @Serializable
    public data class CreateSurface(
        override val version: String,
        val createSurface: Body,
    ) : A2uiFrame {
        @Serializable
        public data class Body(
            val surfaceId: String,
            val catalogId: String,
            val theme: JsonObject? = null,
            val sendDataModel: Boolean = false,
        )
    }

    @Serializable
    public data class UpdateComponents(
        override val version: String,
        val updateComponents: Body,
    ) : A2uiFrame {
        @Serializable
        public data class Body(
            val surfaceId: String,
            val components: List<ComponentNode>,
        )
    }

    @Serializable
    public data class UpdateDataModel(
        override val version: String,
        val updateDataModel: Body,
    ) : A2uiFrame {
        @Serializable
        public data class Body(
            val surfaceId: String,
            val path: String,
            val value: JsonElement,
        )
    }

    @Serializable
    public data class DeleteSurface(
        override val version: String,
        val deleteSurface: Body,
    ) : A2uiFrame {
        @Serializable
        public data class Body(val surfaceId: String)
    }

    /**
     * **A2CUI experimental extension** — multi-op data-model patch. Replaces a run of individual
     * `updateDataModel` frames with a single JSON-Patch-style batch. The exact wire shape is
     * not finalised in the A2UI spec yet — this is an A2CUI-proposed candidate discriminated by
     * the top-level `dataModelPatch` key. Revisit when the draft firms up.
     */
    @ExperimentalA2uiDraft
    @Serializable
    public data class DataModelPatch(
        override val version: String,
        val dataModelPatch: Body,
    ) : A2uiFrame {
        @Serializable
        public data class Body(
            val surfaceId: String,
            val operations: List<Operation>,
        )

        @Serializable
        public data class Operation(
            val op: String,
            val path: String,
            val value: JsonElement? = null,
        )
    }

    /**
     * **A2CUI experimental extension** — server-initiated focus / scroll hint. Agents can nudge the client to
     * bring a specific component into view (e.g. after appending a new validation error to the
     * data model, scroll the offending field into focus). Discriminated by the top-level
     * `scrollTo` key. Exact motion semantics (instant vs animated, offset rules) are left to
     * the host catalog.
     */
    @ExperimentalA2uiDraft
    @Serializable
    public data class ScrollTo(
        override val version: String,
        val scrollTo: Body,
    ) : A2uiFrame {
        @Serializable
        public data class Body(
            val surfaceId: String,
            /** Component id to bring into view. Must already exist in the current surface tree. */
            val componentId: String,
            /** Optional behaviour hint: `"auto"` (default), `"smooth"`, or `"instant"`. */
            val behavior: String? = null,
            /** If true, also request keyboard focus on the component (when applicable). */
            val focus: Boolean = false,
        )
    }
}

internal object A2uiFrameSerializer : JsonContentPolymorphicSerializer<A2uiFrame>(A2uiFrame::class) {
    @OptIn(ExperimentalA2uiDraft::class)
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<A2uiFrame> {
        val obj = element.jsonObject
        return when {
            "createSurface" in obj -> A2uiFrame.CreateSurface.serializer()
            "updateComponents" in obj -> A2uiFrame.UpdateComponents.serializer()
            "updateDataModel" in obj -> A2uiFrame.UpdateDataModel.serializer()
            "deleteSurface" in obj -> A2uiFrame.DeleteSurface.serializer()
            // A2CUI experimental extensions — never hit on v0.9 JSON.
            "dataModelPatch" in obj -> A2uiFrame.DataModelPatch.serializer()
            "scrollTo" in obj -> A2uiFrame.ScrollTo.serializer()
            else -> throw SerializationException(
                "Unknown A2UI frame — no recognized key in ${obj.keys}"
            )
        }
    }
}
