package dev.mikepenz.a2cui.agui.mockserver

/**
 * Raw A2UI JSON frames — mirror of `a2cui-sample-shared/SampleFrames.kt` (which is `internal`).
 * Kept here verbatim so the mock server can embed them as AG-UI `CUSTOM` values without crossing
 * the internal visibility boundary.
 */
public object SampleA2uiFrames {

    public fun createSurface(id: String): String = """
        {
          "version": "v0.9",
          "createSurface": {
            "surfaceId": "$id",
            "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json"
          }
        }
    """.trimIndent()

    public fun bookingComponents(id: String): String = """
        {
          "version": "v0.9",
          "updateComponents": {
            "surfaceId": "$id",
            "components": [
              { "id": "root", "component": "Column", "spacing": 12, "children": ["title","sub","email","name","subscribe","submit"] },
              { "id": "title", "component": "Text", "text": "Book your table", "variant": "h2" },
              { "id": "sub",   "component": "Text", "text": "Live agent demo over SSE.", "variant": "body" },
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

    public fun seedEmail(id: String): String = """
        { "version": "v0.9",
          "updateDataModel": { "surfaceId": "$id", "path": "/form/email", "value": "hello@example.com" } }
    """.trimIndent()
}
