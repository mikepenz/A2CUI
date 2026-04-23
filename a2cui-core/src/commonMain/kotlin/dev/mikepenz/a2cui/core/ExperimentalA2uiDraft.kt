package dev.mikepenz.a2cui.core

/**
 * Opt-in marker for A2CUI experimental extensions beyond the stable A2UI v0.9 wire spec.
 *
 * ### Context
 *
 * A2UI v0.9 is the spec version this library is feature-complete against. A2CUI occasionally
 * ships additional primitives to address common SDUI needs that v0.9 does not cover — today
 * that includes multi-op data-model patches, server-initiated scroll hints, client viewport
 * reports, and conditional property values. These primitives are **not** part of the published
 * A2UI spec; they are A2CUI proposals we intend to contribute upstream when the spec evolves.
 *
 * ### Stability contract
 *
 * Wire shapes of types gated behind this annotation may change without notice in any release
 * until either
 *  - the corresponding primitive is accepted into a future A2UI spec version, or
 *  - A2CUI reaches 1.0 and pins the current shape as a stable extension.
 *
 * Consumers who agree to that risk can opt in site-locally
 * (`@OptIn(ExperimentalA2uiDraft::class)`) or propagate the marker on their own declarations.
 *
 * ### Published A2UI spec index
 *  - v0.8 — current stable: https://a2ui.org/specification/v0.8-a2ui/
 *  - v0.9 — published draft: https://a2ui.org/specification/v0.9-a2ui/
 *
 * No v0.10 draft is published at the time of writing.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is an A2CUI experimental extension — not part of the published A2UI spec. Wire shapes may change.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalA2uiDraft
