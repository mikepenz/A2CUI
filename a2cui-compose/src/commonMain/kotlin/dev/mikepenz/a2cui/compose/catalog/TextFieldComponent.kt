package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive

/**
 * TextField with optional two-way data binding via `{"value": {"path":"/ptr"}}`.
 *
 * Supported props (beyond `value` and `label`):
 *  - `placeholder` (str)
 *  - `enabled` (bool, default true)
 *  - `singleLine` (bool, default true)
 *  - `supportingText` (str, shown below the field)
 *  - `keyboardType` (str, one of `text`, `email`, `number`, `phone`, `url`, `password`,
 *    `decimal`, `ascii`; default `text`). `password` also applies a password visual mask.
 *  - `isError` (bool, renders the Material error state)
 */
internal val TextFieldFactory: ComponentFactory = @Composable { node, scope ->
    val label = scope.resolveString(node, "label")
    val placeholder = scope.resolveString(node, "placeholder")
    val support = scope.resolveString(node, "supportingText")
    val enabled = scope.resolveBool(node, "enabled", default = true)
    val singleLine = scope.resolveBool(node, "singleLine", default = true)
    val isError = scope.resolveBool(node, "isError", default = false)
    val keyboardTypeKey = scope.resolveString(node, "keyboardType", default = "text")
    val isPassword = keyboardTypeKey.equals("password", ignoreCase = true)
    val keyboardOptions = remember(keyboardTypeKey, singleLine) {
        KeyboardOptions(
            keyboardType = keyboardTypeKey.keyboardType(),
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        )
    }
    val visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None

    val bindingPath = remember(node.id) { pathOf(node.properties, "value") }
    if (bindingPath != null) {
        val valueElement by scope.dataModel
            .observe(bindingPath)
            .collectAsState(initial = scope.dataModel.read(bindingPath))
        val current = valueElement.displayString()
        OutlinedTextField(
            value = current,
            onValueChange = { scope.dataModel.write(bindingPath, JsonPrimitive(it)) },
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
            supportingText = if (support.isNotEmpty()) { { Text(support) } } else null,
            enabled = enabled,
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
        )
    } else {
        val literal = scope.resolveString(node, "value")
        OutlinedTextField(
            value = literal,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = if (label.isNotEmpty()) { { Text(label) } } else null,
            supportingText = if (support.isNotEmpty()) { { Text(support) } } else null,
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
        )
    }
}

private fun String.keyboardType(): KeyboardType = when (lowercase()) {
    "email" -> KeyboardType.Email
    "number" -> KeyboardType.Number
    "decimal" -> KeyboardType.Decimal
    "phone" -> KeyboardType.Phone
    "url" -> KeyboardType.Uri
    "ascii" -> KeyboardType.Ascii
    "password" -> KeyboardType.Password
    else -> KeyboardType.Text
}
