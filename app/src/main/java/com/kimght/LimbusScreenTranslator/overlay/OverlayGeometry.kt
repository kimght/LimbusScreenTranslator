package com.kimght.LimbusScreenTranslator.overlay

data class ScreenPosition(val x: Int, val y: Int)

fun clampToScreen(
    x: Int,
    y: Int,
    viewW: Int,
    viewH: Int,
    screenW: Int,
    screenH: Int,
): ScreenPosition {
    val maxX = (screenW - viewW).coerceAtLeast(0)
    val maxY = (screenH - viewH).coerceAtLeast(0)
    return ScreenPosition(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
}
