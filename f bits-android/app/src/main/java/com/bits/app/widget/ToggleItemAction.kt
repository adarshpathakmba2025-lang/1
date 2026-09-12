package com.bits.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.bits.app.data.BitsRepository
import com.bits.app.data.toggleItem

/** Tapping an item on the widget marks it done, or reopens it. */
class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ItemIdKey] ?: return
        BitsRepository.get(context).editNow { it.toggleItem(itemId) }
    }

    companion object {
        val ItemIdKey = ActionParameters.Key<String>("itemId")
    }
}
