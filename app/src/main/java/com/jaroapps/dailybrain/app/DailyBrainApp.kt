package com.jaroapps.dailybrain.app

import android.app.Application
import com.jaroapps.dailybrain.app.di.appModule
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import com.jaroapps.dailybrain.feature.reminders.ReminderScheduler
import com.jaroapps.dailybrain.feature.brain.BrainStateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DailyBrainApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val koinApplication = startKoin {
            androidContext(this@DailyBrainApp)
            modules(appModule)
        }

        ReminderScheduler(this).scheduleAll()
        BrainStateScheduler(this).scheduleNext()

        applicationScope.launch {
            koinApplication.koin.get<UserProfileRepository>().observeProfile().first()
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }
}
