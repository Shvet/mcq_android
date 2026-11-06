package com.onegoal.mcq.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.onegoal.mcq.R
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.model.HomeItem
import com.onegoal.mcq.model.HomeSeeAll
import com.onegoal.mcq.model.ReadingPosition
import com.onegoal.mcq.ui.components.BannerAd
import com.onegoal.mcq.ui.components.BannerSize
import com.onegoal.mcq.ui.components.DrawerDestination
import com.onegoal.mcq.ui.components.DrawerHost
import com.onegoal.mcq.ui.components.LastReadInfoDialog
import com.onegoal.mcq.ui.components.MessageState
import com.onegoal.mcq.ui.components.OverflowAction
import com.onegoal.mcq.ui.components.OverflowMenu
import com.onegoal.mcq.ui.components.SupportDialog
import com.onegoal.mcq.ui.theme.McqColors
import com.onegoal.mcq.util.Intents
import kotlinx.coroutines.launch

/**
 * Single scrolling home screen: a branded header, quick actions (PDF notes, MCQ tests, continue
 * reading, bookmarks), then the sections built in admin for this app. Taps go one level deeper.
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeSectionsViewModel,
    config: AppConfig,
    bookmarkCount: Int,
    lastRead: ReadingPosition?,
    isLastReadHintShown: suspend () -> Boolean,
    onLastReadHintShown: () -> Unit,
    onOpenHomeItem: (HomeItem) -> Unit,
    onSeeAll: (HomeSeeAll) -> Unit,
    onResume: (ReadingPosition) -> Unit,
    onBookmarks: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showSupport by remember { mutableStateOf(false) }
    var showLastReadHint by remember { mutableStateOf(false) }
    var showNothingToResume by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = drawerState.isClosed) { showSupport = true }

    fun continueReading() {
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
    }

    DrawerHost(
        config = config,
        bookmarkCount = bookmarkCount,
        selected = DrawerDestination.Home,
        onHome = {},
        onBookmarks = onBookmarks,
        drawerState = drawerState,
    ) { openDrawer ->
        Column(
            Modifier
                .fillMaxSize()
                .background(HomeStyle.Background)
                .navigationBarsPadding(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f),
            ) {
                item(key = "header") {
                    HomeHeader(
                        config = config,
                        onMenu = openDrawer,
                        overflow = buildList {
                            add(OverflowAction(context.getString(R.string.menu_share)) { Intents.shareApp(context, config.shareMessage) })
                            add(OverflowAction(context.getString(R.string.menu_rate)) { Intents.rateApp(context) })
                            config.moreAppsUrl?.let { url -> add(OverflowAction(context.getString(R.string.menu_more_apps)) { Intents.openUrl(context, url) }) }
                            config.privacyPolicyUrl?.let { url -> add(OverflowAction(context.getString(R.string.menu_privacy)) { Intents.openUrl(context, url) }) }
                        },
                    )
                }
                item(key = "actions") {
                    QuickActions(
                        bookmarkCount = bookmarkCount,
                        onPdfs = { onSeeAll(HomeSeeAll.AllPdfs) },
                        onQuizzes = { onSeeAll(HomeSeeAll.AllQuizzes) },
                        onContinue = ::continueReading,
                        onBookmarks = onBookmarks,
                        modifier = Modifier.offset(y = (-28).dp),
                    )
                }
                if (lastRead != null) {
                    item(key = "continue") {
                        ContinueCard(lastRead, onClick = { onResume(lastRead) }, modifier = Modifier.offset(y = (-14).dp))
                    }
                }
                when (val current = state) {
                    HomeSectionsUiState.Loading -> item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    HomeSectionsUiState.Error -> item(key = "error") {
                        MessageState(stringResource(R.string.error_loading), onRetry = homeViewModel::load)
                    }
                    is HomeSectionsUiState.Loaded -> if (current.sections.isEmpty()) {
                        item(key = "empty") { MessageState(stringResource(R.string.empty_list)) }
                    } else {
                        items(current.sections, key = { "section:${it.id}" }) { section ->
                            HomeSectionBlock(section = section, onOpen = onOpenHomeItem, onSeeAll = onSeeAll)
                        }
                    }
                }
            }
            BannerAd(
                enabled = config.adsEnabled,
                adUnitId = config.bannerAdUnitId,
                size = BannerSize.Banner,
            )
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

/** Blue gradient header behind the status bar: menu, app logo and name, and a greeting. */
@Composable
private fun HomeHeader(config: AppConfig, onMenu: () -> Unit, overflow: List<OverflowAction>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(listOf(McqColors.Primary, McqColors.PrimaryDark)))
            .statusBarsPadding()
            .padding(start = 4.dp, end = 4.dp, bottom = 48.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_menu))
                }
                Text(
                    config.toolbarTitle ?: stringResource(R.string.toolbar_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (overflow.isNotEmpty()) OverflowMenu(overflow)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_greeting),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                    )
                    Text(
                        stringResource(R.string.home_headline),
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (config.logoUrl != null) {
                    AsyncImage(
                        model = config.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }
        }
    }
}

/** White card overlapping the header with four shortcuts. */
@Composable
private fun QuickActions(
    bookmarkCount: Int,
    onPdfs: () -> Unit,
    onQuizzes: () -> Unit,
    onContinue: () -> Unit,
    onBookmarks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(horizontal = HomeStyle.Gutter).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        ) {
            QuickAction(Icons.Filled.AutoStories, stringResource(R.string.home_action_pdfs), HomeStyle.Palette[0], onPdfs)
            QuickAction(Icons.Filled.Quiz, stringResource(R.string.home_action_tests), HomeStyle.Palette[1], onQuizzes)
            QuickAction(Icons.Filled.History, stringResource(R.string.home_action_continue), HomeStyle.Palette[2], onContinue)
            QuickAction(Icons.Filled.Bookmarks, stringResource(R.string.home_action_bookmarks), HomeStyle.Palette[3], onBookmarks, badge = bookmarkCount)
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, colors: Pair<Color, Color>, onClick: () -> Unit, badge: Int = 0) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        BadgedBox(badge = { if (badge > 0) Badge { Text(badge.toString()) } }) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(colors.first),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.second, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            label,
            color = HomeStyle.Title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** "Continue reading" card for the last PDF page the student had open. */
@Composable
private fun ContinueCard(position: ReadingPosition, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HomeCard(onClick = onClick, modifier = modifier.padding(horizontal = HomeStyle.Gutter).fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFF8E1), Color.White)))
                .padding(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_continue_title),
                    color = Color(0xFFB26A00),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    position.item.title,
                    color = HomeStyle.Title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                val section = position.item.sections.getOrNull(position.sectionIndex)?.label.orEmpty()
                Text(
                    listOf(section, stringResource(R.string.page_no, position.page + 1)).filter { it.isNotBlank() }.joinToString(" · "),
                    color = HomeStyle.Meta,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFB8C00)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.open_last_pdf), tint = Color.White)
            }
        }
    }
}
