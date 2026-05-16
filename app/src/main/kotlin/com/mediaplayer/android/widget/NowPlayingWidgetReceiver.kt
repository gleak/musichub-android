package com.mediaplayer.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@UnstableApi
class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SHARED_WIDGET

    /**
     * While at least one widget instance is bound, listen to [WidgetState]
     * and re-render whenever the playback snapshot changes. Avoids needing
     * the playback service to know about widgets at all — service writes
     * to [WidgetState], the receiver picks it up here.
     *
     * The first emission is dropped because [GlanceAppWidget.update] is
     * already implicitly called when a widget is added; pushing the cold
     * StateFlow value would no-op anyway, but `drop(1)` keeps logs clean.
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startObservingOnce(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        startObservingOnce(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        stopObserving()
    }

    companion object {
        // Single widget instance shared across every receiver invocation.
        // Each onReceive() previously allocated a fresh GlanceAppWidget per
        // broadcast — fine for state-less widgets, but combined with the
        // collector below it meant every system APPWIDGET_UPDATE stacked a
        // new infinite collector retaining a new widget instance.
        private val SHARED_WIDGET: GlanceAppWidget = NowPlayingWidget()

        private val observerLock = Any()
        private var observerScope: CoroutineScope? = null

        // BroadcastReceiver instances are recycled per broadcast — anything
        // stored on `this` leaks because the OS keeps re-instantiating the
        // class. The collector lives in the companion (process-scoped) so
        // there's only ever one active collector at a time regardless of
        // how many system broadcasts arrive.
        private fun startObservingOnce(context: Context) {
            synchronized(observerLock) {
                if (observerScope != null) return
                val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                observerScope = s
                val app = context.applicationContext
                s.launch {
                    WidgetState.now.drop(1).collect {
                        runCatching { SHARED_WIDGET.updateAll(app) }
                    }
                }
            }
        }

        private fun stopObserving() {
            synchronized(observerLock) {
                observerScope?.cancel()
                observerScope = null
            }
        }
    }
}
