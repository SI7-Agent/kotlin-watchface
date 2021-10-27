package com.si7agent.digital_neon.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import com.si7agent.digital_neon.R
import com.si7agent.digital_neon.model.DigitalNeonWatchFaceStyle
import com.si7agent.digital_neon.model.WatchFaceBackgroundImage
import java.lang.ref.WeakReference
import java.util.*

private const val INTERACTIVE_UPDATE_RATE_MS = 1000
private const val MSG_UPDATE_TIME = 0

private const val LAST_FONT = 5
private const val LAST_THEME = 4

abstract class AbstractKotlinWatchFace : CanvasWatchFaceService() {

    protected var currentTheme: Int = 1
    protected var currentFont: Int = 1
    protected var tools: MiscTools = MiscTools()

    private var TAG = "digital_neon"

    private lateinit var digitalNeonWatchFaceStyle: DigitalNeonWatchFaceStyle
    abstract fun getWatchFaceStyle(): DigitalNeonWatchFaceStyle

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: AbstractKotlinWatchFace.Engine): Handler(Looper.getMainLooper()) {
        private val weakReference: WeakReference<AbstractKotlinWatchFace.Engine> =
            WeakReference(reference)

        override fun handleMessage(msg: Message) {

        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        //private lateinit var hourStyle
        //private lateinit var minuteStyle
        //private lateinit var secondStyle
        //private lateinit var textPaint

        private lateinit var themeResources: MutableMap<String, MutableMap<String, Bitmap>>

        private var registeredTimeZoneReceiver = false
        private val updateTimeHandler = EngineHandler(this)
        private lateinit var calendar: Calendar
        private lateinit var screenSize: MutableMap<String, Int>

        private var isBackgroundImageEnabled: Boolean = false
        private lateinit var backgroundBitmap: Bitmap
        private lateinit var grayBackgroundBitmap: Bitmap
        private lateinit var backgroundPaint: Paint

        private var burnInProtection: Boolean = false
        private var ambient: Boolean = false
        private var lowBitAmbient: Boolean = false

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            digitalNeonWatchFaceStyle = getWatchFaceStyle()

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@AbstractKotlinWatchFace)
                    .setAcceptsTapEvents(true)
                    .build()
            )

            calendar = Calendar.getInstance()

            Log.d(TAG, "onCreate: ")

            initializeThemeImages()
            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            Log.d(TAG, "initializeBackground: ")
            isBackgroundImageEnabled =
                digitalNeonWatchFaceStyle.backgroundImage.backgroundImageResource !=
                        WatchFaceBackgroundImage.EMPTY_IMAGE_RESOURCE

            if (isBackgroundImageEnabled) {
                backgroundBitmap = BitmapFactory.decodeResource(
                    resources,
                    digitalNeonWatchFaceStyle.backgroundImage.backgroundImageResource
                )
            }
            else {
                val colors = IntArray(4)
                colors[0] = 255
                colors[1] = 255
                colors[2] = 255
                colors[3] = 255
                backgroundBitmap = Bitmap.createBitmap(colors, 360, 360, Bitmap.Config.ALPHA_8)
            }

            backgroundPaint = Paint().apply {
                color = Color.BLACK
            }
        }

        private fun initializeWatchFace() {

        }

        private fun initializeThemeImages() {
            themeResources = mutableMapOf()
            var state = 1

            for (i in tools.getIndexesFromTypedArray(resources, R.array.themes)) {
                val currentTheme = "theme$state"

                val themeResourcesIndexes = tools.getIndexesFromTypedArray(resources, i)
                val themeResourcesBitmaps = mutableMapOf(
                    "ic_app_launcher" to tools.createBitmapFromId(resources, themeResourcesIndexes[0]),
                    "neon_bg" to tools.createBitmapFromId(resources, themeResourcesIndexes[1]),
                    "second_mover" to tools.createBitmapFromId(resources, themeResourcesIndexes[1]),
                    "watch" to tools.createBitmapFromId(resources, themeResourcesIndexes[2]),
                )

                themeResources[currentTheme] = themeResourcesBitmaps
                state++
            }
        }

        override fun onDestroy() {
            super.onDestroy()

        }

        override fun onPropertiesChanged(properties: Bundle) {

        }

        override fun onTimeTick() {

        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            ambient = inAmbientMode
        }

        private fun updateWatchHandStyle() {

        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {

        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceChanged: ")

            screenSize = mutableMapOf(
                "width" to width,
                "height" to height
            )

            val scale = width.toFloat() / backgroundBitmap.width.toFloat()
            val icAppLauncherScale = scale * 0.1225f
            val neonBgScale = scale * 1.1f
            val secondMoverScale = scale * 0.5f

            if (isBackgroundImageEnabled)
            {
                backgroundBitmap = tools.scaleBitmap(backgroundBitmap, scale, scale)

                if (!burnInProtection && !lowBitAmbient) {
                    initGrayBackgroundBitmap()
                }
            }

            for (i in themeResources.keys) {
                themeResources[i]?.get("ic_app_launcher")?.let { icAppLauncher ->
                    tools.scaleBitmap(icAppLauncher, icAppLauncherScale, icAppLauncherScale)
                }?.let { icAppLauncherScaled ->
                    themeResources[i]?.set("ic_app_launcher", icAppLauncherScaled)
                }

                themeResources[i]?.get("neon_bg")?.let { neonBg ->
                    tools.scaleBitmap(neonBg, neonBgScale, neonBgScale)
                }?.let { neonBgScaled ->
                    themeResources[i]?.set("neon_bg", neonBgScaled)
                }

                themeResources[i]?.get("second_mover")?.let { secondMover ->
                    tools.scaleBitmap(secondMover, secondMoverScale, secondMoverScale)
                }?.let { secondMoverScaled ->
                    themeResources[i]?.set("second_mover", secondMoverScaled)
                }

                themeResources[i]?.get("watch")?.let { watch ->
                    tools.scaleBitmap(watch, scale, scale)
                }?.let { watchScaled ->
                    themeResources[i]?.set("watch", watchScaled)
                }
            }
        }

        private fun initGrayBackgroundBitmap() {
            grayBackgroundBitmap = Bitmap.createBitmap(
                backgroundBitmap.width,
                backgroundBitmap.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(grayBackgroundBitmap)
            val grayPaint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(colorMatrix)
            grayPaint.colorFilter = filter
            canvas.drawBitmap(backgroundBitmap, 0f, 0f, grayPaint)
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            Log.d(TAG, "onDraw: ")
            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            Log.d(TAG, "drawBackground: ")
            tools.putImageOnCanvas(canvas, backgroundBitmap, 0, 0)
        }

        private fun drawWatchFace(canvas: Canvas) {
            val icAppLauncher = themeResources[digitalNeonWatchFaceStyle.theme.themeNamePic]?.get("ic_app_launcher")
            val neonBg = themeResources[digitalNeonWatchFaceStyle.theme.themeNamePic]?.get("neon_bg")
            val secondMover = themeResources[digitalNeonWatchFaceStyle.theme.themeNamePic]?.get("second_mover")
            val watch = themeResources[digitalNeonWatchFaceStyle.theme.themeNamePic]?.get("watch")

            if (icAppLauncher != null) {
                screenSize["width"]?.times(0.5f)?.let { left ->
                    (screenSize["height"]?.times(0.15f))?.let { top ->
                        tools.putImageOnCanvas(canvas, icAppLauncher,
                            (left - icAppLauncher.width * 0.5f).toInt(), (top - icAppLauncher.width * 0.5f).toInt())
                    }
                }
            }

            if (neonBg != null) {
                screenSize["width"]?.times(0.25f)?.let { left ->
                    (screenSize["height"]?.times(0.77f))?.let { top ->
                        tools.putImageOnCanvas(canvas, neonBg,
                            (left - neonBg.width * 0.5f).toInt(), (top - neonBg.width * 0.5f).toInt())
                    }
                }

                screenSize["width"]?.times(0.75f)?.let { left ->
                    (screenSize["height"]?.times(0.77f))?.let { top ->
                        tools.putImageOnCanvas(canvas, neonBg,
                            (left - neonBg.width * 0.5f).toInt(), (top - neonBg.width * 0.5f).toInt())
                    }
                }

                screenSize["width"]?.times(0.87f)?.let { left ->
                    (screenSize["height"]?.times(0.5f))?.let { top ->
                        tools.putImageOnCanvas(canvas, neonBg,
                            (left - neonBg.width * 0.5f).toInt(), (top - neonBg.width * 0.5f).toInt())
                    }
                }
            }

            if (watch != null) {
                tools.putImageOnCanvas(canvas, watch, 0, 0)
                //putImageOnCanvas(canvas, watch, 0, 0) - rotated by 30 degrees
                //putImageOnCanvas(canvas, watch, 0, 0) - rotated by 60 degrees
            }

            if (secondMover != null) {
                screenSize["width"]?.times(0.5f)?.let { left ->
                        tools.putImageOnCanvas(canvas, secondMover,
                            (left - secondMover.width * 0.5f).toInt(), 0)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun changeFont() {
            if (currentFont == LAST_FONT)
                currentFont = 1
            else
                currentFont++

            digitalNeonWatchFaceStyle = getWatchFaceStyle()
            //setFont(currentFont)
        }



        private fun changeTheme() {
            if (currentTheme == LAST_THEME)
                currentTheme = 1
            else
                currentTheme++

            digitalNeonWatchFaceStyle = getWatchFaceStyle()
            //setTheme(currentTheme)
        }

        private fun registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@AbstractKotlinWatchFace.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = false
            this@AbstractKotlinWatchFace.unregisterReceiver(timeZoneReceiver)
        }

        /**
         * Starts/stops the [.updateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.updateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !ambient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}
