package com.embabel.guide

import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * Resolves the `guide.extends` inheritance chain so a profile (e.g. `embabel`) can build on a base
 * profile (e.g. `ddd`) rather than being a parallel alternative.
 *
 * Inheritance is **additive for the capability/branding layer only**:
 *  - reference tool files (`references-*.yml`)
 *  - `guide.domain.references` (system-prompt branding bullets)
 *  - `guide.domain.pronunciations` (TTS rules)
 *
 * It deliberately does NOT inherit the ingested corpus (`guide.content`, `guide.repositories`), the
 * Neo4j database, or the scalar domain fields (`name`, `description`, `toolGuidance`) — those stay
 * child-owned, so each profile keeps its own isolated vector store.
 *
 * Base profiles are read directly from their `application-{profile}.yml` on the classpath (only the
 * `guide.references-file`, `guide.domain` and `guide.extends` subtrees are bound). Note: Spring
 * placeholders (`${...}`) in those subtrees are not resolved here, but the curated domain/reference
 * config contains none.
 */
@Component
class GuideComposition(
    private val guideProperties: GuideProperties,
) {

    private data class Layer(
        val extends: String?,
        val referencesFile: String?,
        val domain: DomainConfig?,
    )

    private val yamlLoader = YamlPropertySourceLoader()

    /** Base-first: `[ddd, embabel]` for the embabel profile; `[ddd]` for the standalone ddd profile. */
    private val chain: List<Layer> by lazy { resolveChain() }

    private val cachedDomain: DomainConfig by lazy { computeEffectiveDomain() }

    /** Reference (`.yml`) files to load, base profile first, de-duplicated. */
    fun referenceFiles(): List<String> = chain.mapNotNull { it.referencesFile }.distinct()

    /**
     * The active profile's domain, with `references` and `pronunciations` prepended from the extends
     * chain (base first), de-duplicated. Scalar fields remain the active profile's own.
     */
    fun effectiveDomain(): DomainConfig = cachedDomain

    private fun computeEffectiveDomain(): DomainConfig {
        val leaf = guideProperties.domain
        val bases = chain.dropLast(1) // everything but the active (leaf) profile
        val baseReferences = bases.flatMap { it.domain?.references ?: emptyList() }
        val basePronunciations = bases.flatMap { it.domain?.pronunciations ?: emptyList() }
        return leaf.copy(
            references = (baseReferences + leaf.references).distinct(),
            pronunciations = (basePronunciations + leaf.pronunciations).distinct(),
        )
    }

    private fun resolveChain(): List<Layer> {
        val leaf = Layer(guideProperties.extends, guideProperties.referencesFile, guideProperties.domain)
        val layers = mutableListOf(leaf)
        val seen = mutableSetOf<String>()
        var next = leaf.extends
        while (next != null) {
            check(seen.add(next)) { "guide.extends cycle detected at profile '$next'" }
            val base = loadLayer(next)
            layers.add(base)
            next = base.extends
        }
        return layers.asReversed().toList() // base-first
    }

    private fun loadLayer(profile: String): Layer {
        val resource = ClassPathResource("application-$profile.yml")
        require(resource.exists()) {
            "guide.extends references unknown profile '$profile' (no application-$profile.yml on the classpath)"
        }
        val sources: List<PropertySource<*>> = yamlLoader.load("application-$profile", resource)
        val binder = Binder(ConfigurationPropertySources.from(sources))
        return Layer(
            extends = binder.bind("guide.extends", Bindable.of(String::class.java)).orElse(null),
            referencesFile = binder.bind("guide.references-file", Bindable.of(String::class.java)).orElse(null),
            domain = binder.bind("guide.domain", Bindable.of(DomainConfig::class.java)).orElse(null),
        )
    }
}
