package com.mediaplayer.android.data.dto

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The DTOs are the contract with the backend, and the client decodes with
 * `ignoreUnknownKeys` and `explicitNulls = false` — which means a field the
 * server stops sending does not fail loudly, it silently becomes its default.
 * These tests pin the defaults that decision makes load-bearing.
 */
class DtoSerializationTest {

    /** Same configuration the app's Retrofit converter uses. */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun `a song decodes from the minimal server payload`() {
        val song = json.decodeFromString(
            SongDto.serializer(),
            """{"id":1,"title":"Vivere","artist":"De André","durationMs":215000,"hasCoverArt":true}""",
        )

        assertEquals(1L, song.id)
        assertEquals("Vivere", song.title)
        assertNull("album is optional", song.album)
        assertFalse("video defaults off", song.hasVideo)
        assertTrue("songs are playable unless told otherwise", song.playable)
    }

    /**
     * `playable` defaulting to true is the risky one: a server that stops
     * sending it turns every song playable again rather than hiding it.
     */
    @Test
    fun `an unplayable song survives the round trip`() {
        val original = SongDto(
            id = 9,
            title = "T",
            artist = "A",
            album = "Al",
            durationMs = 1,
            hasCoverArt = false,
            hasVideo = true,
            playable = false,
        )

        val decoded = json.decodeFromString(SongDto.serializer(), json.encodeToString(SongDto.serializer(), original))

        assertEquals(original, decoded)
    }

    @Test
    fun `unknown server fields are ignored rather than fatal`() {
        val song = json.decodeFromString(
            SongDto.serializer(),
            """{"id":1,"title":"T","artist":"A","durationMs":1,"hasCoverArt":false,"brandNewField":42}""",
        )

        assertEquals(1L, song.id)
    }

    @Test
    fun `a page envelope decodes with its items`() {
        val page = json.decodeFromString(
            PageResponse.serializer(SongDto.serializer()),
            """{"items":[{"id":1,"title":"T","artist":"A","durationMs":1,"hasCoverArt":false}],
                "page":2,"size":50,"totalItems":120,"totalPages":3}""",
        )

        assertEquals(1, page.items.size)
        assertEquals(2, page.page)
        assertEquals(120L, page.totalItems)
        assertEquals(3, page.totalPages)
    }

    @Test
    fun `an empty page is representable`() {
        val page = json.decodeFromString(
            PageResponse.serializer(SongDto.serializer()),
            """{"items":[],"page":0,"size":50,"totalItems":0,"totalPages":0}""",
        )

        assertTrue(page.items.isEmpty())
    }

    @Test
    fun `a user decodes without optional identity fields`() {
        val user = json.decodeFromString(UserDto.serializer(), """{"id":5}""")

        assertEquals(5L, user.id)
        assertNull(user.email)
        assertNull(user.name)
        assertFalse("onboarding must not be assumed done", user.onboardingComplete)
    }

    @Test
    fun `a user round-trips`() {
        val original = UserDto(id = 5, email = "a@b.c", name = "Antonio", onboardingComplete = true)

        assertEquals(
            original,
            json.decodeFromString(UserDto.serializer(), json.encodeToString(UserDto.serializer(), original)),
        )
    }

    /**
     * The update manifest is the one DTO whose defaults have a security
     * consequence: an absent checksum used to mean "install unverified".
     * The empty default is still there, so the refusal has to live in the
     * policy, not in the wire format.
     */
    @Test
    fun `an update manifest without a checksum decodes to a blank one`() {
        val manifest = json.decodeFromString(
            AppUpdateDto.serializer(),
            """{"version":"1.0.0","versionCode":10,"url":"https://example.test/a.apk"}""",
        )

        assertEquals("", manifest.sha256)
        assertEquals("", manifest.releaseNotes)
        assertFalse(manifest.required)
    }

    @Test
    fun `a full update manifest round-trips`() {
        val original = AppUpdateDto(
            version = "1.2.3",
            versionCode = 42,
            url = "https://example.test/a.apk",
            sha256 = "a".repeat(64),
            releaseNotes = "Note",
            required = true,
        )

        assertEquals(
            original,
            json.decodeFromString(AppUpdateDto.serializer(), json.encodeToString(AppUpdateDto.serializer(), original)),
        )
    }

    @Test
    fun `lists of songs decode as a whole`() {
        val songs = json.decodeFromString(
            ListSerializer(SongDto.serializer()),
            """[{"id":1,"title":"A","artist":"X","durationMs":1,"hasCoverArt":false},
                {"id":2,"title":"B","artist":"Y","durationMs":2,"hasCoverArt":true}]""",
        )

        assertEquals(listOf(1L, 2L), songs.map { it.id })
    }
}
