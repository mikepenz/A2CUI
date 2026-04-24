package dev.mikepenz.a2cui.compose.catalog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mikepenz.a2cui.compose.ComponentFactory
import kotlinx.serialization.json.JsonPrimitive

/**
 * DateTimeInput renders a read-only text field showing the current ISO-8601 value. Tapping the
 * field opens a Material 3 date or time picker dialog keyed off the `format` prop:
 *
 *  - `format: "date"` (default) → [DatePicker], value stored as `YYYY-MM-DD`.
 *  - `format: "time"` → [TimePicker], value stored as `HH:MM` (24-hour).
 *  - `format: "datetime"` → date picker followed by time picker; value stored as
 *    `YYYY-MM-DDTHH:MM` (ISO-8601 local-time form, no timezone). Cancelling the time picker
 *    after the date picker still commits the date portion at midnight (`T00:00`) so the field
 *    is never left half-set.
 *
 * Manual text entry is deliberately NOT supported — the picker is the only input path so values
 * stay well-formed. Hosts that want free-text entry can `registry.register("DateTimeInput") { ... }`
 * with their own implementation.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal val DateTimeInputFactory: ComponentFactory = @Composable { node, scope ->
    val label = scope.resolveString(node, "label")
    val format = scope.resolveString(node, "format", default = "date").lowercase()
    val path = remember(node.id) { pathOf(node.properties, "value") }

    val current: String = if (path != null) {
        val el by scope.dataModel.observe(path).collectAsState(initial = scope.dataModel.read(path))
        (el as? JsonPrimitive)?.content ?: ""
    } else {
        scope.resolveString(node, "value")
    }

    var showPicker by remember { mutableStateOf(false) }

    // Read-only OutlinedTextField still consumes pointer events for focus/cursor handling, so
    // wrapping it in `clickable` or hand-rolled pointerInput loops will either swallow the
    // tap or leak forever. The Material-recommended pattern is `interactionSource` + observe
    // press interactions: the TextField surfaces every press through the same flow whether the
    // field is enabled, disabled, or read-only.
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showPicker = true
        }
    }

    OutlinedTextField(
        value = current,
        onValueChange = {},
        readOnly = true,
        label = if (label.isNotEmpty()) { { Text(label) } } else null,
        placeholder = { Text(if (format == "time") "HH:MM" else "YYYY-MM-DD") },
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth(),
    )

    // For `datetime`, the date picker confirms first, then we switch into the time picker.
    // `pendingDate` carries the chosen ISO date between the two stages.
    var pendingDate by remember { mutableStateOf<String?>(null) }

    if (showPicker && path != null) {
        when (format) {
            "time" -> {
                val (h, m) = parseTime(current)
                val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
                TimePickerSheet(
                    onDismiss = { showPicker = false },
                    onConfirm = {
                        scope.dataModel.write(path, JsonPrimitive(formatTime(state.hour, state.minute)))
                        showPicker = false
                    },
                ) {
                    Box(Modifier.padding(24.dp)) { TimePicker(state = state) }
                }
            }
            "datetime" -> {
                if (pendingDate == null) {
                    val state = rememberDatePickerState(initialSelectedDateMillis = parseDateMillis(current))
                    DatePickerDialog(
                        onDismissRequest = { showPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val millis = state.selectedDateMillis
                                pendingDate = if (millis != null) millisToIsoDate(millis) else null
                                if (pendingDate == null) showPicker = false
                            }) { Text("Next") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                        },
                    ) { DatePicker(state = state) }
                } else {
                    val (h0, m0) = parseTime(current.substringAfter('T', ""))
                    val state = rememberTimePickerState(initialHour = h0, initialMinute = m0, is24Hour = true)
                    TimePickerSheet(
                        onDismiss = {
                            // User cancelled the time stage — commit the date at midnight so we don't
                            // leave the field half-set.
                            scope.dataModel.write(path, JsonPrimitive("${pendingDate}T00:00"))
                            pendingDate = null
                            showPicker = false
                        },
                        onConfirm = {
                            scope.dataModel.write(
                                path,
                                JsonPrimitive("${pendingDate}T${formatTime(state.hour, state.minute)}"),
                            )
                            pendingDate = null
                            showPicker = false
                        },
                    ) {
                        Box(Modifier.padding(24.dp)) { TimePicker(state = state) }
                    }
                }
            }
            else -> {
                val state = rememberDatePickerState(initialSelectedDateMillis = parseDateMillis(current))
                DatePickerDialog(
                    onDismissRequest = { showPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val millis = state.selectedDateMillis
                            if (millis != null) scope.dataModel.write(path, JsonPrimitive(millisToIsoDate(millis)))
                            showPicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                    },
                ) {
                    DatePicker(state = state)
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val hh = hour.toString().padStart(2, '0')
    val mm = minute.toString().padStart(2, '0')
    return "$hh:$mm"
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimePickerSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    // Material3 ships DatePickerDialog but not a TimePickerDialog in common — reuse DatePickerDialog's
    // dialog chrome by wrapping the TimePicker ourselves via androidx.compose.ui.window.Dialog.
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
        ) {
            androidx.compose.foundation.layout.Column {
                content()
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(end = 8.dp, bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}

/** Parse `HH:MM` to a `(hour, minute)` pair; returns `(0,0)` on failure. */
private fun parseTime(raw: String): Pair<Int, Int> {
    val parts = raw.split(':')
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

/** Parse `YYYY-MM-DD` (or a leading date from a longer ISO string) to epoch millis; null on failure. */
private fun parseDateMillis(raw: String): Long? {
    val datePart = raw.take(10)
    val parts = datePart.split('-')
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val mo = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (y < 1970 || mo !in 1..12 || d !in 1..31) return null
    // Days from 1970-01-01 using a simple civil-date conversion — good enough for a form
    // picker that doesn't care about timezones. Sourced from howardhinnant.github.io/date_algorithms.
    val yy = y - (if (mo <= 2) 1 else 0)
    val era = (if (yy >= 0) yy else yy - 399) / 400
    val yoe = yy - era * 400
    val doy = (153 * (mo + (if (mo > 2) -3 else 9)) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era * 146097L + doe - 719468L
    return days * 86_400_000L
}

/** Inverse of [parseDateMillis]. */
private fun millisToIsoDate(millis: Long): String {
    val days = (millis / 86_400_000L) + 719468L
    val era = (if (days >= 0) days else days - 146096L) / 146097L
    val doe = days - era * 146097L
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val mo = mp + (if (mp < 10) 3 else -9)
    val yAdj = y + (if (mo <= 2) 1 else 0)
    val yStr = yAdj.toString().padStart(4, '0')
    val moStr = mo.toString().padStart(2, '0')
    val dStr = d.toString().padStart(2, '0')
    return "$yStr-$moStr-$dStr"
}
