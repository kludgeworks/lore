package com.embabel.guide

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [GuideComposition]. Base profiles are loaded from the real
 * `application-{profile}.yml` on the (main) classpath, so these assert the actual ddd foundation.
 */
class GuideCompositionTest {

    private fun props(
        extends: String?,
        referencesFile: String,
        domain: DomainConfig,
    ) = GuideProperties(
        reloadContentOnStartup = false,
        defaultPersona = "adaptive",
        projectsPath = "./projects",
        chunkerConfig = null,
        referencesFile = referencesFile,
        extends = extends,
        domain = domain,
        content = ContentConfig(
            versioned = VersionedContentConfig(baseUrl = "", versions = emptyList()),
            supplementary = emptyList(),
        ),
        toolPrefix = "",
        directories = null,
        toolGroups = emptySet(),
    )

    @Test
    fun `embabel extends ddd merges reference files base-first`() {
        val embabelDomain = DomainConfig(
            name = "Embabel",
            description = "agent framework",
            references = listOf("embabel-agent: core framework"),
            pronunciations = listOf("\"Embabel\" -> \"embaybel\""),
        )
        val composition = GuideComposition(props("ddd", "references-embabel.yml", embabelDomain))

        assertEquals(
            listOf("references-ddd.yml", "references-embabel.yml"),
            composition.referenceFiles(),
        )
    }

    @Test
    fun `embabel extends ddd merges domain references and pronunciations`() {
        val embabelDomain = DomainConfig(
            name = "Embabel",
            description = "agent framework",
            references = listOf("embabel-agent: core framework"),
            pronunciations = listOf("\"Embabel\" -> \"embaybel\""),
        )
        val domain = GuideComposition(props("ddd", "references-embabel.yml", embabelDomain)).effectiveDomain()

        // child scalar fields are preserved
        assertEquals("Embabel", domain.name)
        // ddd foundation references are inherited, embabel's own retained
        assertTrue(domain.references.any { it.contains("Spring Modulith") }) { "should inherit ddd references: ${domain.references}" }
        assertTrue(domain.references.contains("embabel-agent: core framework")) { "should keep embabel references" }
        // pronunciations merged: ddd (jMolecules) + embabel (embaybel)
        assertTrue(domain.pronunciations.any { it.contains("jMolecules") }) { "should inherit ddd pronunciations: ${domain.pronunciations}" }
        assertTrue(domain.pronunciations.any { it.contains("embaybel") }) { "should keep embabel pronunciations" }
        // no duplicates introduced
        assertEquals(domain.references.distinct(), domain.references)
        assertEquals(domain.pronunciations.distinct(), domain.pronunciations)
    }

    @Test
    fun `ddd standalone has no extends and is unchanged`() {
        val dddDomain = DomainConfig(
            name = "Domain-Driven Design & Spring Modulith",
            description = "DDD",
            references = listOf("Spring Modulith: x"),
            pronunciations = listOf("\"DDD\" -> \"dee-dee-dee\""),
        )
        val composition = GuideComposition(props(extends = null, referencesFile = "references-ddd.yml", domain = dddDomain))

        assertEquals(listOf("references-ddd.yml"), composition.referenceFiles())
        val domain = composition.effectiveDomain()
        assertEquals(dddDomain.references, domain.references)
        assertEquals(dddDomain.pronunciations, domain.pronunciations)
        assertEquals("Domain-Driven Design & Spring Modulith", domain.name)
    }
}
