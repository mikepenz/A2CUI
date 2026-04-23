package dev.mikepenz.a2cui.compose

import androidx.compose.runtime.Composable
import dev.mikepenz.a2cui.core.ComponentNode

/**
 * Renders one [ComponentNode]. Implementations read typed properties from [node].properties,
 * resolve data bindings through [scope].resolver, and recurse into [scope].Child for each
 * declared child id.
 */
public typealias ComponentFactory = @Composable (node: ComponentNode, scope: RenderScope) -> Unit
