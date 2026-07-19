package com.kimght.LimbusScreenTranslator.overlay

import android.view.WindowManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class OverlayWindowParamsMoveAnimationTest {

    @Test
    @Config(sdk = [34])
    fun `move animation is disabled through the public api`() {
        assertFalse(overlayWindowLayoutParams().canPlayMoveAnimation())
    }

    @Test
    @Config(sdk = [33])
    fun `move animation is disabled through the private flag below api 34`() {
        val params = overlayWindowLayoutParams()
        val privateFlags = WindowManager.LayoutParams::class.java
            .getField("privateFlags")
            .getInt(params)
        assertTrue(privateFlags and 0x00000040 != 0)
    }
}
