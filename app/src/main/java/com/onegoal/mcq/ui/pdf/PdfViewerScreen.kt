package com.onegoal.mcq.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onegoal.mcq.R
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.ui.components.BannerAd
import com.onegoal.mcq.ui.components.BannerSize
import com.onegoal.mcq.ui.components.JumpToPageDialog
import com.onegoal.mcq.ui.components.MessageState
import com.onegoal.mcq.ui.components.OverflowAction
import com.onegoal.mcq.ui.components.OverflowMenu
import com.onegoal.mcq.ui.theme.McqColors
import com.onegoal.mcq.util.Intents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_ZOOM = 3f

/** Inverts page colours for night mode (white paper becomes black). */
private val InvertFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

@Composable
fun PdfViewerScreen(
    viewModel: PdfViewerViewModel,
    config: AppConfig,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val item = viewModel.item
    val position by viewModel.position.collectAsStateWithLifecycle()
    val documentState by viewModel.documentState.collectAsStateWithLifecycle()
    val nightMode by viewModel.nightMode.collectAsStateWithLifecycle()
    val bookmarkedPages by viewModel.bookmarkedPages.collectAsStateWithLifecycle()
    val scrollRequest by viewModel.scrollRequest.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()

    val drawing = remember { DrawingState() }
    var drawMode by remember { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    val captureLayer = rememberGraphicsLayer()

    val pageCount = (documentState as? DocumentState.Ready)?.renderer?.pageCount ?: 0

    fun exitDrawMode() {
        drawMode = false
        drawing.clear()
    }

    fun takeScreenshot() {
        scope.launch {
            val file = runCatching { saveScreenshot(context, captureLayer.toImageBitmap()) }.getOrNull()
            if (file == null) {
                Toast.makeText(context, R.string.screenshot_failed, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.screenshot_captured, Toast.LENGTH_LONG).show()
                Intents.shareImage(context, file)
            }
        }
    }

    BackHandler(enabled = drawMode) { exitDrawMode() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (nightMode) Color.Black else Color.White)
            .navigationBarsPadding(),
    ) {
        ViewerTopBar(
            pageText = if (pageCount > 0) "${position.page + 1}/$pageCount" else "–",
            nightMode = nightMode,
            bookmarked = position.page in bookmarkedPages,
            onBack = { if (drawMode) exitDrawMode() else onBack() },
            onPageClick = { if (pageCount > 0) showJump = true },
            onToggleNight = viewModel::toggleNightMode,
            onSearch = { if (pageCount > 0) showSearch = true },
            onScreenshot = ::takeScreenshot,
            onBookmark = {
                if (pageCount > 0) {
                    val added = viewModel.toggleBookmark()
                    val message = if (added) {
                        context.getString(R.string.bookmark_added, position.page + 1)
                    } else {
                        context.getString(R.string.bookmark_removed)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onDraw = { if (drawMode) exitDrawMode() else drawMode = true },
            overflow = listOf(
                OverflowAction(context.getString(R.string.menu_jump_to_page)) { if (pageCount > 0) showJump = true },
                OverflowAction(context.getString(R.string.menu_share)) { Intents.shareApp(context, config.shareMessage) },
                OverflowAction(context.getString(R.string.menu_feedback)) {
                    val section = item.sections.getOrNull(position.sectionIndex)?.label.orEmpty()
                    Intents.sendFeedback(
                        context,
                        config.feedbackEmail,
                        listOfNotNull(item.parentTitle, item.title, section, "Page ${position.page + 1}").joinToString("\n"),
                    )
                },
                OverflowAction(context.getString(R.string.menu_rate)) { Intents.rateApp(context) },
            ),
        )

        SectionTabs(
            labels = item.sections.map { it.label },
            selected = position.sectionIndex,
            onSelect = viewModel::selectSection,
        )

        BannerAd(
            enabled = config.adsEnabled,
            adUnitId = config.bannerAdUnitId,
            size = BannerSize.Banner,
            modifier = Modifier
                .background(Color.White)
                .padding(vertical = 2.dp),
        )

        if (drawMode) {
            Text(
                text = stringResource(R.string.draw_hint),
                fontSize = 17.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .clickable { exitDrawMode() }
                    .padding(vertical = 8.dp),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawWithContent {
                    captureLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(captureLayer)
                }
                .background(if (nightMode) Color.Black else Color(0xFFE0E0E0)),
        ) {
            when (val state = documentState) {
                DocumentState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                DocumentState.Error -> MessageState(
                    message = stringResource(R.string.error_loading),
                    onRetry = viewModel::retry,
                    modifier = Modifier.align(Alignment.Center),
                )
                is DocumentState.Ready -> PdfPages(
                    renderer = state.renderer,
                    nightMode = nightMode,
                    scrollEnabled = !drawMode,
                    scrollRequest = scrollRequest,
                    onPageVisible = viewModel::onPageVisible,
                )
            }
            DrawingOverlay(state = drawing, enabled = drawMode, modifier = Modifier.fillMaxSize())
        }

        if (drawMode) {
            DrawToolbar(drawing)
        }
    }

    if (showJump && pageCount > 0) {
        JumpToPageDialog(
            currentPage = position.page + 1,
            pageCount = pageCount,
            onJump = { page ->
                showJump = false
                viewModel.jumpToPage(page - 1)
            },
            onDismiss = { showJump = false },
        )
    }

    if (showSearch) {
        PdfSearchDialog(
            state = searchState,
            onSearch = viewModel::search,
            onResultClick = { page ->
                showSearch = false
                viewModel.jumpToPage(page)
            },
            onDismiss = {
                showSearch = false
                viewModel.clearSearch()
            },
        )
    }
}

@Composable
private fun ViewerTopBar(
    pageText: String,
    nightMode: Boolean,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onPageClick: () -> Unit,
    onToggleNight: () -> Unit,
    onSearch: () -> Unit,
    onScreenshot: () -> Unit,
    onBookmark: () -> Unit,
    onDraw: () -> Unit,
    overflow: List<OverflowAction>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(McqColors.Primary)
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
        Text(
            text = pageText,
            color = Color.White,
            fontSize = 21.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPageClick)
                .padding(vertical = 8.dp),
        )
        if (nightMode) {
            BarIcon(Icons.Outlined.LightMode, stringResource(R.string.day_mode), onToggleNight)
        } else {
            BarIcon(Icons.Filled.DarkMode, stringResource(R.string.night_mode), onToggleNight, tint = McqColors.MoonYellow)
        }
        BarIcon(Icons.Filled.Search, stringResource(R.string.search), onSearch)
        BarIcon(Icons.Filled.PhotoCamera, stringResource(R.string.screenshot), onScreenshot)
        BarIcon(
            if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            stringResource(R.string.bookmark),
            onBookmark,
        )
        BarIcon(Icons.Outlined.Edit, stringResource(R.string.draw), onDraw)
        Box { OverflowMenuWhite(overflow) }
    }
}

@Composable
private fun OverflowMenuWhite(actions: List<OverflowAction>) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides Color.White,
    ) {
        OverflowMenu(actions)
    }
}

