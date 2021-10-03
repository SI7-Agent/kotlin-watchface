package com.si7agent.digital_neon.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import com.si7agent.digital_neon.model.DigitalNeonWatchFaceStyle
import com.si7agent.digital_neon.model.WatchFaceBackgroundImage

import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.TimeZone

/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

/**
 * This is a helper class which renders an analog watch face based on a data object passed in
 * representing the style. The class implements all best practices for watch faces, so the
 * developer can just focus on designing the watch face they want.
 *
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the
 * [Watch Face Code Lab](https://codelabs.developers.google.com/codelabs/watchface/index.html#0)
 */
abstract class AbstractKotlinWatchFace : CanvasWatchFaceService() {

    private lateinit var digitalNeonWatchFaceStyle: DigitalNeonWatchFaceStyle

    abstract fun getWatchFaceStyle(): DigitalNeonWatchFaceStyle

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: AbstractKotlinWatchFace.Engine) : Handler(Looper.getMainLooper()) {
        private val weakReference: WeakReference<AbstractKotlinWatchFace.Engine> =
            WeakReference(reference)

        override fun handleMessage(msg: Message) {

        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

        }

        private fun initializeBackground() {

        }

        private fun initializeWatchFace() {

        }

        override fun onDestroy() {
            super.onDestroy()

        }

        override fun onPropertiesChanged(properties: Bundle) {

        }

        override fun onTimeTick() {

        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {

        }

        private fun updateWatchHandStyle() {

        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {

        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

        }

        private fun initGrayBackgroundBitmap() {

        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {

        }

        private fun drawBackground(canvas: Canvas) {

        }

        private fun drawWatchFace(canvas: Canvas) {

        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

        }

        private fun registerReceiver() {

        }

        private fun unregisterReceiver() {

        }

        /**
         * Starts/stops the [.updateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {

        }

        /**
         * Returns whether the [.updateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return true
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {

        }
    }
}
