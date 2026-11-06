package com.onegoal.mcq.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onegoal.mcq.R
import com.onegoal.mcq.model.QuizNode
import com.onegoal.mcq.model.QuizOption
import com.onegoal.mcq.model.QuizQuestion
import com.onegoal.mcq.model.formatDuration
import com.onegoal.mcq.ui.components.MessageState
import com.onegoal.mcq.ui.theme.McqColors
import kotlinx.coroutines.delay

/** Intro -> timed session -> finish. [onFinished] gets the id of the stored result. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTestScreen(
    viewModel: QuizTestViewModel,
    onBack: () -> Unit,
    onFinished: (resultId: Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val running = state.phase == QuizPhase.Running

    var showQuit by rememberSaveable { mutableStateOf(false) }
    var showFinish by rememberSaveable { mutableStateOf(false) }
    var showPalette by rememberSaveable { mutableStateOf(false) }

    // Ticks the on-screen clock; the real time lives in the ViewModel.
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running, state.paused) {
        elapsed = viewModel.elapsedMillis()
        while (running && !state.paused) {
            delay(250)
            elapsed = viewModel.elapsedMillis()
        }
    }

    // Leaving the app (call, notification, home) pauses the session.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (running) viewModel.pause() }

    BackHandler(enabled = running) { showQuit = true }

    fun finish() {
        showFinish = false
        showPalette = false
        onFinished(viewModel.finish())
    }

    Scaffold(
        containerColor = McqColors.QuizBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.test.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 19.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (running) showQuit = true else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (running) {
                        TimerChip(
                            elapsedMillis = elapsed,
                            paused = state.paused,
                            onClick = { if (state.paused) viewModel.resume() else viewModel.pause() },
                        )
                        IconButton(onClick = { showPalette = true }) {
                            Icon(Icons.Filled.GridView, contentDescription = stringResource(R.string.quiz_palette))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = McqColors.Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            if (running && !state.paused) {
                QuizNavBar(
                    canGoBack = state.currentIndex > 0,
                    isLast = state.isLast,
                    onPrevious = viewModel::previous,
                    onNext = { if (state.isLast) showFinish = true else viewModel.next() },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (state.phase) {
                QuizPhase.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                QuizPhase.Error -> MessageState(
                    message = stringResource(R.string.error_loading),
                    onRetry = viewModel::load,
                    modifier = Modifier.align(Alignment.Center),
                )
                QuizPhase.Empty -> MessageState(stringResource(R.string.quiz_empty_test), Modifier.align(Alignment.Center))
                QuizPhase.Intro -> IntroCard(test = viewModel.test, questionCount = state.questions.size, onStart = viewModel::start)
                QuizPhase.Running -> if (state.paused) {
                    PausedCard(onResume = viewModel::resume, modifier = Modifier.align(Alignment.Center))
                } else {
                    RunningContent(state = state, onSelect = viewModel::select)
                }
            }
        }
    }

    if (showPalette && running) {
        ModalBottomSheet(
            onDismissRequest = { showPalette = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
        ) {
            QuestionPalette(
                state = state,
                onSelect = { index ->
                    viewModel.goTo(index)
                    showPalette = false
                },
                onFinish = { showFinish = true },
            )
        }
    }

    if (showFinish) {
        val unattempted = state.questions.size - state.answeredCount
        AlertDialog(
            onDismissRequest = { showFinish = false },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = McqColors.Correct) },
            title = { Text(stringResource(R.string.quiz_finish_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.quiz_finish_message, state.answeredCount, state.questions.size))
                    if (unattempted > 0) {
                        Text(stringResource(R.string.quiz_finish_unattempted, unattempted), color = McqColors.Skipped)
                    }
                }
            },
            confirmButton = { Button(onClick = ::finish) { Text(stringResource(R.string.quiz_finish)) } },
            dismissButton = { TextButton(onClick = { showFinish = false }) { Text(stringResource(R.string.quiz_keep_going)) } },
            containerColor = Color.White,
        )
    }

    if (showQuit) {
        AlertDialog(
            onDismissRequest = { showQuit = false },
            title = { Text(stringResource(R.string.quiz_quit_title)) },
            text = { Text(stringResource(R.string.quiz_quit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showQuit = false
                    onBack()
                }) { Text(stringResource(R.string.quiz_quit), color = McqColors.Wrong) }
            },
            dismissButton = { Button(onClick = { showQuit = false }) { Text(stringResource(R.string.quiz_continue)) } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun TimerChip(elapsedMillis: Long, paused: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White,
        modifier = Modifier.padding(end = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = formatDuration(elapsedMillis),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Icon(
                imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = stringResource(if (paused) R.string.quiz_resume else R.string.quiz_pause),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun IntroCard(test: QuizNode, questionCount: Int, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(McqColors.TimeContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Quiz, contentDescription = null, tint = McqColors.PrimaryDark, modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.height(16.dp))
                test.parentTitle?.let {
                    Text(it, color = McqColors.MutedText, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    test.title,
                    fontSize = 21.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1B1B1B),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        text = pluralStringResource(R.plurals.quiz_question_count, questionCount, questionCount),
                        container = McqColors.TimeContainer,
                        content = McqColors.TimeBlue,
                        icon = Icons.Outlined.Quiz,
                    )
                    Pill(
                        text = "00:00",
                        container = McqColors.SkippedContainer,
                        content = McqColors.Skipped,
                        icon = Icons.Outlined.Timer,
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.quiz_ready_title), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                listOf(
                    R.string.quiz_rule_timer,
                    R.string.quiz_rule_select,
                    R.string.quiz_rule_navigate,
                    R.string.quiz_rule_unattempted,
                ).forEachIndexed { index, rule ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "${index + 1}",
                            color = McqColors.Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(22.dp),
                        )
                        Text(stringResource(rule), color = McqColors.MutedText, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }

        Button(
            onClick = onStart,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = McqColors.Primary),
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.quiz_start), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RunningContent(state: QuizTestUiState, onSelect: (Int) -> Unit) {
    val total = state.questions.size
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else state.answeredCount.toFloat() / total,
        label = "answeredProgress",
    )
    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { progress },
            color = McqColors.Correct,
            trackColor = McqColors.CardOutline,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.quiz_question_of, state.currentIndex + 1, total),
                fontWeight = FontWeight.SemiBold,
                color = McqColors.PrimaryDark,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.quiz_answered_of, state.answeredCount, total),
                fontSize = 13.sp,
                color = McqColors.MutedText,
            )
        }

        AnimatedContent(
            targetState = state.currentIndex,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { width -> if (forward) width / 4 else -width / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> if (forward) -width / 4 else width / 4 } + fadeOut())
            },
            label = "question",
            modifier = Modifier.weight(1f),
        ) { index ->
            val question = state.questions.getOrNull(index) ?: return@AnimatedContent
            QuestionPage(
                question = question,
                selected = state.answers[question.id],
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun QuestionPage(question: QuizQuestion, selected: Int?, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            QuizContent(
                text = question.text,
                imageUrl = question.imageUrl,
                fontSize = 17,
                fontWeight = FontWeight.Medium,
                maxImageHeight = 280.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            )
        }
        question.options.forEach { option ->
            OptionCard(option = option, selected = selected == option.number, onClick = { onSelect(option.number) })
        }
    }
}

@Composable
private fun OptionCard(option: QuizOption, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = if (selected) McqColors.SelectedContainer else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) McqColors.Primary else McqColors.CardOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OptionLetter(
                label = option.label,
                container = if (selected) McqColors.Primary else McqColors.QuizBackground,
                content = if (selected) Color.White else McqColors.PrimaryDark,
            )
            Spacer(Modifier.width(12.dp))
            QuizContent(text = option.text, imageUrl = option.imageUrl, modifier = Modifier.weight(1f), maxImageHeight = 180.dp)
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = McqColors.Primary)
            }
        }
    }
}

@Composable
private fun QuizNavBar(canGoBack: Boolean, isLast: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = canGoBack,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.quiz_previous))
            }
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isLast) McqColors.Correct else McqColors.Primary),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(if (isLast) R.string.quiz_finish else R.string.quiz_next), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (isLast) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun PausedCard(onResume: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.padding(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Pause, contentDescription = null, tint = McqColors.Skipped, modifier = Modifier.size(44.dp))
            Text(stringResource(R.string.quiz_paused_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.quiz_paused_message),
                color = McqColors.MutedText,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onResume,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = McqColors.Primary),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.quiz_resume))
            }
        }
    }
}

@Composable
private fun QuestionPalette(state: QuizTestUiState, onSelect: (Int) -> Unit, onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(stringResource(R.string.quiz_palette), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.quiz_answered_of, state.answeredCount, state.questions.size),
            color = McqColors.MutedText,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(McqColors.Primary, null, stringResource(R.string.quiz_legend_answered))
            LegendDot(Color.White, McqColors.Primary, stringResource(R.string.quiz_legend_current))
            LegendDot(McqColors.QuizBackground, McqColors.CardOutline, stringResource(R.string.quiz_legend_unanswered))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = McqColors.CardOutline)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.heightIn(max = 340.dp),
        ) {
            itemsIndexed(state.questions, key = { _, q -> q.id }) { index, question ->
                val answered = state.answers.containsKey(question.id)
                val current = index == state.currentIndex
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (answered) McqColors.Primary else if (current) Color.White else McqColors.QuizBackground)
                        .border(
                            width = if (current) 2.dp else 1.dp,
                            color = if (current) McqColors.PrimaryDark else if (answered) McqColors.Primary else McqColors.CardOutline,
                            shape = CircleShape,
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        color = if (answered) Color.White else Color(0xFF1B1B1B),
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onFinish,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = McqColors.Correct),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.quiz_finish_test), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LegendDot(fill: Color, outline: Color?, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(fill)
                .then(if (outline != null) Modifier.border(1.5.dp, outline, CircleShape) else Modifier),
        )
        Text(label, fontSize = 12.sp, color = McqColors.MutedText)
    }
}
