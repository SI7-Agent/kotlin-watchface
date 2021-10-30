package com.si7agent.digital_neon.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.*
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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

    var specW: Int = 0
    var specH: Int = 0
    lateinit var watchLayout: View

    protected var currentTheme: Int = 4
    protected var currentFont: Int = 1
    protected var tools: MiscTools = MiscTools()

    private var TAG = "digital_neon"

    private lateinit var digitalNeonWatchFaceStyle: DigitalNeonWatchFaceStyle
    abstract fun getWatchFaceStyle(): DigitalNeonWatchFaceStyle

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: AbstractKotlinWatchFace.Engine): Handler(Looper.getMainLooper()) {
        private val weakReference: WeakReference<AbstractKotlinWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = weakReference.get()
            if (engine != null) {
                when (msg.what) {
                    com.si7agent.digital_neon.service.MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {
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
        private var muteMode: Boolean = false

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

            initializeLayout()
            initializeThemeImages()
            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeLayout() {
            val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            watchLayout = inflater.inflate(R.layout.watchface_view, null)
        }

        private fun initializeBackground() {
            Log.d(TAG, "initializeBackground: ")

            backgroundPaint = Paint().apply {
                color = ContextCompat.getColor(applicationContext, R.color.default_bg2)
            }

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
                val bitmap = Bitmap.createBitmap(360, 360, Bitmap.Config.ALPHA_8)
                val canvas = Canvas(bitmap)
                canvas.drawRect(0f, 0f, 360f, 360f, backgroundPaint)
                backgroundBitmap = bitmap
            }
        }

        private fun initializeWatchFace() {
            val hourText = watchLayout.findViewById<TextView>(R.id.hourTextView)
            hourText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            hourText.textSize = digitalNeonWatchFaceStyle.font.hourSize.toFloat()
            hourText.setTextColor(ContextCompat.getColor(applicationContext, digitalNeonWatchFaceStyle.theme.labelColor))
            hourText.setShadowLayer(8f, 0f, 0f, ContextCompat.getColor(applicationContext, R.color.hour_shade))

            val minuteText = watchLayout.findViewById<TextView>(R.id.minuteTextView)
            minuteText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            minuteText.textSize = digitalNeonWatchFaceStyle.font.minuteSize.toFloat()
            minuteText.setTextColor(ContextCompat.getColor(applicationContext, digitalNeonWatchFaceStyle.theme.labelColor))
            //minuteText.setShadowLayer(8f, 0f, 0f, ContextCompat.getColor(applicationContext, R.color.minute_shade))

            val secondText = watchLayout.findViewById<TextView>(R.id.secondTextView)
            secondText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            secondText.textSize = digitalNeonWatchFaceStyle.font.secondSize.toFloat()
            secondText.setTextColor(ContextCompat.getColor(applicationContext, digitalNeonWatchFaceStyle.theme.labelColor))
            //secondText.setShadowLayer(8f, 0f, 0f, ContextCompat.getColor(applicationContext, R.color.second_shade))

            val dayText = watchLayout.findViewById<TextView>(R.id.dayTextView)
            dayText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            dayText.textSize = digitalNeonWatchFaceStyle.font.labelSize.toFloat()
            dayText.setTextColor(ContextCompat.getColor(applicationContext, R.color.default_bg))

            val monthText = watchLayout.findViewById<TextView>(R.id.monthTextView)
            monthText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            monthText.textSize = digitalNeonWatchFaceStyle.font.labelSize.toFloat()
            monthText.setTextColor(ContextCompat.getColor(applicationContext, digitalNeonWatchFaceStyle.theme.labelColor))

            val weekDayText = watchLayout.findViewById<TextView>(R.id.weekDayTextView)
            weekDayText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            weekDayText.textSize = digitalNeonWatchFaceStyle.font.labelSize.toFloat()
            weekDayText.setTextColor(ContextCompat.getColor(applicationContext, digitalNeonWatchFaceStyle.theme.labelColor))

            val stepText = watchLayout.findViewById<TextView>(R.id.stepTextView)
            stepText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            stepText.textSize = digitalNeonWatchFaceStyle.font.labelSize.toFloat()
            stepText.setTextColor(ContextCompat.getColor(applicationContext, digitalNeonWatchFaceStyle.theme.labelColor))

            val hrmText = watchLayout.findViewById<TextView>(R.id.hrmTextView)
            hrmText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            hrmText.textSize = digitalNeonWatchFaceStyle.font.labelSize.toFloat()
            hrmText.setTextColor(ContextCompat.getColor(applicationContext, digitalNeonWatchFaceStyle.theme.labelColor))

            val batteryText = watchLayout.findViewById<TextView>(R.id.batteryTextView)
            batteryText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            batteryText.textSize = digitalNeonWatchFaceStyle.font.labelSize.toFloat()
            batteryText.setTextColor(ContextCompat.getColor(applicationContext, R.color.battery_charging))
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
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            ambient = inAmbientMode

            updateWatchHandStyle()
            updateTimer()
        }

        private fun updateWatchHandStyle() {

        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            if (muteMode != inMuteMode) {
                muteMode = inMuteMode
                //hourPaint.alpha = if (inMuteMode) 100 else 255
                //minutePaint.alpha = if (inMuteMode) 100 else 255
                //secondPaint.alpha = if (inMuteMode) 80 else 255
                invalidate()
            }
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

            val currentTime = System.currentTimeMillis()
            calendar.timeInMillis = currentTime

            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            Log.d(TAG, "drawBackground: ")
            tools.putImageOnCanvas(canvas, backgroundBitmap, 0, 0)
        }

        private fun drawWatchFace(canvas: Canvas) {
            setGraphic()
            setTime()
            setDate()
            setBattery()

            watchLayout.measure(specW, specH)
            watchLayout.layout(0, 0, watchLayout.measuredWidth, watchLayout.measuredHeight)
            watchLayout.draw(canvas)
        }

        private fun setGraphic() {
            val icAppLauncher = themeResources[digitalNeonWatchFaceStyle.theme.themeNamePic]?.get("ic_app_launcher")
            val neonBg = themeResources[digitalNeonWatchFaceStyle.theme.themeNamePic]?.get("neon_bg")
            val watch = themeResources[digitalNeonWatchFaceStyle.theme.themeNamePic]?.get("watch")

            val appLauncherImageView = watchLayout.findViewById<ImageView>(R.id.appLauncherImageView)
            appLauncherImageView.setImageBitmap(icAppLauncher)

            val neonBgDayZoneImageView = watchLayout.findViewById<ImageView>(R.id.neonBgDayZoneImageView)
            neonBgDayZoneImageView.setImageBitmap(neonBg)

            val neonBgHrmZoneImageView = watchLayout.findViewById<ImageView>(R.id.neonBgHrmZoneImageView)
            neonBgHrmZoneImageView.setImageBitmap(neonBg)

            val neonBgStepZoneImageView = watchLayout.findViewById<ImageView>(R.id.neonBgStepZoneImageView)
            neonBgStepZoneImageView.setImageBitmap(neonBg)

            val secondMoverImageView = watchLayout.findViewById<ImageView>(R.id.secondMoverImageView)
            secondMoverImageView.setImageBitmap(neonBg)

            val watchZero = watchLayout.findViewById<ImageView>(R.id.watchZeroImageView)
            watchZero.setImageBitmap(watch)

            val watchThirty = watchLayout.findViewById<ImageView>(R.id.watchThirtyImageView)
            watchThirty.setImageBitmap(watch)

            val watchSixty = watchLayout.findViewById<ImageView>(R.id.watchSixtyImageView)
            watchSixty.setImageBitmap(watch)
        }

        private fun setTime() {
            val hourText = watchLayout.findViewById<TextView>(R.id.hourTextView)
            hourText.text = tools.intToStr(calendar.get(Calendar.HOUR_OF_DAY))

            val minuteText = watchLayout.findViewById<TextView>(R.id.minuteTextView)
            minuteText.text = tools.intToStr(calendar.get(Calendar.MINUTE))

            val secondText = watchLayout.findViewById<TextView>(R.id.secondTextView)
            secondText.text = tools.intToStr(calendar.get(Calendar.SECOND))
        }

        private fun setDate() {
            val weekDayText = watchLayout.findViewById<TextView>(R.id.weekDayTextView)
            weekDayText.text = resources.getStringArray(R.array.week_days)[calendar.get(Calendar.DAY_OF_WEEK) - 1]

            val monthText = watchLayout.findViewById<TextView>(R.id.monthTextView)
            monthText.text = resources.getStringArray(R.array.months)[calendar.get(Calendar.MONTH)]

            val dayText = watchLayout.findViewById<TextView>(R.id.dayTextView)
            dayText.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        }

        private fun setBattery() {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.isCharging
            val batteryText = watchLayout.findViewById<TextView>(R.id.batteryTextView)

            if (isCharging) {
                batteryText.setTextColor(ContextCompat.getColor(applicationContext, R.color.battery_charging))
            }
            else if (batteryLevel == 100) {
                batteryText.setTextColor(ContextCompat.getColor(applicationContext, R.color.battery_full))
            }
            else {
                when ((batteryLevel/10)%10) {
                    0, 1, 2 -> batteryText.setTextColor(ContextCompat.getColor(applicationContext, R.color.battery_empty))
                    3, 4, 5, 6, 7 -> batteryText.setTextColor(ContextCompat.getColor(applicationContext, R.color.battery_middle))
                    else -> batteryText.setTextColor(ContextCompat.getColor(applicationContext, R.color.battery_high))
                }
            }

            batteryText.text = "${batteryLevel}%"
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

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)

            specW = View.MeasureSpec.makeMeasureSpec(screenSize["width"]!!, View.MeasureSpec.EXACTLY)
            specH = View.MeasureSpec.makeMeasureSpec(screenSize["height"]!!, View.MeasureSpec.EXACTLY)
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
