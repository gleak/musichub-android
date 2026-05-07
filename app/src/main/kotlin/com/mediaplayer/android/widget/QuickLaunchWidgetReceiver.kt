package com.mediaplayer.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
class QuickLaunchWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = QuickLaunchWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Drop persisted tile selections for the widgets the user removed
        // so reusing the same appWidgetId later starts fresh.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            appWidgetIds.forEach { QuickLaunchPrefs.remove(context, it) }
        }
    }
}