@Composable
private fun BarIcon(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Color.White) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(28.dp))
    }
}

/** Questions / Answers tabs: the selected one is black with yellow text and a red underline. */
@Composable
private fun SectionTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    if (labels.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) Color.Black else Color.White)
                    .clickable { onSelect(index) },
            ) {
                Text(
                    text = label,
                    color = if (isSelected) McqColors.TabSelectedText else Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 6.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(if (isSelected) McqColors.TabIndicator else Color(0xFFF5F5F5)),
                )
            }
        }
    }
}

@Composable
private fun PdfPages(
    renderer: PdfDocumentRenderer,
    nightMode: Boolean,
    scrollEnabled: Boolean,
    scrollRequest: ScrollRequest,
    onPageVisible: (Int) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = scrollRequest.page.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0)))
    var zoom by remember { mutableFloatStateOf(1f) }
    val horizontalScroll = rememberScrollState()

    LaunchedEffect(scrollRequest, renderer) {
        if (renderer.pageCount > 0) listState.scrollToItem(scrollRequest.page.coerceIn(0, renderer.pageCount - 1))
    }
    val visiblePage by remember(listState) { derivedStateOf { listState.mostVisiblePage() } }
    LaunchedEffect(listState) {
        snapshotFlow { visiblePage }.distinctUntilChanged().collect { onPageVisible(it) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(scrollEnabled) {
                if (!scrollEnabled) return@pointerInput
                // Two-finger pinch zooms; one finger is left to the list for scrolling.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            zoom = (zoom * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            val pan = event.calculatePan()
                            horizontalScroll.dispatchRawDelta(-pan.x)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val density = LocalDensity.current
        val contentWidth = maxWidth * zoom
        // Re-render in quarter steps so pinching does not re-render on every frame.
        val renderWidthPx = with(density) { (maxWidth * (Math.round(zoom * 4) / 4f)).roundToPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScroll, enabled = scrollEnabled && zoom > 1f),
        ) {
            LazyColumn(
                state = listState,
                userScrollEnabled = scrollEnabled,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .width(contentWidth)
                    .fillMaxHeight(),
            ) {
                items(count = renderer.pageCount, key = { it }) { index ->
                    PdfPage(
                        renderer = renderer,
                        index = index,
                        renderWidthPx = renderWidthPx,
                        nightMode = nightMode,
                    )
                }
            }
        }
    }
}

