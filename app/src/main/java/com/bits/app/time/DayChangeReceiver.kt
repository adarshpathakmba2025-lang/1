package com.bits.app.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bits.app.data.BitsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val repository = BitsRepository.get(context)
                repository.load()
                repository.refreshWidgetsNow()
                MidnightScheduler.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
