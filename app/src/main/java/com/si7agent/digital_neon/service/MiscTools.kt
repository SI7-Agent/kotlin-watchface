package com.si7agent.digital_neon.service

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.Log
import com.si7agent.digital_neon.R
import java.lang.Math.abs

enum class Zones {
    APPLAUNCHER_TOUCH_ZONE,
    DAY_TOUCH_ZONE,
    DATE_TOUCH_ZONE,
    TIME_TOUCH_ZONE,
    STEP_TOUCH_ZONE,
    HRM_TOUCH_ZONE,
    BG_TOUCH_ZONE
}

class MiscTools(context: Context) {
    private val c = context

    fun putImageOnCanvas(canvas: Canvas, image: Bitmap, left: Int, top: Int) {
        val paint = Paint()
        paint.color = Color.BLACK
        canvas.drawBitmap(image, left.toFloat(), top.toFloat(), paint)
    }

    fun getIndexesFromTypedArray(id: Int): Array<Int> {
        val arr = c.resources.obtainTypedArray(id)
        val arrSize = arr.length()

        val idxs: MutableList<Int> = ArrayList()
        for (i in 0 until arrSize) {
            idxs.add(arr.getResourceId(i, -1))
        }

        arr.recycle()
        return idxs.toTypedArray()
    }

    fun createBitmapFromId(id: Int): Bitmap {
        return BitmapFactory.decodeResource(
            c.resources,
            id
        )
    }

    fun setupPaintForTextDraw(font: Typeface, size: Int = 30,
                              color: Color = Color.valueOf(255f, 255f, 255f, 255f)): Paint {
        val paint = Paint()

        paint.typeface = font
        paint.textSize = size.toFloat()
        paint.color = color.toArgb()

        return paint
    }

    fun putTextOnCanvas(canvas: Canvas, text: String, x: Int, y: Int, paint: Paint) {
        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)
    }

    fun scaleBitmap(bm: Bitmap, scaleX: Float, scaleY: Float): Bitmap {
        return Bitmap.createScaledBitmap(
            bm,
            (bm.width * scaleX).toInt(),
            (bm.height * scaleY).toInt(),
            true
        )
    }

    fun intToStr(num: Int, mode: Boolean = true): String {
        val res: String
        val sign = if (num >= 0) "" else "-"

        res = if (mode) {
            when(abs(num) < 10){
                true -> sign + "0$num"
                false -> sign + num
            }
        } else {
            sign + num
        }

        return res
    }

    fun defineTouchZone(xDP: Int, yDP: Int, screenCenter: Pair<Int, Int>): com.si7agent.digital_neon.service.Zones {
        val appLauncherLeft = screenCenter.first-pxToDp(c.resources.getDimension(R.dimen.app_launcher_icon_width))/2
        val appLauncherRight = appLauncherLeft+pxToDp(c.resources.getDimension(R.dimen.app_launcher_icon_width))
        val appLauncherTop = pxToDp(c.resources.getDimension(R.dimen.app_launcher_icon_marginTop))
        val appLauncherBottom = appLauncherTop+pxToDp(c.resources.getDimension(R.dimen.app_launcher_icon_height))

        val timeLeft = screenCenter.first-pxToDp(c.resources.getDimension(R.dimen.time_layout_width))/2
        val timeRight = timeLeft+pxToDp(c.resources.getDimension(R.dimen.time_layout_width))
        val timeTop = screenCenter.second-pxToDp(c.resources.getDimension(R.dimen.time_layout_height))/2
        val timeBottom = timeTop+pxToDp(c.resources.getDimension(R.dimen.time_layout_height))

        val stepLeft = timeLeft+pxToDp(c.resources.getDimension(R.dimen.step_zone_marginStart))
        val stepRight = stepLeft+pxToDp(c.resources.getDimension(R.dimen.neon_bg_icon_width))
        val stepTop = timeBottom
        val stepBottom = stepTop+pxToDp(c.resources.getDimension(R.dimen.neon_bg_icon_height))

        val hrmLeft = timeRight-pxToDp(c.resources.getDimension(R.dimen.hrm_zone_marginEnd))-pxToDp(c.resources.getDimension(R.dimen.neon_bg_icon_width))
        val hrmRight = hrmLeft+pxToDp(c.resources.getDimension(R.dimen.neon_bg_icon_width))
        val hrmTop = timeBottom
        val hrmBottom = hrmTop+pxToDp(c.resources.getDimension(R.dimen.neon_bg_icon_height))

        val dayLeft = timeRight
        val dayRight = dayLeft+pxToDp(c.resources.getDimension(R.dimen.neon_bg_icon_width))
        val dayTop = screenCenter.second-pxToDp(c.resources.getDimension(R.dimen.neon_bg_icon_height))/2
        val dayBottom = dayTop+pxToDp(c.resources.getDimension(R.dimen.neon_bg_icon_height))

        val dateLeft = screenCenter.first-pxToDp(c.resources.getDimension((R.dimen.date_layout_width)))/2
        val dateRight = dateLeft+pxToDp(c.resources.getDimension((R.dimen.date_layout_width)))
        val dateTop = appLauncherBottom-pxToDp(c.resources.getDimension(R.dimen.date_layout_marginBottom))-pxToDp(c.resources.getDimension(R.dimen.date_layout_height))
        val dateBottom = dateTop+pxToDp(c.resources.getDimension(R.dimen.date_layout_height))

        if (xDP in appLauncherLeft..appLauncherRight)
            if (yDP in appLauncherTop..appLauncherBottom)
                return Zones.APPLAUNCHER_TOUCH_ZONE

        if (xDP in dateLeft..dateRight)
            if (yDP in dateTop..dateBottom)
                return Zones.DATE_TOUCH_ZONE

        if (xDP in dayLeft..dayRight)
            if (yDP in dayTop..dayBottom)
                return Zones.DAY_TOUCH_ZONE

        if (xDP in timeLeft..timeRight)
            if (yDP in timeTop..timeBottom)
                return Zones.TIME_TOUCH_ZONE

        if (xDP in stepLeft..stepRight)
            if (yDP in stepTop..stepBottom)
                return Zones.STEP_TOUCH_ZONE

        if (xDP in hrmLeft..hrmRight)
            if (yDP in hrmTop..hrmBottom)
                return Zones.HRM_TOUCH_ZONE

        return Zones.BG_TOUCH_ZONE
    }

    fun pxToDp(px: Float): Int {
        val density = c.resources.displayMetrics.density
        return (px/density).toInt();
    }

    fun dpToPx(dp: Float): Int {
        val density = c.resources.displayMetrics.density
        return (dp*density).toInt();
    }
}