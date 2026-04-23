package dev.mikepenz.a2cui.compose

import androidx.compose.runtime.Immutable
import kotlinx.serialization.json.JsonObject

/**
 * Agent-supplied theme hints, carried through from [dev.mikepenz.a2cui.core.A2uiFrame.CreateSurface].
 * Catalog components are expected to *merge* these hints with the host app's `MaterialTheme`
 * rather than treat them as authoritative — the host decides what's safe to honour.
 */
@Immutable
public data class A2cuiTheme(val hints: JsonObject?) {
    public companion object {
        public val Empty: A2cuiTheme = A2cuiTheme(hints = null)
    }
}
