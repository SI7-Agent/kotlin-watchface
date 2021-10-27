package com.si7agent.digital_neon.service

import android.graphics.Color
import com.si7agent.digital_neon.R
import com.si7agent.digital_neon.model.DigitalNeonWatchFaceStyle
import com.si7agent.digital_neon.model.WatchFaceBackgroundImage
import com.si7agent.digital_neon.model.WatchFaceDimensions
import com.si7agent.digital_neon.model.WatchFaceTheme
import com.si7agent.digital_neon.model.WatchFaceFont

@DslMarker
annotation class WatchFaceStyleDSL

@WatchFaceStyleDSL
class WatchFaceFontBuilder(font: Int) {

    private val fontPicked: Int = font

    private val availableFonts: Array<MutableMap<String, Any?>> = arrayOf(
        mutableMapOf(
            "hourSize" to 90,
            "minuteSize" to 55,
            "secondSize" to 30,
            "labelSize" to 16,
            "font" to R.font.main_font_minaeff,
        ),
        mutableMapOf(
            "hourSize" to 77,
            "minuteSize" to 55,
            "secondSize" to 30,
            "labelSize" to 13,
            "font" to R.font.main_font_3am,
        ),
        mutableMapOf(
            "hourSize" to 100,
            "minuteSize" to 60,
            "secondSize" to 35,
            "labelSize" to 20,
            "font" to R.font.main_font_basis33,
        ),
        mutableMapOf(
            "hourSize" to 90,
            "minuteSize" to 52,
            "secondSize" to 30,
            "labelSize" to 18,
            "font" to R.font.main_font_aurach,
        )
    )

    var hourSize: Int by availableFonts[fontPicked - 1]
    var minuteSize: Int by availableFonts[fontPicked - 1]
    var secondSize: Int by availableFonts[fontPicked - 1]
    var labelSize: Int by availableFonts[fontPicked - 1]
    var font: Int by availableFonts[fontPicked - 1]

    fun build(): WatchFaceFont {
        return WatchFaceFont(
            hourSize,
            minuteSize,
            secondSize,
            labelSize,
            font
        )
    }
}

@WatchFaceStyleDSL
class WatchFaceThemeBuilder(theme: Int) {

    private val themePicked: Int = theme

    private val availableThemes: Array<MutableMap<String, Any?>> = arrayOf(
        mutableMapOf(
            "labelColor" to R.color.theme1,
            "themeNamePic" to "theme1"
        ),
        mutableMapOf(
            "labelColor" to R.color.theme2,
            "themeNamePic" to "theme2"
        ),
        mutableMapOf(
            "labelColor" to R.color.theme3,
            "themeNamePic" to "theme3"
        ),
        mutableMapOf(
            "labelColor" to R.color.theme4,
            "themeNamePic" to "theme4"
        )
    )

    var labelColor: Int by availableThemes[themePicked - 1]
    var themeNamePic: String by availableThemes[themePicked - 1]

    fun build(): WatchFaceTheme {
        return WatchFaceTheme(
            labelColor,
            themeNamePic
        )
    }
}

@WatchFaceStyleDSL
class WatchFaceDimensionsBuilder {

    private val attributesMap: MutableMap<String, Any?> = mutableMapOf(
        "hourHandRadiusRatio" to 0.5f,
        "minuteHandRadiusRatio" to 0.75f,
        "secondHandRadiusRatio" to 0.875f,
        "hourHandWidth" to 5f,
        "minuteHandWidth" to 3f,
        "secondHandWidth" to 2f,
        "shadowRadius" to 2f,
        "innerCircleRadius" to 4f,
        "innerCircleToArmsDistance" to 5f
    )

    var hourHandRadiusRatio:Float by attributesMap
    var minuteHandRadiusRatio:Float by attributesMap
    var secondHandRadiusRatio:Float by attributesMap
    var hourHandWidth:Float by attributesMap
    var minuteHandWidth:Float by attributesMap
    var secondHandWidth:Float by attributesMap
    var shadowRadius:Float by attributesMap
    var innerCircleRadius:Float by attributesMap
    var innerCircleToArmsDistance:Float by attributesMap

