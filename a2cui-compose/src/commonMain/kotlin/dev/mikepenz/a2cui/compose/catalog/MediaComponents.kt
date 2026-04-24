package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
    val contentDescription = scope.resolveString(node, "contentDescription", default = "")

    val hasSize = node.properties["size"] != null
    val hasWidth = node.properties["width"] != null
    val hasHeight = node.properties["height"] != null

    // Precedence:
    //   * explicit width/height override everything (mix-and-match OK),
    //   * legacy `size` is a square shortcut,
    //   * otherwise default to a 96dp square so the LLM doesn't need to know layout units.
    val widthDp = when {
        hasWidth -> scope.resolveInt(node, "width", default = 96).dp
        hasSize -> scope.resolveInt(node, "size", default = 96).dp
        else -> 96.dp
    }
    val heightDp = when {
        hasHeight -> scope.resolveInt(node, "height", default = 96).dp
        hasSize -> scope.resolveInt(node, "size", default = 96).dp
        else -> 96.dp
    }

    val scaleKey = scope.resolveString(node, "contentScale", default = "crop").lowercase()
    val scale = when (scaleKey) {
        "fit" -> ContentScale.Fit
        "inside" -> ContentScale.Inside
        "fill", "fillbounds" -> ContentScale.FillBounds
        "fillwidth" -> ContentScale.FillWidth
        "fillheight" -> ContentScale.FillHeight
        "none" -> ContentScale.None
        else -> ContentScale.Crop
    }

    val shape = RoundedCornerShape(4.dp)
    val sizingModifier = Modifier.width(widthDp).height(heightDp).clip(shape)

    if (src.isEmpty()) {
        Box(
            modifier = sizingModifier
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
            contentScale = scale,
            modifier = sizingModifier,
        )
    }
}

/**
 * Icon factory backed by Material Symbols (via `compose.materialIconsExtended`).
 *
 * Props:
 *  - `name` — icon identifier. Matched case-insensitively against [materialIconLookup] using
 *    snake_case, camelCase, or lowercase variants (e.g. `"home"`, `"more_vert"`, `"moreVert"`,
 *    `"shopping_cart"` all resolve). When the name doesn't match, falls back to a text chip
 *    showing the raw name so the surface stays debuggable rather than rendering a blank.
 *  - `style` — `"filled"` (default), `"outlined"`. The map only exposes the most common
 *    outlined variants; unknown styles fall through to filled. Hosts can extend coverage by
 *    registering their own Icon factory.
 *  - `size` — int dp, default 24.
 *
 * The bundled lookup is intentionally curated rather than exhaustive: shipping all ~2k icons
 * in the lookup `when` would balloon the binary for marginal benefit. Hosts that need broader
 * coverage can override via `registry.register("Icon") { ... }`.
 */
