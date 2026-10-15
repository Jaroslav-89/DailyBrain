package com.jaroapps.dailybrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jaroapps.dailybrain.navigation.AppNavHost
import com.jaroapps.dailybrain.ui.theme.DailyBrainTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyBrainTheme {
                AppNavHost()
            }
        }
    }
}