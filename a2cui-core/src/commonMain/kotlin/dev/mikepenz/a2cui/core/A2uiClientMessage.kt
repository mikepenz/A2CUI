package dev.mikepenz.a2cui.core

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Client → server messages. Two shapes from the v0.9 spec:
 *  - [Action]: a named event with resolved context, fired when a server-bound action triggers.
 *  - [Error]: validation / runtime error envelope with a JSON Pointer pointing at the offender.
 */
@Serializable(with = A2uiClientMessageSerializer::class)
public sealed interface A2uiClientMessage {
    public val version: String

    @Serializable
    public data class Action(
        override val version: String,
        val action: Body,
    ) : A2uiClientMessage {
        @Serializable
        public data class Body(
            val surfaceId: String,
            val name: String,
            val sourceComponentId: String,
            val context: JsonObject = JsonObject(emptyMap()),
            val timestamp: Long? = null,
        )
    }

    @Serializable
    public data class Error(
        override val version: String,
        val error: Body,
    ) : A2uiClientMessage {
        @Serializable
        public data class Body(
            val code: String,
            val message: String,
            val path: String? = null,
            val surfaceId: String? = null,
        )
    }
}

internal object A2uiClientMessageSerializer :
    JsonContentPolymorphicSerializer<A2uiClientMessage>(A2uiClientMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<A2uiClientMessage> {
        val obj = element.jsonObject
        return when {
            "action" in obj -> A2uiClientMessage.Action.serializer()
            "error" in obj -> A2uiClientMessage.Error.serializer()
            else -> throw SerializationException(
                "Unknown A2UI client message — no recognized key in ${obj.keys}"
            )
        }
    }
}
