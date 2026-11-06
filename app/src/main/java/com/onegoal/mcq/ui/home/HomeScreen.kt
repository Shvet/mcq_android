package com.onegoal.mcq.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onegoal.mcq.R
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.model.QuizNode
import com.onegoal.mcq.model.ReadingPosition
import com.onegoal.mcq.ui.components.BannerAd
import com.onegoal.mcq.ui.components.BannerSize
import com.onegoal.mcq.ui.components.ContentList
import com.onegoal.mcq.ui.components.DrawerDestination
import com.onegoal.mcq.ui.components.DrawerHost
import com.onegoal.mcq.ui.components.LastReadInfoDialog
import com.onegoal.mcq.ui.components.McqTopBar
import com.onegoal.mcq.ui.components.OverflowAction
import com.onegoal.mcq.ui.components.SupportDialog
import com.onegoal.mcq.ui.list.ContentListViewModel
import com.onegoal.mcq.ui.quiz.QuizListViewModel
import com.onegoal.mcq.ui.quiz.QuizNodeList
import com.onegoal.mcq.ui.theme.McqColors
import com.onegoal.mcq.util.Intents
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: ContentListViewModel,
    quizViewModel: QuizListViewModel,
    config: AppConfig,
    bookmarkCount: Int,
    lastRead: ReadingPosition?,
    isLastReadHintShown: suspend () -> Boolean,
    onLastReadHintShown: () -> Unit,
    onOpenItem: (ContentItem) -> Unit,
    onOpenQuiz: (QuizNode) -> Unit,
    onResume: (ReadingPosition) -> Unit,
    onBookmarks: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val quizState by quizViewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.Pdf) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showSupport by remember { mutableStateOf(false) }
    var showLastReadHint by remember { mutableStateOf(false) }
    var showNothingToResume by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = drawerState.isClosed) { showSupport = true }

    DrawerHost(
        config = config,
        bookmarkCount = bookmarkCount,
        selected = DrawerDestination.Home,
        onHome = {},
        onBookmarks = onBookmarks,
        drawerState = drawerState,
    ) { openDrawer ->
        Scaffold(
            topBar = {
                McqTopBar(
                    title = config.toolbarTitle ?: stringResource(R.string.toolbar_title),
                    onNavigation = openDrawer,
                    showBack = false,
                    onHome = { if (selectedTab == HomeTab.Pdf) viewModel.load() else quizViewModel.load() },
                    overflow = buildList {
                        add(OverflowAction(context.getString(R.string.menu_share)) { Intents.shareApp(context, config.shareMessage) })
                        add(OverflowAction(context.getString(R.string.menu_rate)) { Intents.rateApp(context) })
                        config.moreAppsUrl?.let { url -> add(OverflowAction(context.getString(R.string.menu_more_apps)) { Intents.openUrl(context, url) }) }
                        config.privacyPolicyUrl?.let { url -> add(OverflowAction(context.getString(R.string.menu_privacy)) { Intents.openUrl(context, url) }) }
                    },
                )
            },
            floatingActionButton = {
                if (selectedTab == HomeTab.Pdf) FloatingActionButton(
                    onClick = {
                        scope.launch {
                            when {
                                !isLastReadHintShown() -> {
                                    showLastReadHint = true
                                    onLastReadHintShown()
                                }
                                lastRead != null -> onResume(lastRead)
                                else -> showNothingToResume = true
                            }
                        }
                    },
                    shape = CircleShape,
                    containerColor = McqColors.PrimaryDark,
                    contentColor = Color.Black,
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(Icons.Filled.Repeat, contentDescription = stringResource(R.string.open_last_pdf))
                }
            },
            containerColor = Color.White,
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                HomeTabs(selected = selectedTab, onSelect = { selectedTab = it })
                when (selectedTab) {
                    HomeTab.Pdf -> ContentList(
                        state = state,
                        onRetry = viewModel::load,
                        onItemClick = onOpenItem,
                        trailingLabel = { index, _ -> (index + 1).toString() },
                        modifier = Modifier.weight(1f),
                    )
                    HomeTab.Mcq -> QuizNodeList(
                        state = quizState,
                        onRetry = quizViewModel::load,
                        onOpen = onOpenQuiz,
                        modifier = Modifier.weight(1f),
                    )
                }
                BannerAd(
                    enabled = config.adsEnabled,
                    adUnitId = config.bannerAdUnitId,
                    size = BannerSize.MediumRectangle,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }

    if (showLastReadHint) {
        LastReadInfoDialog(
            title = config.lastReadTitle ?: stringResource(R.string.last_read_title),
            message = config.lastReadMessage ?: stringResource(R.string.last_read_message),
            onDismiss = { showLastReadHint = false },
        )
    }
    if (showNothingToResume) {
        LastReadInfoDialog(
            title = config.lastReadTitle ?: stringResource(R.string.last_read_title),
            message = stringResource(R.string.no_last_read),
            onDismiss = { showNothingToResume = false },
        )
    }
    if (showSupport) {
        SupportDialog(
            logoUrl = config.logoUrl,
            title = config.supportTitle ?: stringResource(R.string.support_title),
            message = config.supportMessage ?: stringResource(R.string.support_message),
            onQuit = {
                showSupport = false
                onExit()
            },
            onRate = {
                showSupport = false
                Intents.rateApp(context)
            },
            onDismiss = { showSupport = false },
        )
    }
}

private enum class HomeTab(val label: Int) { Pdf(R.string.tab_pdf), Mcq(R.string.tab_mcq) }

/** PDF / MCQ switch under the app bar, in the toolbar colour so it reads as one header. */
@Composable
private fun HomeTabs(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    TabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = McqColors.Primary,
        contentColor = Color.White,
        indicator = { positions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(positions[selected.ordinal]),
                height = 3.dp,
                color = McqColors.BadgeYellow,
            )
        },
        divider = {},
    ) {
        HomeTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                selectedContentColor = Color.White,
                unselectedContentColor = Color.White.copy(alpha = 0.7f),
                text = {
                    Text(
                        stringResource(tab.label),
                        fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                    )
                },
            )
        }
    }
}
