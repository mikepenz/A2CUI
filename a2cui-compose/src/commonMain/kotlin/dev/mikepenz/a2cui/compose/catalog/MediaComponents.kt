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
import coil3.compose.AsyncImage
import dev.mikepenz.a2cui.compose.ComponentFactory

/**
 * Image factory. Uses Coil 3 Multiplatform's [AsyncImage] against the resolved `src` property
 * (url or `file://` URI). Falls back to `url` for backwards compatibility. When the resolved
 * src is empty, renders a muted placeholder box. Hosts may override this registration with
 * `registry.register("Image") { node, scope -> ... }` to plug an alternative loader.
 */
internal val ImageFactory: ComponentFactory = @Composable { node, scope ->
    val src = scope.resolveString(node, "src").ifEmpty { scope.resolveString(node, "url") }
    val size = scope.resolveInt(node, "size", default = 96).dp
    val contentDescription = scope.resolveString(node, "contentDescription", default = "")
    val shape = RoundedCornerShape(4.dp)
    if (src.isEmpty()) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
        ) {
            Text(
                text = "🖼",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = src,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .clip(shape),
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
