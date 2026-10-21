package com.jaroapps.dailybrain.feature.brain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class BrainStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BrainStateScheduler.ACTION_REFRESH) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                GlobalContext.get().get<BrainStateManager>().refresh()
            } catch (_: Exception) {
                // Keep the receiver resilient to transient initialization/database failures.
            } finally {
                BrainStateScheduler(appContext).scheduleNext()
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
