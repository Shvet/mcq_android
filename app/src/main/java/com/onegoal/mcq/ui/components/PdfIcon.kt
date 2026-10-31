package com.onegoal.mcq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onegoal.mcq.ui.theme.McqColors

/** The stacked pink "PDF" badge shown at the start of each list row. */
@Composable
fun PdfIcon(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = this.size.width * 0.07f
            val back = Size(this.size.width * 0.78f, this.size.height * 0.78f)
            // Back sheet outline (bottom-left)
            drawRoundRect(
                color = McqColors.PdfOutline,
                topLeft = Offset(stroke, this.size.height - back.height - stroke),
                size = back,
                cornerRadius = CornerRadius(stroke * 1.5f),
                style = Stroke(width = stroke),
            )
            // Front badge (top-right)
            drawRoundRect(
                color = McqColors.PdfPink,
                topLeft = Offset(this.size.width - back.width, 0f),
                size = Size(back.width, back.height * 0.82f),
                cornerRadius = CornerRadius(stroke * 1.5f),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = size * 0.78f, height = size * 0.64f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "PDF",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.24f).sp,
                lineHeight = (size.value * 0.24f).sp,
            )
        }
    }
}
