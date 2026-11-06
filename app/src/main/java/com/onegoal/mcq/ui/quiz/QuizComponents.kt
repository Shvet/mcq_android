package com.onegoal.mcq.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.onegoal.mcq.R
import com.onegoal.mcq.model.QuizNode
import com.onegoal.mcq.ui.components.MessageState
import com.onegoal.mcq.ui.theme.McqColors

/** MCQ tab and MCQ folder screens: folders open, tests start. */
@Composable
fun QuizNodeList(
    state: QuizListUiState,
    onRetry: () -> Unit,
    onOpen: (QuizNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(McqColors.QuizBackground)) {
        when (state) {
            QuizListUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            QuizListUiState.Error -> MessageState(
                message = stringResource(R.string.error_loading),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
            is QuizListUiState.Loaded -> if (state.items.isEmpty()) {
                MessageState(stringResource(R.string.quiz_empty_list), Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.key }) { node ->
                        QuizNodeRow(node = node, onClick = { onOpen(node) })
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizNodeRow(node: QuizNode, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NodeBadge(node)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.title,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B1B1B),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (node.isTest) {
                    Text(
                        text = pluralStringResource(R.plurals.quiz_question_count, node.questionCount, node.questionCount),
                        fontSize = 13.sp,
                        color = McqColors.MutedText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (node.isTest) {
                Surface(shape = CircleShape, color = McqColors.Primary, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.quiz_start),
                        tint = Color.White,
                        modifier = Modifier.padding(7.dp),
                    )
                }
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.quiz_open_folder),
                    tint = McqColors.MutedText,
                )
            }
        }
    }
}

@Composable
private fun NodeBadge(node: QuizNode) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(if (node.isTest) McqColors.TimeContainer else McqColors.NoteContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (node.imageUrl != null) {
            AsyncImage(
                model = node.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = if (node.isTest) Icons.Outlined.Quiz else Icons.Outlined.Folder,
                contentDescription = null,
                tint = if (node.isTest) McqColors.PrimaryDark else McqColors.NoteAccent,
            )
        }
    }
}

/**
 * Text and/or image of a question or an answer. Images open full screen with pinch-to-zoom,
 * which matters for diagrams and formulas.
 */
@Composable
fun QuizContent(
    text: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color(0xFF1B1B1B),
    maxImageHeight: Dp = 220.dp,
) {
    var zoomed by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!text.isNullOrBlank()) {
            Text(text = text, fontSize = fontSize.sp, lineHeight = (fontSize * 1.45f).sp, fontWeight = fontWeight, color = color)
        }
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.quiz_zoom_image),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = maxImageHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { zoomed = true },
            )
        }
    }
    if (zoomed && imageUrl != null) {
        ZoomableImageDialog(imageUrl = imageUrl, onDismiss = { zoomed = false })
    }
}

@Composable
private fun ZoomableImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transform = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            offset = if (scale == 1f) Offset.Zero else offset + panChange
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.quiz_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .transformable(transform)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
            }
        }
    }
}

/** Round letter badge used on answers (A, B, C...). */
@Composable
fun OptionLetter(label: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = content, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** Small rounded label, e.g. "Right", "Wrong", "Question 3 of 20". */
@Composable
fun Pill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    border: BorderStroke? = null,
) {
    Surface(shape = RoundedCornerShape(50), color = container, border = border, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Text(text, color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
