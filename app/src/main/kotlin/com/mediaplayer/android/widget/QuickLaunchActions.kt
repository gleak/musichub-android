package com.mediaplayer.android.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import com.mediaplayer.android.MainActivity

/**
 * Builds tap actions for [QuickLaunchWidget] tiles. The tap fires
 * [LaunchCallback] which then starts [MainActivity] with
 * [WIDGET_LAUNCH_ACTION] + [WIDGET_LAUNCH_KIND_EXTRA] — the activity
 * intercepts and routes to the playback view-model.
 *
 * Going through an `ActionCallback` (rather than `actionStartActivity`
 * with an `Intent`) keeps the kind name in the action parameter map,
 * which Glance serializes alongside the widget instance state. The Intent
 * is only constructed at tap time inside the callback.
 */
object QuickLaunchActions {
    const val WIDGET_LAUNCH_ACTION = "com.mediaplayer.android.WIDGET_LAUNCH"
    const val WIDGET_LAUNCH_KIND_EXTRA = "widget_launch_kind"

    val KIND_KEY: ActionParameters.Key<String> = ActionParameters.Key("kind")

    fun launchAction(context: Context, kind: QuickLaunchKind): Action =
        actionRunCallback<LaunchCallback>(actionParametersOf(KIND_KEY to kind.name))
}

class LaunchCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val kindName = parameters[QuickLaunchActions.KIND_KEY] ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            action = QuickLaunchActions.WIDGET_LAUNCH_ACTION
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(QuickLaunchActions.WIDGET_LAUNCH_KIND_EXTRA, kindName)
        }
        context.startActivity(intent)
    }
}
