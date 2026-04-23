package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.mikepenz.a2cui.compose.ComponentFactory

internal val TextFactory: ComponentFactory = @Composable { node, scope ->
    val text = scope.resolveString(node, "text")
    val variant = scope.resolveString(node, "variant", default = "body")
    val style = when (variant) {
        "h1", "headline-large" -> MaterialTheme.typography.headlineLarge
        "h2", "headline-medium" -> MaterialTheme.typography.headlineMedium
        "h3", "headline-small" -> MaterialTheme.typography.headlineSmall
        "title" -> MaterialTheme.typography.titleMedium
        "label" -> MaterialTheme.typography.labelMedium
        "caption" -> MaterialTheme.typography.bodySmall
        else -> MaterialTheme.typography.bodyMedium
    }
    Text(text = text, style = style)
}
