package com.onegoal.mcq.ui.components

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onegoal.mcq.R
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.util.Intents
import kotlinx.coroutines.launch

/** Navigation drawer shared by the home and bookmark screens. */
@Composable
fun DrawerHost(
    config: AppConfig,
    bookmarkCount: Int,
    selected: DrawerDestination,
    onHome: () -> Unit,
    onBookmarks: () -> Unit,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable (openDrawer: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                title = config.splashTitle ?: stringResource(R.string.splash_title),
                logoUrl = config.logoUrl,
                bookmarkCount = bookmarkCount,
                selected = selected,
                onSelect = { destination ->
                    scope.launch { drawerState.close() }
                    when (destination) {
                        DrawerDestination.Home -> if (selected != DrawerDestination.Home) onHome()
                        DrawerDestination.Bookmarks -> if (selected != DrawerDestination.Bookmarks) onBookmarks()
                        DrawerDestination.Share -> Intents.shareApp(context, config.shareMessage)
                        DrawerDestination.Rate -> Intents.rateApp(context)
                        DrawerDestination.Feedback -> Intents.sendFeedback(context, config.feedbackEmail)
                    }
                },
            )
        },
    ) {
        content { scope.launch { drawerState.open() } }
    }
}
