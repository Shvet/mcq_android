package com.onegoal.mcq.ui.quiz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onegoal.mcq.R
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.model.QuizNode
import com.onegoal.mcq.ui.components.BannerAd
import com.onegoal.mcq.ui.components.BannerSize
import com.onegoal.mcq.ui.components.McqTopBar
import com.onegoal.mcq.ui.components.OverflowAction
import com.onegoal.mcq.ui.theme.McqColors
import com.onegoal.mcq.util.Intents

/** Children of an MCQ category or sub category. */
@Composable
fun QuizListScreen(
    parent: QuizNode,
    viewModel: QuizListViewModel,
    config: AppConfig,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpen: (QuizNode) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            McqTopBar(
                title = parent.title,
                onNavigation = onBack,
                showBack = true,
                overflow = listOf(
                    OverflowAction(context.getString(R.string.menu_home), onHome),
                    OverflowAction(context.getString(R.string.menu_share)) { Intents.shareApp(context, config.shareMessage) },
                    OverflowAction(context.getString(R.string.menu_feedback)) { Intents.sendFeedback(context, config.feedbackEmail) },
                ),
            )
        },
        containerColor = McqColors.QuizBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            QuizNodeList(
                state = state,
                onRetry = viewModel::load,
                onOpen = onOpen,
                modifier = Modifier.weight(1f),
            )
            BannerAd(enabled = config.adsEnabled, adUnitId = config.bannerAdUnitId, size = BannerSize.Banner)
        }
    }
}
