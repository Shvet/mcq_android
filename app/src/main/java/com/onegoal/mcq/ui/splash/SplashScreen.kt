package com.onegoal.mcq.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.onegoal.mcq.R
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.ui.theme.McqColors
import kotlinx.coroutines.delay

private const val SPLASH_MILLIS = 1800L

/** White splash with the brand badge, the lab illustration and the red app title. */
@Composable
fun SplashScreen(config: AppConfig, onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        delay(SPLASH_MILLIS)
        finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.brand_badge),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp)
                .background(McqColors.BrandTeal, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (config.splashImageUrl.isNullOrBlank()) {
                TestTubeStand(Modifier.size(width = 220.dp, height = 190.dp))
            } else {
                AsyncImage(model = config.splashImageUrl, contentDescription = null, modifier = Modifier.size(220.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = config.splashTitle ?: stringResource(R.string.splash_title),
                color = McqColors.SplashRed,
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Test tubes on a stand over a burner, drawn in code so the splash works without assets. */
@Composable
private fun TestTubeStand(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maroon = Color(0xFF5D0A0A)

        // Stand: two posts, crossbar and feet
        drawRoundRect(maroon, Offset(w * 0.12f, h * 0.05f), Size(w * 0.03f, h * 0.82f), CornerRadius(4f))
        drawRoundRect(maroon, Offset(w * 0.85f, h * 0.05f), Size(w * 0.03f, h * 0.82f), CornerRadius(4f))
        drawOval(maroon, Offset(w * 0.0f, h * 0.84f), Size(w * 0.26f, h * 0.12f))
        drawOval(maroon, Offset(w * 0.74f, h * 0.84f), Size(w * 0.26f, h * 0.12f))

        tube(Offset(w * 0.24f, h * 0.0f), w * 0.11f, h * 0.55f, Color(0xFFC62828), 0.55f)
        tube(Offset(w * 0.445f, h * 0.0f), w * 0.11f, h * 0.55f, Color(0xFFFFA000), 0.6f)
        tube(Offset(w * 0.65f, h * 0.0f), w * 0.11f, h * 0.55f, Color(0xFF0288D1), 0.5f)

        drawRect(maroon, Offset(w * 0.1f, h * 0.12f), Size(w * 0.8f, h * 0.07f))

        // Flame
        val flame = Path().apply {
            moveTo(w * 0.5f, h * 0.56f)
            cubicTo(w * 0.40f, h * 0.62f, w * 0.40f, h * 0.70f, w * 0.47f, h * 0.73f)
            lineTo(w * 0.53f, h * 0.73f)
            cubicTo(w * 0.60f, h * 0.70f, w * 0.60f, h * 0.62f, w * 0.5f, h * 0.56f)
            close()
        }
        drawPath(flame, Brush.verticalGradient(listOf(Color(0xFFFFEB3B), Color(0xFFFF9800)), h * 0.56f, h * 0.73f))

        // Burner
        drawRoundRect(Color(0xFF00695C), Offset(w * 0.40f, h * 0.73f), Size(w * 0.20f, h * 0.05f), CornerRadius(6f))
        val base = Path().apply {
            moveTo(w * 0.35f, h * 0.95f)
            lineTo(w * 0.43f, h * 0.78f)
            lineTo(w * 0.57f, h * 0.78f)
            lineTo(w * 0.65f, h * 0.95f)
            close()
        }
        drawPath(base, Color(0xFF00796B))
    }
}

private fun DrawScope.tube(topLeft: Offset, width: Float, height: Float, liquid: Color, fill: Float) {
    val radius = CornerRadius(width / 2, width / 2)
    drawRoundRect(Color(0xFFE0F2F7), topLeft, Size(width, height), radius)
    val liquidHeight = height * fill
    drawRoundRect(liquid, Offset(topLeft.x, topLeft.y + height - liquidHeight), Size(width, liquidHeight), radius)
    drawRoundRect(Color(0xFF90A4AE), topLeft, Size(width, height), radius, style = Stroke(width = 3f))
}