internal val IconFactory: ComponentFactory = @Composable { node, scope ->
    val name = scope.resolveString(node, "name", default = "")
    val style = scope.resolveString(node, "style", default = "filled").lowercase()
    val size = scope.resolveInt(node, "size", default = 24)
    val description = scope.resolveString(node, "contentDescription", default = name)

    val vector = materialIconLookup(name, style)
    if (vector != null) {
        Icon(
            imageVector = vector,
            contentDescription = description,
            modifier = Modifier.size(size.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        // Unknown name — render the literal so missing icons are visible rather than invisible.
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = name.ifEmpty { "?" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/**
 * Map a Material Symbol [name] to an [ImageVector]. The match is case-insensitive and tolerant
 * of `snake_case`/`camelCase` so agents can pass either flavour.
 */
private fun materialIconLookup(name: String, style: String): ImageVector? {
    if (name.isEmpty()) return null
    val key = name.lowercase().replace("_", "").replace("-", "")
    return when (style) {
        "outlined" -> outlinedIcon(key) ?: filledIcon(key)
        else -> filledIcon(key)
    }
}

@Suppress("LongMethod")
private fun filledIcon(key: String): ImageVector? = when (key) {
    "add", "plus" -> Icons.Filled.Add
    "arrowback", "back" -> Icons.AutoMirrored.Filled.ArrowBack
    "arrowdown", "arrowdownward" -> Icons.Filled.ArrowDownward
    "arrowdropdown", "dropdown" -> Icons.Filled.ArrowDropDown
    "arrowforward", "forward" -> Icons.AutoMirrored.Filled.ArrowForward
    "arrowup", "arrowupward" -> Icons.Filled.ArrowUpward
    "calculate", "calculator" -> Icons.Filled.Calculate
    "calendar", "calendarmonth", "date" -> Icons.Filled.CalendarMonth
    "cancel" -> Icons.Filled.Cancel
    "check", "tick" -> Icons.Filled.Check
    "checkcircle" -> Icons.Filled.CheckCircle
    "close", "x" -> Icons.Filled.Close
    "delete", "trash" -> Icons.Filled.Delete
    "done" -> Icons.Filled.Done
    "download" -> Icons.Filled.Download
    "edit", "pencil" -> Icons.Filled.Edit
    "email", "mail" -> Icons.Filled.Email
    "error" -> Icons.Filled.Error
    "expandless" -> Icons.Filled.ExpandLess
    "expandmore" -> Icons.Filled.ExpandMore
    "favorite", "heart" -> Icons.Filled.Favorite
    "favoriteborder", "heartborder" -> Icons.Filled.FavoriteBorder
    "filter" -> Icons.Filled.Filter
    "help", "question" -> Icons.AutoMirrored.Filled.Help
    "home", "house" -> Icons.Filled.Home
    "image", "picture" -> Icons.Filled.Image
    "info", "information" -> Icons.Filled.Info
    "keyboardarrowdown", "chevrondown" -> Icons.Filled.KeyboardArrowDown
    "keyboardarrowleft", "chevronleft" -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
    "keyboardarrowright", "chevronright" -> Icons.AutoMirrored.Filled.KeyboardArrowRight
    "keyboardarrowup", "chevronup" -> Icons.Filled.KeyboardArrowUp
    "list", "menulist" -> Icons.AutoMirrored.Filled.List
    "lock" -> Icons.Filled.Lock
    "logout", "signout" -> Icons.AutoMirrored.Filled.Logout
    "menu", "hamburger" -> Icons.Filled.Menu
    "morehoriz" -> Icons.Filled.MoreHoriz
    "morevert" -> Icons.Filled.MoreVert
    "notifications", "bell" -> Icons.Filled.Notifications
    "person", "user", "profile", "account" -> Icons.Filled.Person
    "phone", "call" -> Icons.Filled.Phone
    "play", "playarrow" -> Icons.Filled.PlayArrow
    "refresh", "reload" -> Icons.Filled.Refresh
    "remove", "minus" -> Icons.Filled.Remove
    "save" -> Icons.Filled.Save
    "schedule", "clock", "time" -> Icons.Filled.Schedule
    "search", "find" -> Icons.Filled.Search
    "send" -> Icons.AutoMirrored.Filled.Send
    "settings", "gear", "cog" -> Icons.Filled.Settings
    "share" -> Icons.Filled.Share
    "shoppingcart", "cart", "basket" -> Icons.Filled.ShoppingCart
    "star" -> Icons.Filled.Star
    "starborder" -> Icons.Filled.StarBorder
    "thumbdown", "dislike" -> Icons.Filled.ThumbDown
    "thumbup", "like" -> Icons.Filled.ThumbUp
    "upload" -> Icons.Filled.Upload
    "visibility", "show", "eye" -> Icons.Filled.Visibility
    "visibilityoff", "hide", "eyeoff" -> Icons.Filled.VisibilityOff
    "warning", "alert" -> Icons.Filled.Warning
    else -> null
}

private fun outlinedIcon(key: String): ImageVector? = when (key) {
    "arrowback", "back" -> Icons.AutoMirrored.Outlined.ArrowBack
    "arrowforward", "forward" -> Icons.AutoMirrored.Outlined.ArrowForward
    "email", "mail" -> Icons.Outlined.Email
    "favorite", "heart" -> Icons.Outlined.Favorite
    "home", "house" -> Icons.Outlined.Home
    "info", "information" -> Icons.Outlined.Info
    "lock" -> Icons.Outlined.Lock
    "notifications", "bell" -> Icons.Outlined.Notifications
    "person", "user", "profile", "account" -> Icons.Outlined.Person
    "settings", "gear", "cog" -> Icons.Outlined.Settings
    "star" -> Icons.Outlined.Star
    else -> null
}
