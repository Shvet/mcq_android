package com.onegoal.mcq.ui.pdf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/** One freehand stroke; points are in the overlay's own pixels. */
data class InkStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val alpha: Float,
    val eraser: Boolean,
)

/** Pen settings and stroke history with undo / redo for the drawing overlay. */
@Stable
class DrawingState {
    val strokes = mutableStateListOf<InkStroke>()
    private val redoStack = mutableListOf<InkStroke>()

    var color by mutableStateOf(PenColors.first())
    var width by mutableStateOf(PenWidths[1])
    var alpha by mutableStateOf(1f)
    var eraser by mutableStateOf(false)

    var activePoints by mutableStateOf<List<Offset>>(emptyList())
        private set

    val canUndo: Boolean get() = strokes.isNotEmpty()
    var canRedo by mutableStateOf(false)
        private set

    fun start(point: Offset) {
        activePoints = listOf(point)
    }

    fun extend(point: Offset) {
        activePoints = activePoints + point
    }

    fun finish() {
        if (activePoints.isNotEmpty()) {
            strokes += InkStroke(activePoints, color, if (eraser) width * 3 else width, alpha, eraser)
            redoStack.clear()
            canRedo = false
        }
        activePoints = emptyList()
    }

    fun undo() {
        if (strokes.isEmpty()) return
        redoStack += strokes.removeAt(strokes.lastIndex)
        canRedo = true
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        strokes += redoStack.removeAt(redoStack.lastIndex)
        canRedo = redoStack.isNotEmpty()
    }

    fun clear() {
        strokes.clear()
        redoStack.clear()
        canRedo = false
        activePoints = emptyList()
        eraser = false
    }

    fun currentStroke(): InkStroke? = activePoints.takeIf { it.isNotEmpty() }?.let {
        InkStroke(it, color, if (eraser) width * 3 else width, alpha, eraser)
    }

    companion object {
        val PenColors = listOf(
            Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047), Color(0xFFFDD835),
            Color(0xFF8E24AA), Color(0xFFFF6F00), Color(0xFF000000), Color(0xFF00ACC1),
        )
        val PenWidths = listOf(4f, 8f, 16f, 28f)
        val PenAlphas = listOf(1f, 0.6f, 0.35f)
    }
}

/** Transparent overlay that records finger strokes while drawing mode is on. */
@Composable
fun DrawingOverlay(state: DrawingState, enabled: Boolean, modifier: Modifier = Modifier) {
    val input = if (enabled) {
        Modifier.pointerInput(state) {
            detectDragGestures(
                onDragStart = { state.start(it) },
                onDrag = { change, _ ->
                    change.consume()
                    state.extend(change.position)
                },
                onDragEnd = { state.finish() },
                onDragCancel = { state.finish() },
            )
        }
    } else {
        Modifier
    }
    Canvas(
        modifier = modifier
            // Offscreen compositing lets the eraser clear only the ink, not the PDF below.
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .then(input),
    ) {
        (state.strokes + listOfNotNull(state.currentStroke())).forEach { stroke ->
            val path = Path().apply {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                if (stroke.points.size == 1) {
                    lineTo(stroke.points.first().x + 0.1f, stroke.points.first().y + 0.1f)
                }
                stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = if (stroke.eraser) Color.Transparent else stroke.color,
                alpha = if (stroke.eraser) 1f else stroke.alpha,
                style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                blendMode = if (stroke.eraser) BlendMode.Clear else BlendMode.SrcOver,
            )
        }
    }
}
