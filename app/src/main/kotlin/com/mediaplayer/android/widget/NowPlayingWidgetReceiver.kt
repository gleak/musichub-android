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

    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()

    private var scope: CoroutineScope? = null

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
        startObserving(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (scope == null) startObserving(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        scope?.cancel()
        scope = null
    }

    private fun startObserving(context: Context) {
        if (scope != null) return
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        val app = context.applicationContext
        s.launch {
            WidgetState.now.drop(1).collect {
                runCatching { (glanceAppWidget as NowPlayingWidget).updateAll(app) }
            }
        }
    }
}