    fun build(): WatchFaceDimensions {
        return WatchFaceDimensions(
            hourHandRadiusRatio,
            minuteHandRadiusRatio,
            secondHandRadiusRatio,
            hourHandWidth,
            minuteHandWidth,
            secondHandWidth,
            shadowRadius,
            innerCircleRadius,
            innerCircleToArmsDistance
        )
    }
}

@WatchFaceStyleDSL
class WatchFaceBackgroundImageBuilder {

    private val attributesMap: MutableMap<String, Any?> = mutableMapOf(
        "backgroundImageResource" to WatchFaceBackgroundImage.EMPTY_IMAGE_RESOURCE
    )

    var backgroundImageResource:Int by attributesMap

    fun build(): WatchFaceBackgroundImage {
        return WatchFaceBackgroundImage(backgroundImageResource)
    }
}

@WatchFaceStyleDSL
class DigitalNeonWatchFaceStyleBuilder(theme: Int, font: Int) {

    private val themePicked = theme
    private val fontPicked = font

    private var watchFaceFont: WatchFaceFont? = null
    private var watchFaceTheme: WatchFaceTheme? = null
    private var watchFaceDimensions: WatchFaceDimensions? = null
    private var watchFaceBackgroundImage: WatchFaceBackgroundImage =
        WatchFaceBackgroundImageBuilder().build()

    fun watchFaceFont(setup: WatchFaceFontBuilder.() -> Unit) {
        val watchFaceFontBuilder = WatchFaceFontBuilder(fontPicked)
        watchFaceFontBuilder.setup()
        watchFaceFont = watchFaceFontBuilder.build()
    }

    fun watchFaceTheme(setup: WatchFaceThemeBuilder.() -> Unit) {
        val watchFaceThemeBuilder = WatchFaceThemeBuilder(themePicked)
        watchFaceThemeBuilder.setup()
        watchFaceTheme = watchFaceThemeBuilder.build()
    }

    fun watchFaceDimensions(setup: WatchFaceDimensionsBuilder.() -> Unit) {
        val analogWatchFaceDimensionsBuilder = WatchFaceDimensionsBuilder()
        analogWatchFaceDimensionsBuilder.setup()
        watchFaceDimensions = analogWatchFaceDimensionsBuilder.build()
    }

    fun watchFaceBackgroundImage(setup: WatchFaceBackgroundImageBuilder.() -> Unit) {
        val analogWatchFaceBackgroundImageBuilder = WatchFaceBackgroundImageBuilder()
        analogWatchFaceBackgroundImageBuilder.setup()
        watchFaceBackgroundImage = analogWatchFaceBackgroundImageBuilder.build()
    }


    fun build(): DigitalNeonWatchFaceStyle {
        return DigitalNeonWatchFaceStyle(
            watchFaceFont ?: throw InstantiationException("Must define watch face font in DSL."),
            watchFaceTheme ?: throw InstantiationException("Must define watch face theme in DSL."),
            watchFaceDimensions ?: throw InstantiationException("Must define watch face dimensions in DSL."),
            watchFaceBackgroundImage
        )
    }

    @Suppress("UNUSED_PARAMETER")
    @Deprecated(level = DeprecationLevel.ERROR, message = "WatchFaceStyles can't be nested.")
    fun digitalNeonWatchFaceStyle(theme: Int = 1, font: Int = 1, param: () -> Unit = {}) {
    }
}

@WatchFaceStyleDSL
fun digitalNeonWatchFaceStyle (theme: Int, font: Int, setup: DigitalNeonWatchFaceStyleBuilder.() -> Unit):
        DigitalNeonWatchFaceStyle {
    val digitalNeonWatchFaceStyleBuilder = DigitalNeonWatchFaceStyleBuilder(theme, font)
    digitalNeonWatchFaceStyleBuilder.setup()
    return digitalNeonWatchFaceStyleBuilder.build()
}
