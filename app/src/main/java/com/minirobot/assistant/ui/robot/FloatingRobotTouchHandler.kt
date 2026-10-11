package com.minirobot.assistant.ui.robot

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/**
 * Touch listener attached to the floating robot view in WindowManager.
 * Distinguishes between:
 * - Single Tap -> opens Assistant interface
 * - Double Tap -> opens full Mini Robot Assistant Activity
 * - Long Press & Drag -> repositions the floating robot freely across the phone screen
 */
class FloatingRobotTouchHandler(
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onLongPress: () -> Unit = {},
    private val onDragStart: () -> Unit,
    private val onDragEnd: (finalX: Float, finalY: Float) -> Unit
) : View.OnTouchListener {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val gestureDetector = GestureDetector(null, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isDragging) {
                onSingleTap()
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!isDragging) {
                onDoubleTap()
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (!isDragging) {
                onLongPress()
            }
        }
    })

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        // Feed events to detector for tap and double tap
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                // Threshold to avoid triggering drag on simple micro-jitter
                if (!isDragging && (abs(dx) > 12 || abs(dy) > 12)) {
                    isDragging = true
                    onDragStart()
                }

                if (isDragging) {
                    layoutParams.x = (initialX + dx).toInt()
                    layoutParams.y = (initialY + dy).toInt()
                    try {
                        windowManager.updateViewLayout(view, layoutParams)
                    } catch (_: Exception) {}
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    onDragEnd(layoutParams.x.toFloat(), layoutParams.y.toFloat())
                    return true
                }
            }
        }
        return false
    }
}
