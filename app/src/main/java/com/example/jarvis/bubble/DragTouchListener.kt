package com.example.jarvis.bubble

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager

class DragTouchListener(
    private val lp: WindowManager.LayoutParams,
    private val wm: WindowManager,
    private val view: View,
    private val onTap: () -> Unit
) : View.OnTouchListener {

    private var startX = 0f
    private var startY = 0f
    private var origX = 0
    private var origY = 0
    private var dragging = false
    private val slop = ViewConfiguration.get(view.context).scaledTouchSlop

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = e.rawX
                startY = e.rawY
                origX = lp.x
                origY = lp.y
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - startX
                val dy = e.rawY - startY
                if (dragging || (dx * dx + dy * dy > slop * slop)) {
                    dragging = true
                    lp.x = origX + dx.toInt()
                    lp.y = origY + dy.toInt()
                    try {
                        wm.updateViewLayout(view, lp)
                    } catch (_: Exception) {}
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) {
                    onTap()
                } else {
                    snapToEdge()
                }
            }
        }
        return true
    }

    private fun snapToEdge() {
        val screenWidth = view.resources.displayMetrics.widthPixels
        val viewWidth = if (view.width > 0) view.width else lp.width
        lp.x = if (lp.x + viewWidth / 2 < screenWidth / 2) {
            0
        } else {
            screenWidth - viewWidth
        }
        try {
            wm.updateViewLayout(view, lp)
        } catch (_: Exception) {}
    }
}
