package com.mediaplayer.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The changelog sheet auto-opens when the stored last-seen version differs from
 * [AppVersion.VERSION], so a release whose version was bumped without a matching
 * entry shows returning users an empty "what's new". These tests turn the
 * project's release checklist into something that fails the build instead of
 * being remembered.
 */
class ChangelogTest {

    @Test
    fun `the current version has a changelog entry at the top`() {
        assertEquals(
            "AppVersion.VERSION must match the newest changelog entry",
            AppVersion.VERSION,
            Changelog.entries.first().version,
        )
    }

    @Test
    fun `versions are unique`() {
        val versions = Changelog.entries.map { it.version }

        assertEquals(versions.size, versions.toSet().size)
    }

    /** Newest first — the sheet renders them in list order. */
    @Test
    fun `entries are ordered newest first`() {
        val ordered = Changelog.entries.sortedByDescending { semver(it.version) }

        assertEquals(
            Changelog.entries.map { it.version },
            ordered.map { it.version },
        )
    }

    @Test
    fun `every version is valid semver`() {
        for (entry in Changelog.entries) {
            assertTrue(
                "not MAJOR.MINOR.PATCH: ${entry.version}",
                SEMVER.matches(entry.version),
            )
        }
    }

    @Test
    fun `every entry has a title and at least one highlight`() {
        for (entry in Changelog.entries) {
            assertTrue("blank title for ${entry.version}", entry.title.isNotBlank())
            assertTrue("no highlights for ${entry.version}", entry.highlights.isNotEmpty())
            assertTrue(
                "blank highlight in ${entry.version}",
                entry.highlights.none { it.isBlank() },
            )
        }
    }

    /**
     * Highlights are user-facing copy. Leaking an identifier here is the
     * documented failure mode — the project rule spells out that file paths,
     * class names and JSON keys must never reach this text.
     *
     * URL schemes are deliberately not on the list: entries that explain
     * shareable links to the user quote them on purpose.
     */
    @Test
    fun `highlights read as prose, not as internals`() {
        val giveaways = listOf(".kt", "()", "_ID", "TODO", "FIXME")

        for (entry in Changelog.entries) {
            for (highlight in entry.highlights) {
                for (giveaway in giveaways) {
                    assertTrue(
                        "${entry.version} highlight leaks \"$giveaway\": $highlight",
                        !highlight.contains(giveaway),
                    )
                }
            }
        }
    }

    /** Collapses MAJOR.MINOR.PATCH into one orderable number. */
    private fun semver(v: String): Int {
        val parts = v.split('.').map { it.toIntOrNull() ?: 0 }
        return parts.getOrElse(0) { 0 } * 1_000_000 +
            parts.getOrElse(1) { 0 } * 1_000 +
            parts.getOrElse(2) { 0 }
    }

    private companion object {
        val SEMVER = Regex("""^\d+\.\d+\.\d+$""")
    }
}
