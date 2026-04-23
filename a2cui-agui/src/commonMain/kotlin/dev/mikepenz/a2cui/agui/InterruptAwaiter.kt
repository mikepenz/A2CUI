package dev.mikepenz.a2cui.agui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Suspends until the next `RUN_FINISHED { outcome = "interrupt" }` arrives on [events],
 * then exposes the interrupts and a resolver so the UI can collect the user's decision.
 *
 * The resume contract per the AG-UI draft is a *new* run whose request body carries
 * `resume: [{ interruptId, status, payload? }]`. [ApprovalResponse] models that payload;
 * the transport-side concern of starting the new run lives in the calling app.
 *
 * @see <a href="https://docs.ag-ui.com/drafts/interrupts">AG-UI draft interrupt spec</a>
 */
public class InterruptAwaiter(private val bridge: AguiA2cuiBridge) {

    public data class ApprovalResponse(
        val interruptId: String,
        val status: Status,
        val payload: kotlinx.serialization.json.JsonObject? = null,
    ) {
        public enum class Status { RESOLVED, CANCELLED }
    }

    /** Collect until the first interrupt arrives, then suspend for the user's response. */
    public suspend fun awaitApproval(events: Flow<AguiEvent>): Pair<List<Interrupt>, CompletableDeferred<List<ApprovalResponse>>> {
        val interrupts = bridge.interrupts(events).first()
        return interrupts to CompletableDeferred()
    }
}
