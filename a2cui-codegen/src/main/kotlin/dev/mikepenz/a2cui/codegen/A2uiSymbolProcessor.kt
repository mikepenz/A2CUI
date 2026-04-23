package dev.mikepenz.a2cui.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

public class A2uiSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        A2uiSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options,
        )
}

/**
 * KSP processor that scans the current compilation for `@A2uiComponent` symbols and emits a
 * single `Catalog` implementation plus companion JSON Schema / tool-prompt resources.
 *
 * Options read from `environment.options`:
 *  - `a2cui.catalogId` — catalog id string (default: `"custom"`).
 *  - `a2cui.catalogPackage` — package for the generated object (default: `"a2cui.generated"`).
 *  - `a2cui.catalogClassName` — name for the generated object (default: `"GeneratedA2cuiCatalog"`).
 */
internal class A2uiSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    private val catalogId: String get() = options["a2cui.catalogId"] ?: "custom"
    private val catalogPackage: String get() = options["a2cui.catalogPackage"] ?: "a2cui.generated"
    private val catalogClassName: String get() = options["a2cui.catalogClassName"] ?: "GeneratedA2cuiCatalog"

    private var processed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) return emptyList()

        val componentAnnotationFqn = "dev.mikepenz.a2cui.codegen.annotations.A2uiComponent"
        val symbols = resolver.getSymbolsWithAnnotation(componentAnnotationFqn).toList()
        if (symbols.isEmpty()) return emptyList()

        val specs = mutableListOf<ComponentSpec>()
        val sources = mutableSetOf<KSFile>()

        symbols.forEach { symbol ->
            val spec = when (symbol) {
                is KSPropertyDeclaration -> SpecExtractor.fromProperty(symbol, logger)
                is KSFunctionDeclaration -> {
                    logger.warn("@A2uiComponent on functions not yet supported — mark the ComponentFactory property instead", symbol)
                    null
                }
                is KSClassDeclaration -> {
                    logger.warn("@A2uiComponent on classes not yet supported — mark the ComponentFactory property instead", symbol)
                    null
                }
                else -> null
            }
            if (spec != null) {
                specs += spec
                (symbol as? KSPropertyDeclaration)?.containingFile?.let { sources.add(it) }
            }
        }

        if (specs.isEmpty()) return emptyList()

        val schema = SchemaGenerator.generate(catalogId, specs)
        val prompt = PromptGenerator.generate(catalogId, specs)
        val source = CatalogGenerator.generate(
            packageName = catalogPackage,
            className = catalogClassName,
            catalogId = catalogId,
            specs = specs,
            schema = schema,
            promptFragment = prompt,
        )

        val dependencies = Dependencies(aggregating = true, *sources.toTypedArray())
        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = catalogPackage,
            fileName = catalogClassName,
        ).use { it.write(source.encodeToByteArray()) }

        processed = true
        return emptyList()
    }
}

internal object SpecExtractor {

    private const val A2UI_COMPONENT = "dev.mikepenz.a2cui.codegen.annotations.A2uiComponent"
    private const val A2UI_PROP = "dev.mikepenz.a2cui.codegen.annotations.A2uiProp"
    private const val A2UI_EVENT = "dev.mikepenz.a2cui.codegen.annotations.A2uiEvent"
    private const val A2UI_SLOT = "dev.mikepenz.a2cui.codegen.annotations.A2uiSlot"

    fun fromProperty(property: KSPropertyDeclaration, logger: KSPLogger): ComponentSpec? {
        val annotation = property.annotations.firstOrNull { it.annotationFqn() == A2UI_COMPONENT } ?: return null
        val name = annotation.stringArg("name") ?: run {
            logger.error("@A2uiComponent requires a name", property); return null
        }
        val factoryReference = property.qualifiedName?.asString() ?: run {
            logger.error("Cannot resolve FQN for @A2uiComponent property", property); return null
        }
        return ComponentSpec(
            name = name,
            description = annotation.stringArg("description").orEmpty(),
            factoryReference = factoryReference,
            props = annotation.nestedArray("props").mapNotNull { toPropSpec(it) },
            events = annotation.nestedArray("events").mapNotNull { toEventSpec(it) },
            slots = annotation.nestedArray("slots").mapNotNull { toSlotSpec(it) },
        )
    }

    private fun toPropSpec(annotation: KSAnnotation): PropSpec? {
        if (annotation.annotationFqn() != A2UI_PROP) return null
        val name = annotation.stringArg("name") ?: return null
        val typeName = annotation.enumArg("type") ?: "STRING"
        return PropSpec(
            name = name,
            type = runCatching { PropType.valueOf(typeName) }.getOrDefault(PropType.STRING),
            description = annotation.stringArg("description").orEmpty(),
            required = annotation.boolArg("required") ?: false,
            defaultValue = annotation.stringArg("defaultValue").orEmpty(),
            enumValues = annotation.stringArray("enumValues"),
        )
    }

    private fun toEventSpec(annotation: KSAnnotation): EventSpec? {
        if (annotation.annotationFqn() != A2UI_EVENT) return null
        val name = annotation.stringArg("name") ?: return null
        return EventSpec(
            name = name,
            description = annotation.stringArg("description").orEmpty(),
            context = annotation.nestedArray("context").mapNotNull { toPropSpec(it) },
        )
    }

    private fun toSlotSpec(annotation: KSAnnotation): SlotSpec? {
        if (annotation.annotationFqn() != A2UI_SLOT) return null
        val name = annotation.stringArg("name") ?: return null
        return SlotSpec(
            name = name,
            description = annotation.stringArg("description").orEmpty(),
            multiple = annotation.boolArg("multiple") ?: true,
        )
    }
}

/** Normalises KSP annotation-argument access across forms the resolver might return. */
private fun KSAnnotation.arg(key: String): Any? =
    arguments.firstOrNull { it.name?.asString() == key }?.value

private fun KSAnnotation.annotationFqn(): String? =
    annotationType.resolve().declaration.qualifiedName?.asString()

private fun KSAnnotation.stringArg(key: String): String? = (arg(key) as? String)?.takeIf { it.isNotEmpty() }
private fun KSAnnotation.boolArg(key: String): Boolean? = arg(key) as? Boolean
private fun KSAnnotation.enumArg(key: String): String? = when (val v = arg(key)) {
    is String -> v
    else -> v?.toString()?.substringAfterLast('.')
}

@Suppress("UNCHECKED_CAST")
private fun KSAnnotation.stringArray(key: String): List<String> =
    (arg(key) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

@Suppress("UNCHECKED_CAST")
private fun KSAnnotation.nestedArray(key: String): List<KSAnnotation> =
    (arg(key) as? List<*>)?.mapNotNull { it as? KSAnnotation } ?: emptyList()
