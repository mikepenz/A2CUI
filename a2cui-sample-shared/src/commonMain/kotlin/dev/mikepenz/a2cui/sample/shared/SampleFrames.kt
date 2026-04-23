package dev.mikepenz.a2cui.sample.shared

/**
 * Scripted A2UI frames used by the sample hosts. Kept as raw JSON strings so the demo shows
 * exactly what a live agent would stream over the wire — no KMP-specific serialization ceremony
 * hides the payloads.
 */
internal object SampleFrames {

    fun createSurface(id: String): String = """
        {
          "version": "v0.9",
          "createSurface": {
            "surfaceId": "$id",
            "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json"
          }
        }
    """.trimIndent()

    fun bookingComponents(id: String): String = """
        {
          "version": "v0.9",
          "updateComponents": {
            "surfaceId": "$id",
            "components": [
              { "id": "root", "component": "Column", "spacing": 12, "children": ["title","sub","email","name","subscribe","submit"] },
              { "id": "title", "component": "Text", "text": "Book your table", "variant": "h2" },
              { "id": "sub",   "component": "Text", "text": "Agent-driven Compose UI demo.", "variant": "body" },
              { "id": "email", "component": "TextField", "label": "Email", "value": { "path": "/form/email" } },
              { "id": "name",  "component": "TextField", "label": "Name",  "value": { "path": "/form/name" } },
              { "id": "subscribe", "component": "CheckBox", "label": "Email me the receipt", "value": { "path": "/form/subscribe" } },
              { "id": "submit", "component": "Button", "text": "Submit booking",
                "action": { "event": { "name": "submit_booking",
                  "context": { "email": { "path": "/form/email" }, "name": { "path": "/form/name" }, "subscribe": { "path": "/form/subscribe" } } } } }
            ]
          }
        }
    """.trimIndent()

    fun seedEmail(id: String): String = """
        { "version": "v0.9",
          "updateDataModel": { "surfaceId": "$id", "path": "/form/email", "value": "hello@example.com" } }
    """.trimIndent()
}
