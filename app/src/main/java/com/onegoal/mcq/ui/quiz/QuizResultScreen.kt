package com.onegoal.mcq.ui.quiz

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onegoal.mcq.R
import com.onegoal.mcq.model.AnswerStatus
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.model.QuestionResult
import com.onegoal.mcq.model.QuizOption
import com.onegoal.mcq.model.QuizResult
import com.onegoal.mcq.model.formatDuration
import com.onegoal.mcq.ui.components.BannerAd
import com.onegoal.mcq.ui.components.BannerSize
import com.onegoal.mcq.ui.components.MessageState
import com.onegoal.mcq.ui.theme.McqColors
import com.onegoal.mcq.util.Intents
import com.onegoal.mcq.util.ScreenshotFiles
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class ReviewFilter { All, Correct, Wrong, Unattempted }

/** Score card (shareable as a screenshot) followed by a question-by-question review. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
    result: QuizResult?,
    config: AppConfig,
    onClose: () -> Unit,
    onRetake: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureLayer = rememberGraphicsLayer()
    var filter by rememberSaveable { mutableStateOf(ReviewFilter.All) }
    val brand = config.toolbarTitle ?: stringResource(R.string.toolbar_title)

    fun share() {
        val r = result ?: return
        scope.launch {
            val file = runCatching { ScreenshotFiles.save(context, captureLayer.toImageBitmap(), prefix = "Result") }.getOrNull()
            if (file == null) {
                Toast.makeText(context, R.string.screenshot_failed, Toast.LENGTH_SHORT).show()
            } else {
                val text = context.getString(R.string.quiz_share_text, r.correct, r.total, r.test.title, formatDuration(r.elapsedMillis))
                Intents.shareImage(context, file, text)
            }
        }
    }

    Scaffold(
        containerColor = McqColors.QuizBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quiz_result_title), fontSize = 19.sp) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    if (result != null) {
                        IconButton(onClick = ::share) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.quiz_share_result))
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
            BannerAd(enabled = config.adsEnabled, adUnitId = config.bannerAdUnitId, size = BannerSize.Banner)
        },
    ) { padding ->
        if (result == null) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                MessageState(stringResource(R.string.quiz_result_missing), Modifier.align(Alignment.Center), onRetry = null)
            }
            return@Scaffold
        }

        val reviewItems = result.questions.filter {
            when (filter) {
                ReviewFilter.All -> true
                ReviewFilter.Correct -> it.status == AnswerStatus.CORRECT
                ReviewFilter.Wrong -> it.status == AnswerStatus.WRONG
                ReviewFilter.Unattempted -> it.status == AnswerStatus.UNATTEMPTED
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "score") {
                // Everything inside this box is what "Share result" turns into an image.
                Box(
                    modifier = Modifier
                        .drawWithContent {
                            captureLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(captureLayer)
                        }
                        .background(McqColors.QuizBackground)
                        .padding(16.dp),
                ) {
                    ScoreCard(result = result, brand = brand)
                }
            }
            item(key = "actions") {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = ::share,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = McqColors.Primary),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.quiz_share_result), fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onRetake,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.quiz_retake), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item(key = "reviewHeader") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        stringResource(R.string.quiz_review),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReviewChip(stringResource(R.string.quiz_filter_all), result.total, filter == ReviewFilter.All, McqColors.Primary) { filter = ReviewFilter.All }
                        ReviewChip(stringResource(R.string.quiz_correct), result.correct, filter == ReviewFilter.Correct, McqColors.Correct) { filter = ReviewFilter.Correct }
                        ReviewChip(stringResource(R.string.quiz_wrong), result.wrong, filter == ReviewFilter.Wrong, McqColors.Wrong) { filter = ReviewFilter.Wrong }
                        ReviewChip(stringResource(R.string.quiz_unattempted), result.unattempted, filter == ReviewFilter.Unattempted, McqColors.Skipped) { filter = ReviewFilter.Unattempted }
                    }
                }
            }
            if (reviewItems.isEmpty()) {
                item(key = "noItems") {
                    MessageState(stringResource(R.string.quiz_no_review_items))
                }
            }
            items(reviewItems, key = { it.question.id }) { item ->
                ReviewCard(item = item, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun ScoreCard(result: QuizResult, brand: String) {
    val percent = (result.score * 100).roundToInt()
    val remark = stringResource(
        when {
            result.score >= 0.9f -> R.string.quiz_remark_top
            result.score >= 0.75f -> R.string.quiz_remark_great
            result.score >= 0.5f -> R.string.quiz_remark_good
            else -> R.string.quiz_remark_low
        },
    )
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(McqColors.Primary, McqColors.PrimaryDark)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            result.test.parentTitle?.let {
                Text(
                    it,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                result.test.title,
                color = Color.White,
                fontSize = 19.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            ScoreRing(score = result.score, correct = result.correct, total = result.total, percent = percent)
            Spacer(Modifier.height(12.dp))
            Text(remark, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(Icons.Filled.CheckCircle, stringResource(R.string.quiz_correct), "${result.correct}", McqColors.Correct, McqColors.CorrectContainer, Modifier.weight(1f))
                        StatTile(Icons.Filled.Cancel, stringResource(R.string.quiz_wrong), "${result.wrong}", McqColors.Wrong, McqColors.WrongContainer, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(Icons.Filled.RemoveCircleOutline, stringResource(R.string.quiz_unattempted), "${result.unattempted}", McqColors.Skipped, McqColors.SkippedContainer, Modifier.weight(1f))
                        StatTile(Icons.Outlined.Timer, stringResource(R.string.quiz_time_taken), formatDuration(result.elapsedMillis), McqColors.TimeBlue, McqColors.TimeContainer, Modifier.weight(1f))
                    }
                    if (result.attempted > 0) {
                        Text(
                            stringResource(
                                R.string.quiz_accuracy_line,
                                (result.accuracy * 100).roundToInt(),
                                formatDuration(result.millisPerQuestion),
                            ),
                            color = McqColors.MutedText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(brand, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun ScoreRing(score: Float, correct: Int, total: Int, percent: Int) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(score) {
        sweep.animateTo(score, animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    Box(modifier = Modifier.size(156.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = McqColors.ScoreGold,
                startAngle = -90f,
                sweepAngle = 360f * sweep.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$correct/$total", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.quiz_score_percent, percent), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, tint: Color, container: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = container, modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = tint)
                Text(label, fontSize = 12.sp, color = McqColors.MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ReviewChip(label: String, count: Int, selected: Boolean, color: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$label  $count") },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            selectedContainerColor = color,
            selectedLabelColor = Color.White,
            labelColor = color,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = color.copy(alpha = 0.5f),
            selectedBorderColor = color,
        ),
    )
}

@Composable
private fun ReviewCard(item: QuestionResult, modifier: Modifier = Modifier) {
    val question = item.question
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Q${item.index + 1}", fontWeight = FontWeight.Bold, color = McqColors.PrimaryDark, modifier = Modifier.weight(1f))
                when (item.status) {
                    AnswerStatus.CORRECT -> Pill(stringResource(R.string.quiz_correct), McqColors.CorrectContainer, McqColors.Correct, icon = Icons.Filled.CheckCircle)
                    AnswerStatus.WRONG -> Pill(stringResource(R.string.quiz_wrong), McqColors.WrongContainer, McqColors.Wrong, icon = Icons.Filled.Cancel)
                    AnswerStatus.UNATTEMPTED -> Pill(stringResource(R.string.quiz_not_answered), McqColors.SkippedContainer, McqColors.Skipped, icon = Icons.Filled.RemoveCircleOutline)
                }
            }
            QuizContent(text = question.text, imageUrl = question.imageUrl, fontWeight = FontWeight.Medium)
            question.options.forEach { option ->
                ReviewOption(
                    option = option,
                    isRight = option.number == question.correctOption,
                    isPicked = option.number == item.selectedOption,
                )
            }
            if (!question.note.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = McqColors.NoteContainer,
                    border = BorderStroke(1.dp, McqColors.NoteAccent.copy(alpha = 0.4f)),
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = McqColors.NoteAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.quiz_note), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF8D6E00))
                            Text(question.note, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF3E3A2F))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewOption(option: QuizOption, isRight: Boolean, isPicked: Boolean) {
    val (container, accent) = when {
        isRight -> McqColors.CorrectContainer to McqColors.Correct
        isPicked -> McqColors.WrongContainer to McqColors.Wrong
        else -> Color.White to McqColors.CardOutline
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = BorderStroke(if (isRight || isPicked) 1.5.dp else 1.dp, accent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OptionLetter(
                label = option.label,
                container = if (isRight || isPicked) accent else McqColors.QuizBackground,
                content = if (isRight || isPicked) Color.White else McqColors.MutedText,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                QuizContent(text = option.text, imageUrl = option.imageUrl, fontSize = 15, maxImageHeight = 160.dp)
                if (isRight || isPicked) {
                    Text(
                        text = stringResource(if (isRight) R.string.quiz_right_answer else R.string.quiz_your_answer) +
                            if (isRight && isPicked) " · " + stringResource(R.string.quiz_your_answer) else "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (isRight) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = McqColors.Correct)
            } else if (isPicked) {
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = McqColors.Wrong)
            }
        }
    }
}
