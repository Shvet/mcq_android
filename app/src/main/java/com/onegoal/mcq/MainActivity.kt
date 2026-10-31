package com.onegoal.mcq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.onegoal.mcq.ads.InterstitialAds
import com.onegoal.mcq.ui.AppViewModel
import com.onegoal.mcq.ui.navigation.McqNavHost
import com.onegoal.mcq.ui.theme.McqColors
import com.onegoal.mcq.ui.theme.McqTheme
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels {
        viewModelFactory {
            initializer {
                val container = (application as McqApplication).container
                AppViewModel(container.repository, container.preferences)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(McqColors.PrimaryDark.toArgb()))
        super.onCreate(savedInstanceState)
        val interstitialAds = InterstitialAds(this)
        setContent {
            McqTheme {
                McqNavHost(
                    appViewModel = appViewModel,
                    interstitialAds = interstitialAds,
                    onExit = ::finish,
                )
            }
        }
    }
}
