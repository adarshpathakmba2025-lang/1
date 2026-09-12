package com.bits.app.time

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

/**
 * Asks Android to wake Bits shortly after midnight so the widget shows the new day.
 * Uses an inexact window, which needs no special permission. If the phone delays it,
 * the day transition still happens the next time the app or widget reads data.
 */
object MidnightScheduler {
    private const val ACTION = "com.bits.app.action.DAY_CHANGED"
    private const val WINDOW_MS = 10 * 60 * 1000L

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val nextMidnight = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val intent = Intent(context, DayChangeReceiver::class.java).setAction(ACTION)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, nextMidnight + 1000, WINDOW_MS, pendingIntent)
    }
}
