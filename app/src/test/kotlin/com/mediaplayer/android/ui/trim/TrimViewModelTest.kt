package com.mediaplayer.android.ui.trim

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * The trim editor's two markers must never cross or close to nothing —
 * everything the user can do to them (drag, nudge, snap to silence) goes
 * through clamps that keep IN before OUT with a minimum window between.
 * Get that wrong and the app asks the backend to cut a negative-length
 * region.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TrimViewModelTest {

    private val totalMs = 200_000L
    private lateinit var api: MediaPlayerApi

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        MediaPlayerApp.contextOverride = ApplicationProvider.getApplicationContext<Application>()
        api = mockk(relaxed = true)
        Network.apiOverride = api
    }

    @After
    fun tearDown() {
        Network.apiOverride = null
        MediaPlayerApp.contextOverride = null
        Dispatchers.resetMain()
    }

    private fun viewModel(durationMs: Long = totalMs) =
        TrimViewModel(sourceSongId = 42L, totalDurationMs = durationMs)

    @Test
    fun `the markers open inside the track, not on its edges`() {
        val vm = viewModel()

        assertTrue(vm.inMs.value > 0L)
        assertTrue(vm.outMs.value < totalMs)
        assertTrue(vm.inMs.value < vm.outMs.value)
    }

    @Test
    fun `moving IN before the start is clamped to zero`() {
        val vm = viewModel()

        vm.setIn(-5_000L)

        assertEquals(0L, vm.inMs.value)
    }

    @Test
    fun `moving OUT past the end is clamped to the duration`() {
        val vm = viewModel()

        vm.setOut(totalMs + 60_000L)

        assertEquals(totalMs, vm.outMs.value)
    }

    /** IN cannot be pushed past OUT — the window would invert. */
    @Test
    fun `IN cannot cross OUT`() {
        val vm = viewModel()
        vm.setOut(100_000L)

        vm.setIn(150_000L)

        assertTrue(vm.inMs.value < vm.outMs.value)
        assertEquals(TrimViewModel.MIN_WINDOW_MS, vm.outMs.value - vm.inMs.value)
    }

    @Test
    fun `OUT cannot cross IN`() {
        val vm = viewModel()
        vm.setIn(100_000L)

        vm.setOut(50_000L)

        assertTrue(vm.outMs.value > vm.inMs.value)
        assertEquals(TrimViewModel.MIN_WINDOW_MS, vm.outMs.value - vm.inMs.value)
    }

    @Test
    fun `nudging moves a marker by the given delta`() {
        val vm = viewModel()
        vm.setIn(50_000L)

        vm.nudgeIn(1_000L)

        assertEquals(51_000L, vm.inMs.value)
    }

    @Test
    fun `nudging respects the same clamps as dragging`() {
        val vm = viewModel()
        vm.setIn(0L)

        vm.nudgeIn(-1_000L)

        assertEquals(0L, vm.inMs.value)
    }

    @Test
    fun `fade and loop are toggles`() {
        val vm = viewModel()
        val fadeWasOn = vm.fadeEnabled.value

        vm.toggleFade()
        vm.toggleAbLoop()

        assertEquals(!fadeWasOn, vm.fadeEnabled.value)
        assertTrue(vm.abLoopEnabled.value)
    }

    /**
     * Snapping looks for the quietest point near each handle, so a cut
     * lands in silence rather than mid-note.
     */
    @Test
    fun `snapping moves the markers towards a quiet point`() {
        val vm = viewModel()
        vm.setIn(50_000L)
        vm.setOut(150_000L)
        // A loud track with one near-silent bar close to the IN handle.
        val waveform = FloatArray(100) { 0.8f }
        waveform[26] = 0.01f

        vm.snapToSilence(waveform)

        // Bar 26 of 100 over 200s is ~53s; the handle should have moved there.
        assertTrue("in was ${vm.inMs.value}", vm.inMs.value in 52_000L..54_000L)
    }

    @Test
    fun `snapping on an empty waveform does nothing`() {
        val vm = viewModel()
        vm.setIn(50_000L)
        val before = vm.inMs.value

        vm.snapToSilence(FloatArray(0))

        assertEquals(before, vm.inMs.value)
    }

    @Test
    fun `saving sends the chosen window to the backend`() {
        val cut = SongDto(
            id = 99L, title = "Breed (cut)", artist = "Nirvana", album = null,
            durationMs = 60_000L, hasCoverArt = false,
        )
        coEvery { api.cutSong(any(), any()) } returns cut
        val vm = viewModel()
        vm.setIn(10_000L)
        vm.setOut(70_000L)

        vm.save()
        shadowMainLooper().idle()

        val state = vm.saveState.value
        assertTrue("was $state", state is TrimSaveState.Saved)
        assertEquals(99L, (state as TrimSaveState.Saved).newSong.id)
    }

    /**
     * A track shorter than the minimum window has no valid region to cut.
     * That is refused locally rather than sent — the backend would reject
     * it and the user would wait for a round trip to learn something the
     * client already knew.
     */
    @Test
    fun `a track too short to cut is refused without a request`() {
        val short = viewModel(durationMs = 500L)

        short.save()

        assertTrue(short.saveState.value is TrimSaveState.Failed)
        io.mockk.coVerify(exactly = 0) { api.cutSong(any(), any()) }
    }

    @Test
    fun `a failed save is reported rather than swallowed`() {
        coEvery { api.cutSong(any(), any()) } throws java.io.IOException("offline")
        val vm = viewModel()

        vm.save()
        shadowMainLooper().idle()

        assertTrue(vm.saveState.value is TrimSaveState.Failed)
    }

    @Test
    fun `a save already in flight is not started twice`() {
        val vm = viewModel()
        coEvery { api.cutSong(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(10_000)
            error("unreachable")
        }

        vm.save()
        val duringFirst = vm.saveState.value
        vm.save()

        assertTrue(duringFirst is TrimSaveState.Saving)
        assertTrue(vm.saveState.value is TrimSaveState.Saving)
    }

    @Test
    fun `the save state can be consumed back to idle`() {
        coEvery { api.cutSong(any(), any()) } throws java.io.IOException("offline")
        val vm = viewModel()
        vm.save()
        shadowMainLooper().idle()

        vm.consumeSaveState()

        assertEquals(TrimSaveState.Idle, vm.saveState.value)
    }

    /** Replacing only makes sense once something has been saved. */
    @Test
    fun `replacing before a save is a no-op`() {
        val vm = viewModel()

        vm.replaceOriginalInPlaylists()

        assertEquals(TrimSaveState.Idle, vm.saveState.value)
    }

    @Test
    fun `a short track still yields a valid window`() {
        val vm = viewModel(durationMs = 3_000L)

        assertTrue(vm.outMs.value > vm.inMs.value)
        assertFalse(vm.outMs.value - vm.inMs.value < TrimViewModel.MIN_WINDOW_MS)
    }

    /**
     * The OUT marker is a position in the audio, so it can never sit past
     * the end of it — the editor has nowhere to draw it and the cut would
     * run off the file.
     */
    @Test
    fun `the OUT marker never starts past the end of the track`() {
        listOf(500L, 1_000L, 3_000L, 200_000L).forEach { duration ->
            val vm = viewModel(durationMs = duration)
            assertTrue(
                "out ${vm.outMs.value} exceeds duration $duration",
                vm.outMs.value <= duration,
            )
        }
    }
}
