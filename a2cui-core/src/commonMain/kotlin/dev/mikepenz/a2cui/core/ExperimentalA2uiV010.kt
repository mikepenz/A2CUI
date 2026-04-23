package dev.mikepenz.a2cui.core

/**
 * Opt-in marker for A2UI v0.10 draft primitives. v0.9 is the current stable wire spec; any
 * v0.10 types that land ahead of the spec firming up are gated behind this annotation so
 * consumers explicitly acknowledge that their wire shape may change.
 *
 * Usage:
 *  - Annotate the v0.10 API with `@ExperimentalA2uiV010`.
 *  - Callers must either opt in at the use site (`@OptIn(ExperimentalA2uiV010::class)`) or
 *    propagate the marker on their own declarations.
 *
 * See the v0.10 draft at https://a2ui.org/specification/ for the primitives under
 * consideration — wire shapes in this module may be revised when the draft advances.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is an A2UI v0.10 draft primitive. Its wire shape may change when the draft firms up.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalA2uiV010
