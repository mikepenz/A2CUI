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
}

internal object A2uiFrameSerializer : JsonContentPolymorphicSerializer<A2uiFrame>(A2uiFrame::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<A2uiFrame> {
        val obj = element.jsonObject
        return when {
            "createSurface" in obj -> A2uiFrame.CreateSurface.serializer()
            "updateComponents" in obj -> A2uiFrame.UpdateComponents.serializer()
            "updateDataModel" in obj -> A2uiFrame.UpdateDataModel.serializer()
            "deleteSurface" in obj -> A2uiFrame.DeleteSurface.serializer()
            else -> throw SerializationException(
                "Unknown A2UI frame — no recognized key in ${obj.keys}"
            )
        }
    }
}
