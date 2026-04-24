package dev.mikepenz.a2cui.sample.shared

/**
 * Richer multi-section A2UI scenario: movie theater booking.
 *
 * Demonstrates iteration-scope `List` binding, `ChoicePicker`, `Slider`, `Card`, `Row`, `Image`,
 * and a dynamic summary driven by data-model writes — the kind of surface a real production agent
 * would stream rather than the single-form booking demo.
 */
internal object TheaterFrames {

    fun createSurface(id: String): String = """
        {
          "version": "v0.9",
          "createSurface": {
            "surfaceId": "$id",
            "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json"
          }
        }
    """.trimIndent()

    fun seedCatalog(id: String): String = """
        { "version": "v0.9",
          "updateDataModel": { "surfaceId": "$id", "path": "", "value": {
            "movies": [
              { "title": "The Quantum Garden", "rating": "PG-13", "runtime": "2h 14m",
                "poster": "https://picsum.photos/seed/quantum/120/180" },
              { "title": "Echoes of Tomorrow", "rating": "PG",    "runtime": "1h 48m",
                "poster": "https://picsum.photos/seed/echoes/120/180" },
              { "title": "Last Light",         "rating": "R",     "runtime": "2h 02m",
                "poster": "https://picsum.photos/seed/lastlight/120/180" }
            ],
            "booking": {
              "movie":    "The Quantum Garden",
              "showtime": "7:30 PM",
              "tickets":  2,
              "seating":  "Standard",
              "snacks":   true
            }
          } } }
    """.trimIndent()

    fun theaterComponents(id: String): String = """
        {
          "version": "v0.9",
          "updateComponents": {
            "surfaceId": "$id",
            "components": [
              { "id": "root", "component": "Column", "spacing": 16,
                "children": ["headerRow","sub","nowPlayingCard","bookingCard","summaryCard","submit"] },

              { "id": "headerRow", "component": "Row", "spacing": 8, "alignment": "center",
                "children": ["headerIcon","header"] },
              { "id": "headerIcon", "component": "Icon", "name": "calendar_month", "size": 28 },
              { "id": "header", "component": "Text", "text": "Tonight at the Cinema", "variant": "h2" },
              { "id": "sub",    "component": "Text", "text": "Pick a film, choose your seats, agent will handle the rest.", "variant": "body" },

              { "id": "nowPlayingCard", "component": "Card",
                "children": ["nowPlayingTitle","movieList"] },
              { "id": "nowPlayingTitle", "component": "Text", "text": "Now playing", "variant": "h3" },
              { "id": "movieList", "component": "List", "spacing": 12, "lazy": false,
                "items": { "path": "/movies" },
                "children": ["movieRow"] },
              { "id": "movieRow", "component": "Row", "spacing": 12,
                "children": ["moviePoster","movieMeta"] },
              { "id": "moviePoster", "component": "Image",
                "url": { "path": "/poster" }, "width": 60, "height": 90 },
              { "id": "movieMeta", "component": "Column", "spacing": 4,
                "children": ["movieTitle","movieSub"] },
              { "id": "movieTitle", "component": "Text", "text": { "path": "/title" }, "variant": "h3" },
              { "id": "movieSub",   "component": "Text", "text": { "path": "/rating" }, "variant": "body" },

              { "id": "bookingCard", "component": "Card",
                "children": ["bookingTitle","movieField","showtimeField","ticketsSlider","seatingField","snacksField"] },
              { "id": "bookingTitle", "component": "Text", "text": "Your booking", "variant": "h3" },
              { "id": "movieField", "component": "ChoicePicker", "label": "Movie",
                "choices": ["The Quantum Garden","Echoes of Tomorrow","Last Light"],
                "value": { "path": "/booking/movie" } },
              { "id": "showtimeField", "component": "ChoicePicker", "label": "Showtime",
                "choices": ["4:00 PM","7:30 PM","10:15 PM"],
                "value": { "path": "/booking/showtime" } },
              { "id": "ticketsSlider", "component": "Slider", "label": "Tickets",
                "min": 1, "max": 8, "step": 1,
                "value": { "path": "/booking/tickets" } },
              { "id": "seatingField", "component": "ChoicePicker", "label": "Seating",
                "choices": ["Standard","Premium","IMAX"],
                "value": { "path": "/booking/seating" } },
              { "id": "snacksField", "component": "CheckBox", "label": "Add popcorn & drinks combo (+$8)",
                "value": { "path": "/booking/snacks" } },

              { "id": "summaryCard", "component": "Card", "children": ["summaryTitle","summaryMovie","summarySeats","summaryNote"] },
              { "id": "summaryTitle", "component": "Text", "text": "Summary", "variant": "h3" },
              { "id": "summaryMovie", "component": "Text",
                "text": { "path": "/booking/movie" }, "variant": "body" },
              { "id": "summarySeats", "component": "Text",
                "text": { "path": "/booking/seating" }, "variant": "body" },
              { "id": "summaryNote",  "component": "Text",
                "text": "Final pricing confirmed by the agent on submit.", "variant": "body" },

              { "id": "submit", "component": "Button", "text": "Confirm booking",
                "action": { "event": { "name": "confirm_booking",
                  "context": {
                    "movie":    { "path": "/booking/movie" },
                    "showtime": { "path": "/booking/showtime" },
                    "tickets":  { "path": "/booking/tickets" },
                    "seating":  { "path": "/booking/seating" },
                    "snacks":   { "path": "/booking/snacks" }
                  } } } }
            ]
          }
        }
    """.trimIndent()
}
