package dev.mikepenz.a2cui.compose

/**
 * Mutable name → [ComponentFactory] map. The closed set of registered names is the A2CUI
 * trust boundary: incoming [dev.mikepenz.a2cui.core.ComponentNode]s whose `component` is not
 * registered render as a muted placeholder and log telemetry.
 */
public class ComponentRegistry {
    private val factories: MutableMap<String, ComponentFactory> = mutableMapOf()

    public fun register(name: String, factory: ComponentFactory): ComponentRegistry = apply {
        factories[name] = factory
    }

    public fun registerAll(catalog: Catalog): ComponentRegistry = apply {
        catalog.install(this)
    }

    public operator fun get(name: String): ComponentFactory? = factories[name]

    public operator fun contains(name: String): Boolean = name in factories

    public val names: Set<String> get() = factories.keys.toSet()
}
