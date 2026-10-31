package com.onegoal.mcq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onegoal.mcq.R
import com.onegoal.mcq.ui.theme.McqColors

/** Card with the dark-blue strip on the left, used by every list in the app. */
@Composable
private fun AccentCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp),
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = McqColors.RowBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(9.dp)
                    .fillMaxHeight()
                    .background(McqColors.RowAccent),
            )
            content()
        }
    }
}

/**
 * Row with the PDF badge, a title and a small red label in the bottom-right corner
 * (the index number on the home screen, "Chapter-wise" on chapter lists).
 */
@Composable
fun PdfListRow(
    title: String,
    trailingLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccentCard(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 76.dp)
                .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PdfIcon(modifier = Modifier.align(Alignment.Top).padding(top = 8.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B1B1B),
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (!trailingLabel.isNullOrBlank()) {
                    Text(
                        text = trailingLabel,
                        color = McqColors.IndexRed,
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkRow(
    parentTitle: String?,
    title: String,
    sectionLabel: String,
    page: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccentCard(onClick = onClick, modifier = modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!parentTitle.isNullOrBlank()) {
                    Text(parentTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1B))
                }
                Text(title, fontSize = 17.sp, color = McqColors.ChapterBlue)
                if (sectionLabel.isNotBlank()) {
                    Text(sectionLabel, fontSize = 17.sp, color = McqColors.SectionGreen)
                }
                Text(stringResource(R.string.page_no, page), fontSize = 17.sp, color = McqColors.IndexRed)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = McqColors.DeleteIndigo,
                )
            }
        }
    }
}

/** Centered message with an optional retry button. */
@Composable
fun MessageState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, color = Color(0xFF616161), fontSize = 16.sp)
        if (onRetry != null) {
            androidx.compose.material3.TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

