package com.onegoal.mcq.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.onegoal.mcq.model.HomeSeeAll
import kotlinx.coroutines.delay

/*
 * Sections of the home screen, one per admin "Home Screen Section".
 * Sizes, colours and wording follow the admin phone preview (home-section/index.blade.php and
 * the .pv-* rules in main.css), so what the admin builds there is what the app shows.
 */
internal object HomeStyle {
    val Background = Color(0xFFF5F7FB)
    val Title = Color(0xFF111827)
    val Meta = Color(0xFF6B7280)
    val Accent = Color(0xFF1E88E5)
    val Chevron = Color(0xFF9CA3AF)
    val ChevronBg = Color(0xFFF1F5F9)
    val Dot = Color(0xFFD1D5DB)
    val Gutter = 16.dp
    val Gap = 12.dp

    /** Soft tile colours for items without an image; picked by item id (same order as the preview). */
    val Palette = listOf(
        Color(0xFFE3F2FD) to Color(0xFF1E88E5),
        Color(0xFFE8F5E9) to Color(0xFF43A047),
        Color(0xFFFFF3E0) to Color(0xFFFB8C00),
        Color(0xFFF3E5F5) to Color(0xFF8E24AA),
        Color(0xFFFFEBEE) to Color(0xFFE53935),
        Color(0xFFE0F7FA) to Color(0xFF00ACC1),
    )

    fun tile(item: HomeItem): Pair<Color, Color> = Palette[(item.id % Palette.size).toInt().let { if (it < 0) -it else it }]
}

@Composable
internal fun HomeSectionBlock(section: HomeSection, onOpen: (HomeItem) -> Unit, onSeeAll: (HomeSeeAll) -> Unit) {
    Column(Modifier.padding(top = 20.dp)) {
        if (section.layout != HomeLayout.SLIDER && section.title.isNotBlank()) {
            SectionHeader(section.title, section.seeAll?.let { target -> { onSeeAll(target) } })
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
private fun SectionHeader(title: String, onSeeAll: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeStyle.Gutter)
            .padding(bottom = 10.dp),
    ) {
        Text(
            title,
            color = HomeStyle.Title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onSeeAll != null) {
            Text(
                stringResource(R.string.home_see_all),
                color = HomeStyle.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
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
        pageSpacing = 10.dp,
        key = { items[it].key },
    ) { page ->
        val item = items[page]
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onOpen(item) },
        ) {
            ItemArt(item, Modifier.fillMaxSize(), iconSize = 36.dp)
            if (item.name.isNotBlank()) {
                Text(
                    item.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                        .padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 12.dp),
                )
            }
        }
    }
    if (items.size > 1) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            repeat(items.size) { index ->
                val on = index == pager.currentPage
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (on) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (on) HomeStyle.Accent else HomeStyle.Dot),
                )
            }
        }
    }
}

/** Cards in a sideways strip: picture on top, name and caption below. */
@Composable
private fun HorizontalLayout(items: List<HomeItem>, onOpen: (HomeItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = HomeStyle.Gutter, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(HomeStyle.Gap),
    ) {
        items(items, key = { it.key }) { item ->
            HomeCard(onClick = { onOpen(item) }, modifier = Modifier.width(140.dp)) {
                ItemArt(item, Modifier.fillMaxWidth().height(92.dp), iconSize = 30.dp)
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    ItemName(item, maxLines = 2, modifier = Modifier.heightIn(min = 34.dp))
                    ItemMeta(item.meta, Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Two-column tiles: coloured icon square next to the name, like category chips in study apps. */
@Composable
private fun GridLayout(items: List<HomeItem>, onOpen: (HomeItem) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(HomeStyle.Gap),
        modifier = Modifier.padding(horizontal = HomeStyle.Gutter),
    ) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(HomeStyle.Gap)) {
                row.forEach { item ->
                    HomeCard(onClick = { onOpen(item) }, modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp).heightIn(min = 48.dp),
                        ) {
                            ItemArt(item, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)), iconSize = 22.dp)
                            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                ItemName(item, maxLines = 2)
                                ItemMeta(item.meta, Modifier.padding(top = 2.dp))
                            }
                        }
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = HomeStyle.Gutter),
    ) {
        items.forEach { item ->
            HomeCard(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp),
                ) {
                    ItemArt(item, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)), iconSize = 24.dp)
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                        ItemName(item, maxLines = 2, fontSize = 14.sp)
                        ItemMeta(item.meta, Modifier.padding(top = 3.dp))
                    }
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(HomeStyle.ChevronBg),
                        contentAlignment = Alignment.Center,
                    ) {
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
    }
}

@Composable
internal fun HomeCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        content()
    }
}

/** The item's image, or a soft coloured tile with an icon for what it is. */
@Composable
private fun ItemArt(item: HomeItem, modifier: Modifier, iconSize: Dp) {
    if (item.imageUrl != null) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.name.ifBlank { null },
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }
    val (background, tint) = HomeStyle.tile(item)
    Box(modifier.background(background), contentAlignment = Alignment.Center) {
        Icon(itemIcon(item), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

private fun itemIcon(item: HomeItem): ImageVector = when {
    item.isQuiz -> Icons.Filled.Quiz
    item.isFolder -> Icons.Filled.Folder
    else -> Icons.Filled.PictureAsPdf
}

@Composable
private fun ItemName(item: HomeItem, maxLines: Int = 1, fontSize: androidx.compose.ui.unit.TextUnit = 13.sp, modifier: Modifier = Modifier) {
    Text(
        item.name,
        color = HomeStyle.Title,
        fontSize = fontSize,
        lineHeight = fontSize * 1.3f,
        fontWeight = FontWeight.SemiBold,
        maxLines = maxLines,
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
private fun MetaIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = HomeStyle.Meta, modifier = Modifier.size(12.dp))
}
