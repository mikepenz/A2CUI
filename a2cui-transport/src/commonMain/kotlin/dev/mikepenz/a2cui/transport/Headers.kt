package dev.mikepenz.a2cui.transport

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

/** Apply an external header map to a Ktor request. Separated out to avoid KMP destructuring
 *  inference ambiguities that surface on `Map<String, String>.forEach { (k, v) -> ... }`. */
internal fun HttpRequestBuilder.applyHeaders(headers: Map<String, String>) {
    for (entry in headers) header(entry.key, entry.value)
}
