package com.onegoal.mcq.ui.bookmarks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onegoal.mcq.R
import com.onegoal.mcq.data.local.BookmarkEntity
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.ui.components.BannerAd
import com.onegoal.mcq.ui.components.BannerSize
import com.onegoal.mcq.ui.components.BookmarkRow
import com.onegoal.mcq.ui.components.DrawerDestination
import com.onegoal.mcq.ui.components.DrawerHost
import com.onegoal.mcq.ui.components.McqTopBar
import com.onegoal.mcq.ui.components.MessageState

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    config: AppConfig,
    onOpen: (BookmarkEntity) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
    onHome: () -> Unit,
) {
    DrawerHost(
        config = config,
        bookmarkCount = bookmarks.size,
        selected = DrawerDestination.Bookmarks,
        onHome = onHome,
        onBookmarks = {},
    ) { openDrawer ->
        Scaffold(
            topBar = {
                McqTopBar(
                    title = config.toolbarTitle ?: stringResource(R.string.toolbar_title),
                    onNavigation = openDrawer,
                    showBack = false,
                    onHome = onHome,
                )
            },
            containerColor = Color.White,
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (bookmarks.isEmpty()) {
                        MessageState(stringResource(R.string.bookmarks_empty), Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(contentPadding = PaddingValues(vertical = 6.dp)) {
                            items(bookmarks, key = { it.id }) { bookmark ->
                                BookmarkRow(
                                    parentTitle = bookmark.parentTitle,
                                    title = bookmark.title,
                                    sectionLabel = bookmark.sectionLabel,
                                    page = bookmark.page + 1,
                                    onClick = { onOpen(bookmark) },
                                    onDelete = { onDelete(bookmark) },
                                )
                            }
                        }
                    }
                }
                BannerAd(enabled = config.adsEnabled, adUnitId = config.bannerAdUnitId, size = BannerSize.Banner)
            }
        }
    }
}
