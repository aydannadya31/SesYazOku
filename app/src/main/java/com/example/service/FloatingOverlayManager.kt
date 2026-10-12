package com.example.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.R

class FloatingOverlayManager(
    private val context: Context,
    private val onSwitchSpeaker: () -> Unit,
    private val onStopAndSave: () -> Unit
) {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isExpanded = true

    fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun show() {
        if (!isOverlayPermissionGranted() || floatingView != null) return

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 60
                y = 180
            }

            // Create floating container programmatically for reliability
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
                background = createRoundedBackground(context)
                elevation = 16f
            }

            // Header row with drag handle, timer, and minimize button
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val recIndicator = TextView(context).apply {
                text = "🔴 "
                textSize = 14f
            }
            headerRow.addView(recIndicator)

            val titleView = TextView(context).apply {
                text = "Canlı Görüşme Yakalama"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            headerRow.addView(titleView)

            val spacer = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
            }
            headerRow.addView(spacer)

            val toggleBtn = TextView(context).apply {
                text = "  [─]  "
                setTextColor(android.graphics.Color.LTGRAY)
                textSize = 13f
                setPadding(8, 0, 8, 0)
                setOnClickListener {
                    toggleExpandCollapse()
                }
            }
            headerRow.addView(toggleBtn)

            container.addView(headerRow)

            // Body container (hidden when minimized)
            val bodyContainer = LinearLayout(context).apply {
                tag = "body_container"
                orientation = LinearLayout.VERTICAL
                setPadding(0, 12, 0, 0)
            }

            // Live snippet text view
            val liveSnippetText = TextView(context).apply {
                tag = "snippet_text"
                text = "Diğer görüşme uygulamasındaki konuşmalar dinleniyor..."
                setTextColor(android.graphics.Color.parseColor("#E0E7FF"))
                textSize = 12f
                maxLines = 3
                setPadding(0, 4, 0, 10)
            }
            bodyContainer.addView(liveSnippetText)

            // Button actions row
            val actionsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val switchSpeakerBtn = TextView(context).apply {
                tag = "switch_speaker_btn"
                text = "🗣️ Konuşmacı: Ben"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 11f
                setPadding(20, 12, 20, 12)
                background = createButtonBackground(android.graphics.Color.parseColor("#4F46E5"))
                setOnClickListener {
                    onSwitchSpeaker()
                }
            }
            actionsRow.addView(switchSpeakerBtn)

            val spacer2 = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(16, 0)
            }
            actionsRow.addView(spacer2)

            val stopBtn = TextView(context).apply {
                text = "⏹️ Kaydet & Bitir"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 11f
                setPadding(20, 12, 20, 12)
                background = createButtonBackground(android.graphics.Color.parseColor("#DC2626"))
                setOnClickListener {
                    onStopAndSave()
                }
            }
            actionsRow.addView(stopBtn)

            bodyContainer.addView(actionsRow)
            container.addView(bodyContainer)

            // Touch listener for smooth dragging
            container.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isDragging = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            return false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                isDragging = true
                                params.x = initialX + dx
                                params.y = initialY + dy
                                windowManager?.updateViewLayout(container, params)
                            }
                            return isDragging
                        }
                    }
                    return false
                }
            })

            windowManager?.addView(container, params)
            floatingView = container
        } catch (e: Exception) {
            Log.e("FloatingOverlay", "Failed to add floating view", e)
        }
    }

    private fun toggleExpandCollapse() {
        val container = floatingView as? LinearLayout ?: return
        val body = container.findViewWithTag<View>("body_container") ?: return
        isExpanded = !isExpanded
        body.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    fun updateSnippet(text: String, currentSpeaker: String) {
        val container = floatingView as? LinearLayout ?: return
        val snippetView = container.findViewWithTag<TextView>("snippet_text")
        val speakerBtn = container.findViewWithTag<TextView>("switch_speaker_btn")

        snippetView?.text = text.ifBlank { "Diğer görüşme uygulamasındaki konuşmalar dinleniyor..." }
        speakerBtn?.text = "🗣️ $currentSpeaker"
    }

    fun hide() {
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e("FloatingOverlay", "Failed to remove floating view", e)
            } finally {
                floatingView = null
            }
        }
    }

    private fun createRoundedBackground(context: Context): android.graphics.drawable.Drawable {
        val shape = android.graphics.drawable.GradientDrawable()
        shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        shape.cornerRadius = 32f
        shape.setColor(android.graphics.Color.parseColor("#1E1B4B")) // Deep indigo navy
        shape.setStroke(2, android.graphics.Color.parseColor("#4338CA"))
        return shape
    }

    private fun createButtonBackground(color: Int): android.graphics.drawable.Drawable {
        val shape = android.graphics.drawable.GradientDrawable()
        shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        shape.cornerRadius = 20f
        shape.setColor(color)
        return shape
    }
}
