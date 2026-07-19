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

    @Test
    fun `natural point maps into a ROTATION_90 frame and back`() {
        val frame = DisplayFrame(rotation = 1, width = 2400, height = 1080)
        val current = fromNaturalFrame(ScreenPosition(880, 100), frame)
        assertEquals(ScreenPosition(100, 200), current)
        assertEquals(ScreenPosition(880, 100), toNaturalFrame(current, frame))
    }

    @Test
    fun `rotation zero is identity`() {
        val frame = DisplayFrame(rotation = 0, width = 1080, height = 2400)
        assertEquals(ScreenPosition(33, 44), fromNaturalFrame(ScreenPosition(33, 44), frame))
        assertEquals(ScreenPosition(33, 44), toNaturalFrame(ScreenPosition(33, 44), frame))
    }

    @Test
    fun `all rotations round-trip`() {
        for (rotation in 0..3) {
            val portrait = rotation % 2 == 0
            val frame = DisplayFrame(
                rotation = rotation,
                width = if (portrait) 1080 else 2400,
                height = if (portrait) 2400 else 1080,
            )
            val natural = ScreenPosition(700, 1900)
            assertEquals(
                "rotation $rotation",
                natural,
                toNaturalFrame(fromNaturalFrame(natural, frame), frame),
            )
        }
    }

    @Test
    fun `a 180 rotation mirrors both axes`() {
        val frame = DisplayFrame(rotation = 2, width = 1080, height = 2400)
        assertEquals(
            ScreenPosition(1080 - 100, 2400 - 200),
            fromNaturalFrame(ScreenPosition(100, 200), frame),
        )
    }

    @Test
    fun `center anchor keeps a bubble physically centered after a left turn`() {
        val center = topLeftToNaturalCenter(482, 206, 116, 116, DisplayFrame(0, 1080, 2400))
        assertEquals(ScreenPosition(540, 264), center)

        // ROTATION_90
        val landscape = DisplayFrame(rotation = 1, width = 2400, height = 1080)
        assertEquals(
            ScreenPosition(264 - 58, 540 - 58),
            naturalCenterToTopLeft(center, 116, 116, landscape),
        )
    }

    @Test
    fun `center anchor keeps a bubble at the same edge distance after a right turn`() {
        val center = topLeftToNaturalCenter(482, 206, 116, 116, DisplayFrame(0, 1080, 2400))

        // ROTATION_270
        val landscape = DisplayFrame(rotation = 3, width = 2400, height = 1080)
        assertEquals(
            ScreenPosition(2400 - 264 - 58, 540 - 58),
            naturalCenterToTopLeft(center, 116, 116, landscape),
        )
    }

    @Test
    fun `center anchor round-trips through save and apply`() {
        val frame = DisplayFrame(rotation = 1, width = 2400, height = 1080)
        val center = topLeftToNaturalCenter(300, 500, 200, 120, frame)
        assertEquals(ScreenPosition(300, 500), naturalCenterToTopLeft(center, 200, 120, frame))
    }

    @Test
    fun `insets shrink the clampable area`() {
        val insets = ScreenInsets(left = 0, top = 60, right = 0, bottom = 0)
        assertEquals(ScreenPosition(50, 60), clampToScreen(50, 10, 200, 100, 1000, 500, insets))
    }

    @Test
    fun `zero insets keep the old clamping behavior`() {
        assertEquals(
            ScreenPosition(800, 400),
            clampToScreen(950, 480, 200, 100, 1000, 500, ScreenInsets.NONE),
        )
    }
}
