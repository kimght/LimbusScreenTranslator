package com.kimght.limbusscreentranslator.overlay

data class ScreenPosition(val x: Int, val y: Int)

data class ScreenInsets(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    companion object {
        val NONE = ScreenInsets(0, 0, 0, 0)
    }
}

data class DisplayFrame(val rotation: Int, val width: Int, val height: Int)

fun clampToScreen(
    x: Int,
    y: Int,
    viewW: Int,
    viewH: Int,
    screenW: Int,
    screenH: Int,
    insets: ScreenInsets = ScreenInsets.NONE,
): ScreenPosition {
    val maxX = (screenW - insets.right - viewW).coerceAtLeast(insets.left)
    val maxY = (screenH - insets.bottom - viewH).coerceAtLeast(insets.top)
    return ScreenPosition(x.coerceIn(insets.left, maxX), y.coerceIn(insets.top, maxY))
}

fun toNaturalFrame(p: ScreenPosition, frame: DisplayFrame): ScreenPosition =
    rotateSteps(p, (4 - frame.rotation) % 4, frame.width, frame.height)

fun fromNaturalFrame(p: ScreenPosition, frame: DisplayFrame): ScreenPosition {
    val naturalW = if (frame.rotation % 2 == 0) frame.width else frame.height
    val naturalH = if (frame.rotation % 2 == 0) frame.height else frame.width
    return rotateSteps(p, frame.rotation % 4, naturalW, naturalH)
}

fun topLeftToNaturalCenter(
    x: Int,
    y: Int,
    viewW: Int,
    viewH: Int,
    frame: DisplayFrame,
): ScreenPosition =
    toNaturalFrame(ScreenPosition(x + viewW / 2, y + viewH / 2), frame)

fun naturalCenterToTopLeft(
    center: ScreenPosition,
    viewW: Int,
    viewH: Int,
    frame: DisplayFrame,
): ScreenPosition {
    val current = fromNaturalFrame(center, frame)
    return ScreenPosition(current.x - viewW / 2, current.y - viewH / 2)
}

private fun rotateSteps(p: ScreenPosition, steps: Int, startW: Int, startH: Int): ScreenPosition {
    var x = p.x
    var y = p.y
    var w = startW
    var h = startH
    repeat(steps) {
        val rotatedX = y
        val rotatedY = w - x
        x = rotatedX
        y = rotatedY
        val swap = w
        w = h
        h = swap
    }
    return ScreenPosition(x, y)
}
