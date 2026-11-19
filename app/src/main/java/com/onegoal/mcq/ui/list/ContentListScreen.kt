package com.onegoal.mcq.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onegoal.mcq.R
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.model.ContentItem
import com.onegoal.mcq.ui.components.BannerAd
import com.onegoal.mcq.ui.components.BannerSize
import com.onegoal.mcq.ui.components.ContentList
import com.onegoal.mcq.ui.components.McqTopBar
import com.onegoal.mcq.ui.components.OverflowAction
import com.onegoal.mcq.util.Intents

/** Chapter list: every PDF category of the app (`title` only), or the children of a folder. */
@Composable
fun ContentListScreen(
    title: String,
    viewModel: ContentListViewModel,
    config: AppConfig,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenItem: (ContentItem) -> Unit,
    isRoot: Boolean = false,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            McqTopBar(
                title = title,
                onNavigation = onBack,
                showBack = true,
                overflow = listOf(
                    OverflowAction(context.getString(R.string.menu_home), onHome),
                    OverflowAction(context.getString(R.string.menu_share)) { Intents.shareApp(context, config.shareMessage) },
                    OverflowAction(context.getString(R.string.menu_feedback)) { Intents.sendFeedback(context, config.feedbackEmail) },
                ),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            ContentList(
                state = state,
                onRetry = viewModel::load,
                onItemClick = onOpenItem,
                trailingLabel = { index, item -> if (isRoot) (index + 1).toString() else item.itemLabel },
                modifier = Modifier.weight(1f),
            )
            BannerAd(enabled = config.adsEnabled, adUnitId = config.bannerAdUnitId, size = BannerSize.Banner)
        }
    }
}
