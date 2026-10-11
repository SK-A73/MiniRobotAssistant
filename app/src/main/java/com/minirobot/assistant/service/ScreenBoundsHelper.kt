package com.minirobot.assistant.service

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager

data class SafeBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val screenWidth: Int,
    val screenHeight: Int
)

/**
 * Accurately determines available screen area, accounting for display cutouts,
 * status bars, navigation bars, and orientation changes.
 */
object ScreenBoundsHelper {

    fun calculateSafeBounds(
        context: Context,
        robotWidthPx: Float,
        robotHeightPx: Float,
        marginPx: Float = 24f,
        bottomOverride: Float? = null
    ): SafeBounds {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val screenWidth: Int
        val screenHeight: Int
        val insetsLeft: Int
        val insetsTop: Int
        val insetsRight: Int
        val insetsBottom: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val bounds = metrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()

            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            insetsLeft = insets.left
            insetsTop = insets.top
            insetsRight = insets.right
            insetsBottom = insets.bottom
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels

            // Approximate status bar and nav bar for API 26-29
            insetsLeft = 0
            insetsTop = 72
            insetsRight = 0
            insetsBottom = 96
        }

        val minX = insetsLeft + marginPx
        val maxX = (screenWidth - insetsRight - robotWidthPx - marginPx).coerceAtLeast(minX)

        val minY = insetsTop + marginPx
        val effectiveBottomLimit = if (bottomOverride != null && bottomOverride > minY) {
            bottomOverride
        } else {
            (screenHeight - insetsBottom).toFloat()
        }
        val maxY = (effectiveBottomLimit - robotHeightPx - marginPx).coerceAtLeast(minY)

        return SafeBounds(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
    }
}
