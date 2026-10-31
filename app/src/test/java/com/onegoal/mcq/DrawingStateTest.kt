package com.onegoal.mcq

import androidx.compose.ui.geometry.Offset
import com.onegoal.mcq.ui.pdf.DrawingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingStateTest {

    private fun DrawingState.draw(first: Offset, vararg rest: Pair<Float, Float>) {
        val points = listOf(first) + rest.map { (x, y) -> Offset(x, y) }
        start(points.first())
        points.drop(1).forEach(::extend)
        finish()
    }

    @Test
    fun undoAndRedoRestoreStrokes() {
        val state = DrawingState()
        state.draw(Offset(0f, 0f), 10f to 10f)
        state.draw(Offset(5f, 5f), 20f to 20f)

        state.undo()
        assertEquals(1, state.strokes.size)
        assertTrue(state.canRedo)

        state.redo()
        assertEquals(2, state.strokes.size)
        assertFalse(state.canRedo)
    }

    @Test
    fun newStrokeClearsRedoHistory() {
        val state = DrawingState()
        state.draw(Offset(0f, 0f), 1f to 1f)
        state.undo()
        state.draw(Offset(2f, 2f), 3f to 3f)

        assertFalse(state.canRedo)
        assertEquals(1, state.strokes.size)
    }

    @Test
    fun eraserStrokesAreWiderAndFlagged() {
        val state = DrawingState()
        state.eraser = true
        state.draw(Offset(0f, 0f), 1f to 1f)

        val stroke = state.strokes.single()
        assertTrue(stroke.eraser)
        assertEquals(state.width * 3, stroke.width)
    }
}
