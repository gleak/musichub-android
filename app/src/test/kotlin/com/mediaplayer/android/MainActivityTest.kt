package com.mediaplayer.android

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * The activity is the app's front door. Before anything is composed it
 * consumes the intent it was launched with — a share link, or a widget's
 * quick-launch extra — and then hands off to the auth gate.
 *
 * These launch it for real through each of those doors. With no credential
 * on the device the gate settles on the signed-out branch, which is where
 * a cold first launch lands and the state that must not come up blank.
 *
 * The signed-in half of the navigation graph is not reachable from here:
 * it needs a real session, and the only way to one is Credential Manager.
 * That is a separate harness rather than more of this one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = Application::class,
    qualifiers = "w411dp-h891dp-xhdpi",
)
class MainActivityTest {

    private lateinit var api: MediaPlayerApi

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        MediaPlayerApp.contextOverride = context
        ReadCache.init(context)
        EventQueue.init(context)
        runBlocking { ReadCache.clearAll() }
        api = mockk(relaxed = true)
        Network.apiOverride = api
    }

    @After
    fun tearDown() {
        Network.apiOverride = null
        MediaPlayerApp.contextOverride = null
    }

    private fun launch(intent: Intent? = null): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val target = intent ?: Intent(context, MainActivity::class.java)
        target.setClass(context, MainActivity::class.java)
        return ActivityScenario.launch<MainActivity>(target).also { shadowMainLooper().idle() }
    }

    @Test
    fun `a plain launch reaches the resumed state`() {
        launch().use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            scenario.onActivity { assertNotNull(it) }
        }
    }

    /**
     * A shared playlist link opens the app through ACTION_VIEW. The token
     * has to survive into the composition, so the launch must not fall over
     * on an intent carrying data instead of nothing.
     */
    @Test
    fun `a share link launch comes up`() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://q-musichub.duckdns.org/p/share-token-123")
        }

        launch(intent).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /** A malformed share link must not take the front door down with it. */
    @Test
    fun `a share link with no token comes up`() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://q-musichub.duckdns.org/p/")
        }

        launch(intent).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun `a widget quick-launch comes up`() {
        val intent = Intent().apply { putExtra("quick_launch_kind", "LIKED") }

        launch(intent).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun `an unrecognised widget extra comes up`() {
        val intent = Intent().apply { putExtra("quick_launch_kind", "SOMETHING_NEW") }

        launch(intent).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /** Rotation and process death both go through recreate. */
    @Test
    fun `recreating the activity does not throw`() {
        launch().use { scenario ->
            scenario.recreate()
            shadowMainLooper().idle()

            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun `the activity can be backgrounded and resumed`() {
        launch().use { scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)
            shadowMainLooper().idle()
            scenario.moveToState(Lifecycle.State.RESUMED)
            shadowMainLooper().idle()

            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
