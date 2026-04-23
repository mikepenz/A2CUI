package dev.mikepenz.a2cui.actions

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal carrying the ambient [ActionRegistry]. The host app sets this via
 * `CompositionLocalProvider(LocalActionRegistry provides …)` at the top of its tree.
 * Descendant composables use [dev.mikepenz.a2cui.actions.rememberAction] to register
 * per-screen generative-UI renderers.
 */
public val LocalActionRegistry = staticCompositionLocalOf<ActionRegistry> {
    error("LocalActionRegistry not provided. Wrap your tree with CompositionLocalProvider.")
}
