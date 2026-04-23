package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory

/**
 * Image placeholder. A real image-loading pipeline (Coil / resource handles) lives outside
 * `:a2cui-compose` so the core module stays free of network deps; host apps can override
 * this registration with `registry.register("Image") { node, scope -> ... }`.
 */
internal val ImageFactory: ComponentFactory = @Composable { node, scope ->
    val url = scope.resolveString(node, "url")
    val size = scope.resolveInt(node, "size", default = 96).dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        Text(
            text = if (url.isNotEmpty()) "🖼 $url" else "🖼",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Icon placeholder. Renders the icon name as a stylised label. A Material-Icons lookup can
 * be layered in an extension module since `compose.materialIconsExtended` is pinned and
 * not included in the base catalog.
 */
internal val IconFactory: ComponentFactory = @Composable { node, scope ->
    val name = scope.resolveString(node, "name", default = "?")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
