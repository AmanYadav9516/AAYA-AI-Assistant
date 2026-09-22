package com.aaya.assistant.engine.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class FloatingOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var statusTextView: TextView? = null
    private var dismissRunnable: Runnable? = null

    var onOverlayTapped: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show(initialText: String = "Hey, AAYA is listening...") {
        if (!canDrawOverlays()) return

        mainHandler.post {
            if (isOverlayShowing && overlayView != null) {
                updateText(initialText)
                scheduleAutoDismiss(6000)
                return@post
            }

            try {
                val layoutParams = WindowManager.LayoutParams().apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                    type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }
                    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    format = PixelFormat.TRANSLUCENT
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 60
                }

                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(36, 24, 36, 24)

                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 80f
                        setColor(Color.parseColor("#E60A0D14")) // Semi-transparent dark glass
                        setStroke(3, Color.parseColor("#00E5FF")) // Neon Cyan border
                    }
                    background = bgDrawable
                    elevation = 20f
                }

                // Glowing Dot / Indicator
                val dotView = View(context).apply {
                    val dotParams = LinearLayout.LayoutParams(24, 24).apply {
                        marginEnd = 24
                    }
                    layoutParams = dotParams
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#00E5FF"))
                    }
                }
                container.addView(dotView)

                // Assistant Text
                statusTextView = TextView(context).apply {
                    val textParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                    layoutParams = textParams
                    text = initialText
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    maxLines = 3
                }
                container.addView(statusTextView)

                // Close Button
                val closeButton = TextView(context).apply {
                    val closeParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = 16
                    }
                    layoutParams = closeParams
                    text = "✕"
                    setTextColor(Color.parseColor("#80FFFFFF"))
                    textSize = 16f
                    setPadding(12, 8, 12, 8)
                    setOnClickListener {
                        hide()
                    }
                }
                container.addView(closeButton)

                container.setOnClickListener {
                    onOverlayTapped?.invoke()
                }

                windowManager.addView(container, layoutParams)
                overlayView = container
                isOverlayShowing = true

                scheduleAutoDismiss(7000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateText(newText: String) {
        mainHandler.post {
            statusTextView?.text = newText
            scheduleAutoDismiss(6000)
        }
    }

    fun hide() {
        mainHandler.post {
            dismissRunnable?.let { mainHandler.removeCallbacks(it) }
            if (isOverlayShowing && overlayView != null) {
                try {
                    windowManager.removeView(overlayView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                overlayView = null
                statusTextView = null
                isOverlayShowing = false
                onDismiss?.invoke()
            }
        }
    }

    private fun scheduleAutoDismiss(delayMs: Long) {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissRunnable = Runnable { hide() }
        mainHandler.postDelayed(dismissRunnable!!, delayMs)
    }

    fun isShowing(): Boolean = isOverlayShowing
}
