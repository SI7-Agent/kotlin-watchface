package com.si7agent.digital_neon.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.si7agent.digital_neon.R
import com.si7agent.digital_neon.model.DigitalNeonWatchFaceStyle
import com.si7agent.digital_neon.model.WatchFaceBackgroundImage
import com.si7agent.digital_neon.RequestActivity
import java.lang.Math.toRadians
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

private const val INTERACTIVE_UPDATE_RATE_MS = 1000
private const val MSG_UPDATE_TIME = 0
private const val DOUBLE_TAP_DELAY = 250

private const val LAST_FONT = 4
private const val LAST_THEME = 4

abstract class AbstractKotlinWatchFace : CanvasWatchFaceService() {

    var specW: Int = 0
    var specH: Int = 0
    lateinit var watchLayout: View

    var timeStart = 0L
    var timeEnd = 0L

    protected var currentTheme: Int = 1
    protected var currentFont: Int = 1
    protected var tools: MiscTools = MiscTools(this@AbstractKotlinWatchFace)

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

    inner class Engine : CanvasWatchFaceService.Engine(), SensorEventListener {

        private var sensorManager: SensorManager? = null
        private var stepSensor: Sensor? = null
        private var hrmSensor: Sensor? = null
        private var isStepSensorRunning = false
        private var isHrmSensorRunning = false
        private var isStepSensorReseted = false
        private var totalSteps = 0f
        private var previousTotalSteps = 0f
        private var hrmValue = 0f

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

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Log.d(TAG, "onAccuracyChanged: $accuracy")
        }

