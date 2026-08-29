package com.hacx.fpsmeter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class FpsOverlayService : Service() {

    companion object {
        var isRunning = false
    }

    private lateinit var windowManager: WindowManager
    private lateinit var fpsView: FpsView
    private lateinit var overlayParams: WindowManager.LayoutParams
    private val CHANNEL_ID = "fps_overlay_channel"
    private val NOTIFICATION_ID = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlay()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("FPS: --"))
    }

    private fun getPrefs() = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)

    private fun createOverlay() {
        fpsView = FpsView(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 200
        }

        overlayParams = params
        windowManager.addView(fpsView, params)
    }

    inner class FpsView(context: Context) : View(context) {

        private val fpsPaint = Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(6f, 2f, 2f, Color.BLACK)
        }

        private val bgPaint = Paint().apply {
            color = Color.parseColor("#CC000000")
        }

        private val subPaint = Paint().apply {
            isAntiAlias = true
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        private var fps = 0
        private var frameTimeMs = 0.0
        private var frameCount = 0
        private var lastFpsTime = System.nanoTime()
        private val handler = Handler(Looper.getMainLooper())

        private var touchStartX = 0f
        private var touchStartY = 0f
        private var paramStartX = 0
        private var paramStartY = 0
        private var isDragging = false

        private var viewWidth = 0f
        private var viewHeight = 0f

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        init {
            isClickable = true
            isFocusable = true
            Choreographer.getInstance().postFrameCallback(frameCallback)

            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (!isRunning) return
                    val now = System.nanoTime()
                    val elapsed = now - lastFpsTime
                    if (elapsed > 0) {
                        fps = (frameCount * 1_000_000_000.0 / elapsed).toInt()
                        frameTimeMs = if (fps > 0) 1000.0 / fps else 0.0
                    }
                    frameCount = 0
                    lastFpsTime = now

                    val prefs = getPrefs()
                    val color = prefs.getString(SettingsActivity.KEY_COLOR, SettingsActivity.COLOR_GREEN) ?: SettingsActivity.COLOR_GREEN
                    val textSizeSp = prefs.getInt(SettingsActivity.KEY_TEXT_SIZE, 20)

                    fpsPaint.color = Color.parseColor(color)
                    fpsPaint.textSize = textSizeSp * resources.displayMetrics.scaledDensity

                    subPaint.color = Color.parseColor("#AAAAAA")
                    subPaint.textSize = (textSizeSp * 0.55f) * resources.displayMetrics.scaledDensity

                    requestLayout()
                    invalidate()

                    val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mgr.notify(NOTIFICATION_ID, buildNotification("FPS: $fps"))

                    handler.postDelayed(this, 1000)
                }
            }, 1000)
        }

        override fun onMeasure(widthSpec: Int, heightSpec: Int) {
            val density = resources.displayMetrics.scaledDensity
            val fpsText = "$fps"
            val ftText = "${String.format("%.1f", frameTimeMs)} ms"

            val fpsWidth = fpsPaint.measureText(fpsText)
            val ftWidth = subPaint.measureText(ftText)
            val maxTextWidth = maxOf(fpsWidth, ftWidth)

            val padH = 20f * density
            val padV = 12f * density

            val textHeight = fpsPaint.textSize
            val subLineHeight = if (getPrefs().getBoolean(SettingsActivity.KEY_SHOW_FRAMETIME, true)) subPaint.textSize + (4f * density) else 0f

            val totalWidth = (maxTextWidth + padH * 2).toInt()
            val totalHeight = (textHeight + subLineHeight + padV * 2).toInt()

            viewWidth = totalWidth.toFloat()
            viewHeight = totalHeight.toFloat()

            setMeasuredDimension(totalWidth, totalHeight)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    paramStartX = overlayParams.x
                    paramStartY = overlayParams.y
                    isDragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    if (dx * dx + dy * dy > 225) isDragging = true
                    if (isDragging) {
                        val metrics = resources.displayMetrics
                        val screenW = metrics.widthPixels
                        val screenH = metrics.heightPixels

                        var newX = (paramStartX + dx).toInt()
                        var newY = (paramStartY + dy).toInt()

                        newX = newX.coerceIn(0, screenW - viewWidth.toInt())
                        newY = newY.coerceIn(0, screenH - viewHeight.toInt())

                        overlayParams.x = newX
                        overlayParams.y = newY
                        windowManager.updateViewLayout(this@FpsView, overlayParams)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> return true
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val prefs = getPrefs()
            val showBg = prefs.getBoolean(SettingsActivity.KEY_SHOW_BG, true)
            val showFt = prefs.getBoolean(SettingsActivity.KEY_SHOW_FRAMETIME, true)

            val density = resources.displayMetrics.scaledDensity
            val padH = 20f * density
            val padV = 12f * density

            val w = width.toFloat()
            val h = height.toFloat()

            if (showBg) {
                canvas.drawRoundRect(RectF(0f, 0f, w, h), 12f * density, 12f * density, bgPaint)
            }

            val fpsY = padV + fpsPaint.textSize * 0.8f
            canvas.drawText("$fps", padH, fpsY, fpsPaint)

            if (showFt) {
                val ftY = fpsY + fpsPaint.textSize * 0.2f + subPaint.textSize
                canvas.drawText("${String.format("%.1f", frameTimeMs)} ms", padH, ftY, subPaint)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "FPS Overlay", NotificationManager.IMPORTANCE_LOW).apply {
            description = "FPS overlay notification"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val showNotif = getPrefs().getBoolean(SettingsActivity.KEY_SHOW_NOTIFICATION, true)
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("FPS Meter")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { windowManager.removeView(fpsView) } catch (_: Exception) {}
    }
}
