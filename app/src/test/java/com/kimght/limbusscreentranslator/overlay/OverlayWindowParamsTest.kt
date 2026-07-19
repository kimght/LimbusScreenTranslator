package com.kimght.limbusscreentranslator.overlay

import android.view.Gravity
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OverlayWindowParamsTest {

    private val params = overlayWindowLayoutParams()

    @Test
    fun `is an application overlay anchored top-start`() {
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, params.type)
        assertEquals(Gravity.TOP or Gravity.START, params.gravity)
    }

    @Test
    fun `lays out in screen coordinates so the origin ignores the status bar`() {
        val required = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        assertEquals(required, params.flags and required)
    }

    @Test
    fun `does not fit any insets so the frame never shrinks below a visible status bar`() {
        assertEquals(0, params.fitInsetsTypes)
    }

    @Test
    fun `extends into the display cutout so y is absolute on notched devices`() {
        assertTrue(
            params.layoutInDisplayCutoutMode ==
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
        )
    }
}
