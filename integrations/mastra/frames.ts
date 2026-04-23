/**
 * A2UI v0.9 frame builders — mirror of `SampleA2uiFrames.kt`.
 *
 * Each builder returns a plain JSON object that is embedded verbatim as the
 * `value` of an AG-UI `CUSTOM` event with `name: "a2ui"`.
 */

export function createSurface(surfaceId: string) {
  return {
    version: "v0.9",
    createSurface: {
      surfaceId,
      catalogId: "https://a2ui.org/specification/v0_9/basic_catalog.json",
    },
  } as const;
}

export function bookingComponents(surfaceId: string) {
  return {
    version: "v0.9",
    updateComponents: {
      surfaceId,
      components: [
        { id: "root", component: "Column", spacing: 12,
          children: ["title", "sub", "email", "name", "subscribe", "submit"] },
        { id: "title", component: "Text", text: "Book your table", variant: "h2" },
        { id: "sub", component: "Text", text: "Live agent demo — Mastra.", variant: "body" },
        { id: "email", component: "TextField", label: "Email", value: { path: "/form/email" } },
        { id: "name", component: "TextField", label: "Name", value: { path: "/form/name" } },
        { id: "subscribe", component: "CheckBox", label: "Email me the receipt",
          value: { path: "/form/subscribe" } },
        { id: "submit", component: "Button", text: "Submit booking",
          action: { event: { name: "submit_booking", context: {
            email: { path: "/form/email" },
            name: { path: "/form/name" },
            subscribe: { path: "/form/subscribe" },
          }}}},
      ],
    },
  } as const;
}

export function seedEmail(surfaceId: string) {
  return {
    version: "v0.9",
    updateDataModel: {
      surfaceId,
      path: "/form/email",
      value: "hello@example.com",
    },
  } as const;
}
