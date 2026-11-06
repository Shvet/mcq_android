package com.onegoal.mcq.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.onegoal.mcq.R
import com.onegoal.mcq.model.HomeItem
import com.onegoal.mcq.model.HomeLayout
import com.onegoal.mcq.model.HomeMeta
import com.onegoal.mcq.model.HomeSection
import com.onegoal.mcq.ui.components.MessageState
import kotlinx.coroutines.delay

/*
 * The Home tab, drawn from the admin "Home Screen Sections" of this app.
 * Sizes and colours follow the admin phone preview (home-section/index.blade.php and the
 * .pv-* rules in main.css), so what the admin sees there is what the app shows.
 */
private object HomeStyle {
    val Title = Color(0xFF111827)
    val Meta = Color(0xFF6B7280)
    val Outline = Color(0xFFE5E7EB)
    val Placeholder = Color(0xFFEEF2FF)
    val PlaceholderIcon = Color(0xFF6366F1)
    val Chevron = Color(0xFF9CA3AF)
    val Dot = Color(0xFFD1D5DB)
    val DotOn = Color(0xFF2196F3)
    val Gutter = 16.dp
    val Gap = 10.dp
}

@Composable
fun HomeSectionsContent(
    state: HomeSectionsUiState,
    onRetry: () -> Unit,
    onOpen: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            HomeSectionsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            HomeSectionsUiState.Error -> MessageState(
                message = stringResource(R.string.error_loading),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
            is HomeSectionsUiState.Loaded -> if (state.sections.isEmpty()) {
                MessageState(stringResource(R.string.empty_list), Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(state.sections, key = { it.id }) { section ->
                        HomeSectionBlock(section = section, onOpen = onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionBlock(section: HomeSection, onOpen: (HomeItem) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        if (section.layout != HomeLayout.SLIDER && section.title.isNotBlank()) {
            Text(
                section.title,
                color = HomeStyle.Title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = HomeStyle.Gutter).padding(bottom = 8.dp),
            )
        }
        when (section.layout) {
            HomeLayout.SLIDER -> SliderLayout(section.items, onOpen)
            HomeLayout.HORIZONTAL -> HorizontalLayout(section.items, onOpen)
            HomeLayout.GRID -> GridLayout(section.items, onOpen)
            HomeLayout.LIST -> ListLayout(section.items, onOpen)
        }
    }
}

@Composable
private fun SliderLayout(items: List<HomeItem>, onOpen: (HomeItem) -> Unit) {
    val pager = rememberPagerState { items.size }
    if (items.size > 1) {
        LaunchedEffect(pager, items.size) {
            while (true) {
                delay(4_000)
                pager.animateScrollToPage((pager.currentPage + 1) % items.size)
            }
        }
    }
    HorizontalPager(
        state = pager,
        contentPadding = PaddingValues(horizontal = HomeStyle.Gutter),
        pageSpacing = HomeStyle.Gap,
        key = { items[it].key },
    ) { page ->
        val item = items[page]
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(shape)
                .clickable { onOpen(item) },
        ) {
            ItemImage(item, height = 140.dp)
            if (item.name.isNotBlank()) {
                Text(
                    item.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                        .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 10.dp),
                )
            }
        }
    }
    if (items.size > 1) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            repeat(items.size) { index ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (index == pager.currentPage) HomeStyle.DotOn else HomeStyle.Dot),
                )
            }
        }
    }
}

@Composable
private fun HorizontalLayout(items: List<HomeItem>, onOpen: (HomeItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HomeStyle.Gutter),
        horizontalArrangement = Arrangement.spacedBy(HomeStyle.Gap),
    ) {
        items(items, key = { it.key }) { item ->
            Column(
                Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpen(item) },
            ) {
                Box(Modifier.clip(RoundedCornerShape(12.dp))) { ItemImage(item, height = 84.dp) }
                ItemName(item, Modifier.padding(top = 6.dp))
                ItemMeta(item.meta, Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun GridLayout(items: List<HomeItem>, onOpen: (HomeItem) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(HomeStyle.Gap),
        modifier = Modifier.padding(horizontal = HomeStyle.Gutter),
    ) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(HomeStyle.Gap)) {
                row.forEach { item ->
                    val shape = RoundedCornerShape(12.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(shape)
                            .border(1.dp, HomeStyle.Outline, shape)
                            .clickable { onOpen(item) }
                            .padding(bottom = 8.dp),
                    ) {
                        ItemImage(item, height = 70.dp)
                        ItemName(item, Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp))
                        ItemMeta(item.meta, Modifier.padding(horizontal = 10.dp).padding(top = 2.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ListLayout(items: List<HomeItem>, onOpen: (HomeItem) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = HomeStyle.Gutter),
    ) {
        items.forEach { item ->
            val shape = RoundedCornerShape(12.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.dp, HomeStyle.Outline, shape)
                    .clickable { onOpen(item) }
                    .padding(10.dp),
            ) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))) { ItemImage(item, height = 46.dp) }
                Column(Modifier.weight(1f)) {
                    ItemName(item)
                    ItemMeta(item.meta, Modifier.padding(top = 2.dp))
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = HomeStyle.Chevron,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ItemImage(item: HomeItem, height: Dp) {
    val modifier = Modifier.fillMaxWidth().height(height)
    if (item.imageUrl != null) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.name.ifBlank { null },
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(HomeStyle.Placeholder), contentAlignment = Alignment.Center) {
            Icon(
                if (item.isQuiz) Icons.Filled.Checklist else Icons.Filled.Image,
                contentDescription = null,
                tint = HomeStyle.PlaceholderIcon,
                modifier = Modifier.size(if (height < 60.dp) 18.dp else 24.dp),
            )
        }
    }
}

@Composable
private fun ItemName(item: HomeItem, modifier: Modifier = Modifier) {
    Text(
        item.name,
        color = HomeStyle.Title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun ItemMeta(meta: HomeMeta, modifier: Modifier = Modifier) {
    val text = when (meta) {
        is HomeMeta.Items -> pluralStringResource(R.plurals.home_items, meta.count, meta.count)
        HomeMeta.TapToOpen -> stringResource(R.string.home_tap_to_open)
        is HomeMeta.Questions -> pluralStringResource(R.plurals.home_questions, meta.count, meta.count)
        is HomeMeta.Media -> null
    }
    if (text != null) {
        Text(text, color = HomeStyle.Meta, fontSize = 11.sp, maxLines = 1, modifier = modifier)
        return
    }
    meta as HomeMeta.Media
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.height(14.dp)) {
        if (meta.pdf) MetaIcon(Icons.Filled.PictureAsPdf)
        if (meta.audio) MetaIcon(Icons.Filled.Headphones)
        if (meta.video) MetaIcon(Icons.Filled.Videocam)
    }
}

@Composable
private fun MetaIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, tint = HomeStyle.Meta, modifier = Modifier.size(12.dp))
}
