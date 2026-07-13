package com.kimght.LimbusScreenTranslator.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayGeometryTest {

    @Test
    fun `in-bounds position is unchanged`() {
        assertEquals(ScreenPosition(50, 80), clampToScreen(50, 80, 200, 100, 1000, 500))
    }

    @Test
    fun `negative coordinates clamp to zero`() {
        assertEquals(ScreenPosition(0, 0), clampToScreen(-30, -10, 200, 100, 1000, 500))
    }

    @Test
    fun `overflow clamps so the view stays fully on screen`() {
        assertEquals(ScreenPosition(800, 400), clampToScreen(950, 480, 200, 100, 1000, 500))
    }

    @Test
    fun `a view larger than the screen pins to the top-left`() {
        assertEquals(ScreenPosition(0, 0), clampToScreen(40, 40, 1200, 600, 1000, 500))
    }

    @Test
    fun `after a rotation swap an old landscape x is pulled back into portrait`() {
        assertEquals(ScreenPosition(300, 80), clampToScreen(900, 80, 200, 100, 500, 1000))
    }
}
