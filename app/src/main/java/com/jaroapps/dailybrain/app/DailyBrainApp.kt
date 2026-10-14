package com.jaroapps.dailybrain.app

import android.app.Application
import com.jaroapps.dailybrain.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DailyBrainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@DailyBrainApp)
            modules(appModule)
        }
    }
}