package com.embabel.guide.references

import com.embabel.agent.api.reference.LlmReference
import com.embabel.agent.api.reference.LlmReferenceProvider
import com.embabel.coding.tools.git.RepositoryReferenceProvider

/**
 * A code-browsing reference like [com.embabel.coding.tools.git.GitHubRepository], but cloned at an
 * explicit git ref (tag, branch or commit) so the version is pinned and reproducible.
 *
 * The upstream `GitHubRepository` provider hard-codes the default branch HEAD; this passes the ref
 * through to [RepositoryReferenceProvider.cloneRepository], whose `branch` parameter accepts any ref.
 * A null/blank [ref] falls back to the repository default branch (same behaviour as GitHubRepository).
 *
 * Referenced from `references-*.yml` by fully-qualified name, e.g.:
 * ```yaml
 * - fqn: com.embabel.guide.references.PinnedGitHubRepository
 *   url: https://github.com/TNG/ArchUnit.git
 *   ref: v1.4.2
 *   description: ArchUnit — architecture testing library for Java.
 * ```
 */
data class PinnedGitHubRepository(
    val url: String,
    val description: String,
    val ref: String? = null,
) : LlmReferenceProvider {

    private val reference: LlmReference =
        RepositoryReferenceProvider.create().cloneRepository(url, description, ref?.ifBlank { null })

    override fun reference(): LlmReference = reference
}