        override fun onSensorChanged(event: SensorEvent?) {
            when(event?.sensor?.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    if (!isStepSensorReseted) {
                        previousTotalSteps = event.values[0]
                        isStepSensorReseted = true
                    }

                    if (isStepSensorRunning) {
                        totalSteps = event.values[0]
                        val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()

                        Log.d(TAG, "onSensorChanged: $currentSteps")
                    }
                }
                Sensor.TYPE_HEART_RATE -> {
                    if (isHrmSensorRunning) {
                        hrmValue = event.values[0]

                        Log.d(TAG, "onSensorChanged hrm: $hrmValue")

                        if (hrmValue.toInt() != 0)
                            sensorManager?.unregisterListener(this, event.sensor)
                    }
                }
                else -> Log.d(TAG, "unknown sensor event defined")
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

            initializeLayout()
            initializeThemeImages()
            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeSensors() {
            val intent = Intent(applicationContext, RequestActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            isStepSensorRunning = true
            isHrmSensorRunning = true

            stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)

            hrmSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
            sensorManager?.registerListener(this, hrmSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        private fun initializeLayout() {
            val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            watchLayout = inflater.inflate(R.layout.watchface_view, null)
        }

        private fun initializeBackground() {
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
            minuteText.setShadowLayer(3f, 0f, 0f, ContextCompat.getColor(applicationContext, R.color.minute_shade))

            val secondText = watchLayout.findViewById<TextView>(R.id.secondTextView)
            secondText.typeface = resources.getFont(digitalNeonWatchFaceStyle.font.font)
            secondText.textSize = digitalNeonWatchFaceStyle.font.secondSize.toFloat()
            secondText.setTextColor(ContextCompat.getColor(applicationContext, digitalNeonWatchFaceStyle.theme.labelColor))
            secondText.setShadowLayer(3f, 0f, 0f, ContextCompat.getColor(applicationContext, R.color.second_shade))

            val dayText = watchLayout.findViewById<TextView>(R.id.dayTextView)
            dayText.setTypeface(resources.getFont(digitalNeonWatchFaceStyle.font.font), Typeface.BOLD)
            dayText.textSize = (digitalNeonWatchFaceStyle.font.labelSize+3).toFloat()
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

            for (i in tools.getIndexesFromTypedArray(R.array.themes)) {
                val currentTheme = "theme$state"

                val themeResourcesIndexes = tools.getIndexesFromTypedArray(i)
                val themeResourcesBitmaps = mutableMapOf(
                    "ic_app_launcher" to tools.createBitmapFromId(themeResourcesIndexes[0]),
                    "neon_bg" to tools.createBitmapFromId(themeResourcesIndexes[1]),
                    "watch" to tools.createBitmapFromId(themeResourcesIndexes[2]),
                )

                themeResources[currentTheme] = themeResourcesBitmaps
                state++
            }
        }

        override fun onDestroy() {
            updateTimeHandler.removeMessages(com.si7agent.digital_neon.service.MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            lowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            burnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
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
            when (ambient) {
                true -> {
                    watchLayout.findViewById<ImageView>(R.id.watchZeroImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.watchThirtyImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.watchSixtyImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.secondMoverImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.neonBgDayZoneImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<TextView>(R.id.dayTextView).setTextColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.ambient_mode_text
                        )
                    )
                    watchLayout.findViewById<TextView>(R.id.monthTextView).setTextColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.ambient_mode_text
                        )
                    )
                    watchLayout.findViewById<TextView>(R.id.weekDayTextView).setTextColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.ambient_mode_text
                        )
                    )
                    watchLayout.findViewById<TextView>(R.id.hourTextView).apply {
                        setTextColor(
                            ContextCompat.getColor(
                                applicationContext,
                                R.color.ambient_mode_text
                            )
                        )
                        setShadowLayer(0f, 0f, 0f, ContextCompat.getColor(applicationContext, R.color.ambient_mode_text))
                    }
                    watchLayout.findViewById<TextView>(R.id.minuteTextView).apply{
                        setTextColor(
                            ContextCompat.getColor(
                                applicationContext,
                                R.color.ambient_mode_text
                            )
                        )
                        textSize = digitalNeonWatchFaceStyle.font.hourSize.toFloat()
                        scaleX = 1.1f
                        scaleY = 1.3f
                        minHeight = 0
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                    val params = watchLayout.findViewById<TextView>(R.id.minuteTextView).getLayoutParams() as LinearLayout.LayoutParams
                    params.setMargins(tools.dpToPx(4f), 0, 0, 0)
                    watchLayout.findViewById<TextView>(R.id.minuteTextView).setLayoutParams(params)

                    watchLayout.findViewById<TextView>(R.id.secondTextView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.neonBgHrmZoneImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.hrmIconImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<TextView>(R.id.hrmTextView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.neonBgStepZoneImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.stepIconImageView).visibility = View.INVISIBLE
                    watchLayout.findViewById<TextView>(R.id.stepTextView).visibility = View.INVISIBLE
                    watchLayout.findViewById<ImageView>(R.id.appLauncherImageView).visibility = View.INVISIBLE
                }
                false -> {
                    watchLayout.findViewById<ImageView>(R.id.watchZeroImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.watchThirtyImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.watchSixtyImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.secondMoverImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.neonBgDayZoneImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<TextView>(R.id.dayTextView).setTextColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.default_bg
                        )
                    )
                    watchLayout.findViewById<TextView>(R.id.monthTextView).setTextColor(
                        ContextCompat.getColor(
                            applicationContext,
                            digitalNeonWatchFaceStyle.theme.labelColor
                        )
                    )
                    watchLayout.findViewById<TextView>(R.id.weekDayTextView).setTextColor(
                        ContextCompat.getColor(
                            applicationContext,
                            digitalNeonWatchFaceStyle.theme.labelColor
                        )
                    )
                    watchLayout.findViewById<TextView>(R.id.hourTextView).apply {
                        setTextColor(
                            ContextCompat.getColor(
                                applicationContext,
                                digitalNeonWatchFaceStyle.theme.labelColor
                            )
                        )
                        setShadowLayer(8f, 0f, 0f, ContextCompat.getColor(applicationContext, R.color.hour_shade))
                    }
                    watchLayout.findViewById<TextView>(R.id.minuteTextView).apply {
                        setTextColor(
                            ContextCompat.getColor(
                                applicationContext,
                                digitalNeonWatchFaceStyle.theme.labelColor
                            )
                        )
                        textSize = digitalNeonWatchFaceStyle.font.minuteSize.toFloat()
                        scaleX = 1.5f
                        scaleY = 1.5f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            resources.getDimension(R.dimen.time_layout_minute_height).toInt()
                        )
                    }
                    val params = watchLayout.findViewById<TextView>(R.id.minuteTextView).getLayoutParams() as LinearLayout.LayoutParams
                    params.setMargins(0, 0, 0, 0)
                    watchLayout.findViewById<TextView>(R.id.minuteTextView).setLayoutParams(params)

                    watchLayout.findViewById<TextView>(R.id.secondTextView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.neonBgHrmZoneImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.hrmIconImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<TextView>(R.id.hrmTextView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.neonBgStepZoneImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.stepIconImageView).visibility = View.VISIBLE
                    watchLayout.findViewById<TextView>(R.id.stepTextView).visibility = View.VISIBLE
                    watchLayout.findViewById<ImageView>(R.id.appLauncherImageView).visibility = View.VISIBLE
                }
            }
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
            screenSize = mutableMapOf(
                "width" to width,
                "height" to height
            )

            initializeSensors()

            val scale = width.toFloat() / backgroundBitmap.width.toFloat()
            val icAppLauncherScale = scale * 0.1225f
            val neonBgScale = scale * 1.1f

            if (isBackgroundImageEnabled) {
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
            val currentTime = System.currentTimeMillis()
            calendar.timeInMillis = currentTime

            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            tools.putImageOnCanvas(canvas, backgroundBitmap, 0, 0)
        }

        private fun drawWatchFace(canvas: Canvas) {
            tools.clearCanvas(canvas)

            setGraphic()
            setTime()
            setSecondMover(calendar.get(Calendar.SECOND))
            setDate()
            setSensorValue()
            setBattery()

            watchLayout.measure(specW, specH)
            watchLayout.layout(0, 0, watchLayout.measuredWidth, watchLayout.measuredHeight)
            watchLayout.draw(canvas)

            when (calendar.get(Calendar.HOUR_OF_DAY)) {
                0 -> {
                    previousTotalSteps = totalSteps
                    isStepSensorReseted = true
                }
                2 -> sensorManager?.registerListener(this, hrmSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
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

        private fun setSecondMover(sec: Int) {
            val secondMoverImageView = watchLayout.findViewById<ImageView>(R.id.secondMoverImageView)
            val backCountedSeconds = if (sec>14) sec-15 else 60+sec-15

            val radiusX = (screenSize["width"]!! - resources.getDimension(R.dimen.neon_bg_second_mover_icon_width))/2
            val radiusY = (screenSize["height"]!! - resources.getDimension(R.dimen.neon_bg_second_mover_icon_height))/2

            secondMoverImageView.translationX = radiusX*cos(toRadians(6.0*backCountedSeconds)).toFloat()
            secondMoverImageView.translationY = radiusY + radiusY*sin(toRadians(6.0*backCountedSeconds)).toFloat()
        }

        private fun setDate() {
            val weekDayText = watchLayout.findViewById<TextView>(R.id.weekDayTextView)
            weekDayText.text = resources.getStringArray(R.array.week_days)[calendar.get(Calendar.DAY_OF_WEEK) - 1]

            val monthText = watchLayout.findViewById<TextView>(R.id.monthTextView)
            monthText.text = resources.getStringArray(R.array.months)[calendar.get(Calendar.MONTH)]

            val dayText = watchLayout.findViewById<TextView>(R.id.dayTextView)
            dayText.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        }

        private fun setSensorValue() {
            val stepText = watchLayout.findViewById<TextView>(R.id.stepTextView)
            stepText.text = (totalSteps - previousTotalSteps).toInt().toString()

            val hrmText = watchLayout.findViewById<TextView>(R.id.hrmTextView)
            hrmText.text = hrmValue.toInt().toString()
        }

        private fun setBattery() {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val batteryStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            val batteryText = watchLayout.findViewById<TextView>(R.id.batteryTextView)

            when (ambient) {
                false -> {
                    when (batteryStatus) {
                        BatteryManager.BATTERY_STATUS_CHARGING -> batteryText.setTextColor(
                            ContextCompat.getColor(
                                applicationContext,
                                R.color.battery_charging
                            )
                        )
                        else -> when (batteryLevel == 100) {
                            true -> batteryText.setTextColor(
                                ContextCompat.getColor(
                                    applicationContext,
                                    R.color.battery_full
                                )
                            )
                            false -> when ((batteryLevel / 10) % 10) {
                                0, 1, 2 -> batteryText.setTextColor(
                                    ContextCompat.getColor(
                                        applicationContext,
                                        R.color.battery_empty
                                    )
                                )
                                3, 4, 5, 6, 7 -> batteryText.setTextColor(
                                    ContextCompat.getColor(
                                        applicationContext,
                                        R.color.battery_middle
                                    )
                                )
                                else -> batteryText.setTextColor(
                                    ContextCompat.getColor(
                                        applicationContext,
                                        R.color.battery_high
                                    )
                                )
                            }
                        }
                    }
                }
                true -> batteryText.setTextColor(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.ambient_mode_text
                    )
                )
            }

            batteryText.text = "${batteryLevel}%"
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }
            updateTimer()
        }

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)

            specW = View.MeasureSpec.makeMeasureSpec(screenSize["width"]!!, View.MeasureSpec.EXACTLY)
            specH = View.MeasureSpec.makeMeasureSpec(screenSize["height"]!!, View.MeasureSpec.EXACTLY)
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    if (timeStart == 0L) {
                        timeStart = eventTime
                    } else {
                        timeEnd = eventTime
                        val delay = timeEnd - timeStart
                        if (delay > DOUBLE_TAP_DELAY) {
                            timeStart = timeEnd
                            timeEnd = 0L
                        } else {
                            timeStart = 0L
                            timeEnd = 0L

                            val density = resources.displayMetrics.density
                            val xDP = x / density
                            val yDP = y / density

                            when (tools.defineTouchZone(xDP.toInt(),
                                                        yDP.toInt(),
                                                        Pair(tools.pxToDp((screenSize["width"]!!/2).toFloat()),
                                                            tools.pxToDp((screenSize["height"]!!/2).toFloat())))) {
                                Zones.APPLAUNCHER_TOUCH_ZONE -> tools.openApplication("com.samsung.android.apps.wearable.recent")
                                Zones.DATE_TOUCH_ZONE -> tools.openApplication("com.samsung.android.calendar")
                                Zones.DAY_TOUCH_ZONE -> tools.openApplication("com.samsung.android.calendar")
                                Zones.TIME_TOUCH_ZONE -> changeFont()
                                Zones.STEP_TOUCH_ZONE -> tools.openApplication("com.samsung.android.wear.shealth", "com.samsung.android.wear.shealth.app.steps.view.StepsActivity")
                                Zones.HRM_TOUCH_ZONE -> tools.openApplication("com.samsung.android.wear.shealth", "com.samsung.android.wear.shealth.app.heartrate.view.HeartRateActivity")
                                else -> changeTheme()
                            }
                        }
                    }
                }
            }
            invalidate()
        }

        private fun changeFont() {
            if (currentFont == LAST_FONT)
                currentFont = 1
            else
                currentFont++

            digitalNeonWatchFaceStyle = getWatchFaceStyle()
            initializeWatchFace()
        }

        private fun changeTheme() {
            if (currentTheme == LAST_THEME)
                currentTheme = 1
            else
                currentTheme++

            digitalNeonWatchFaceStyle = getWatchFaceStyle()
            initializeWatchFace()
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

        private fun updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !ambient
        }

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