private fun LazyListState.mostVisiblePage(): Int {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return firstVisibleItemIndex
    return visible.maxBy { item ->
        val top = maxOf(item.offset, info.viewportStartOffset)
        val bottom = minOf(item.offset + item.size, info.viewportEndOffset)
        bottom - top
    }.index
}

@Composable
private fun PdfPage(renderer: PdfDocumentRenderer, index: Int, renderWidthPx: Int, nightMode: Boolean) {
    var bitmap by remember(renderer, index) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(renderer, index, renderWidthPx) {
        renderer.render(index, renderWidthPx)?.let { bitmap = it.asImageBitmap() }
    }
    val ratio = renderer.pageAspectRatios.getOrElse(index) { 1.414f }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f / ratio)
            .background(if (nightMode) Color.Black else Color.White),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = if (nightMode) InvertFilter else null,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun DrawToolbar(state: DrawingState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val idle = Color(0xFF9E9E9E)
        ToolButton(EraserIcon, stringResource(R.string.eraser), if (state.eraser) McqColors.PrimaryDark else idle) {
            state.eraser = !state.eraser
        }
        ToolMenu(Icons.Outlined.RadioButtonChecked, stringResource(R.string.stroke_size), idle) { close ->
            DrawingState.PenWidths.forEach { width ->
                DropdownMenuItem(
                    text = {
                        Box(Modifier.size(width = 80.dp, height = 24.dp), contentAlignment = Alignment.CenterStart) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((width / 3).dp.coerceAtLeast(1.dp))
                                    .background(if (state.width == width) McqColors.PrimaryDark else Color.DarkGray),
                            )
                        }
                    },
                    onClick = {
                        state.width = width
                        close()
                    },
                )
            }
        }
        ToolMenu(Icons.Outlined.Palette, stringResource(R.string.color), state.color) { close ->
            Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DrawingState.PenColors.take(4).forEach { ColorDot(it, state) { state.color = it; state.eraser = false; close() } }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DrawingState.PenColors.drop(4).forEach { ColorDot(it, state) { state.color = it; state.eraser = false; close() } }
            }
        }
        ToolMenu(Icons.Outlined.Opacity, stringResource(R.string.opacity), idle) { close ->
            DrawingState.PenAlphas.forEach { alpha ->
                DropdownMenuItem(
                    text = { Text("${(alpha * 100).toInt()}%", fontWeight = if (state.alpha == alpha) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        state.alpha = alpha
                        close()
                    },
                )
            }
        }
        ToolButton(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo), if (state.canUndo) Color.DarkGray else idle, state::undo)
        ToolButton(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.redo), if (state.canRedo) Color.DarkGray else idle, state::redo)
    }
}

@Composable
private fun ColorDot(color: Color, state: DrawingState, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, CircleShape)
            .then(if (state.color == color) Modifier.border(3.dp, Color.LightGray, CircleShape) else Modifier)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun ToolButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun ToolMenu(
    icon: ImageVector,
    description: String,
    tint: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.(close: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ToolButton(icon, description, tint) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            content { expanded = false }
        }
    }
}

private suspend fun saveScreenshot(context: Context, image: ImageBitmap): File = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, "screenshots").apply { mkdirs() }
    // Keep only the latest few screenshots around.
    directory.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
    val file = File(directory, "Screenshot_${System.currentTimeMillis()}.png")
    file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    file
}
