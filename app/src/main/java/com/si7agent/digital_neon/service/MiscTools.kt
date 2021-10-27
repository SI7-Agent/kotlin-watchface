package com.si7agent.digital_neon.service

import android.content.res.Resources
import android.graphics.*

class MiscTools {
    fun putImageOnCanvas(canvas: Canvas, image: Bitmap, left: Int, top: Int) {
        val paint = Paint()
        paint.color = Color.BLACK
        canvas.drawBitmap(image, left.toFloat(), top.toFloat(), paint)
    }

    fun getIndexesFromTypedArray(resources: Resources, id: Int): Array<Int> {
        val arr = resources.obtainTypedArray(id)
        val arrSize = arr.length()

        val idxs: MutableList<Int> = ArrayList()
        for (i in 0 until arrSize) {
            idxs.add(arr.getResourceId(i, -1))
        }

        arr.recycle()
        return idxs.toTypedArray()
    }

    fun createBitmapFromId(resources: Resources, id: Int): Bitmap {
        return BitmapFactory.decodeResource(
            resources,
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
}