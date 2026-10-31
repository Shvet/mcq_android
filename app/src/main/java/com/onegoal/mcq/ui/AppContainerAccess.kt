package com.onegoal.mcq.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.onegoal.mcq.AppContainer
import com.onegoal.mcq.McqApplication

/** Access to the app-wide dependency container from composables. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as McqApplication).container
