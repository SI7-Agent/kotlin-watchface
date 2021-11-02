package com.si7agent.digital_neon.service

import com.si7agent.digital_neon.R
import com.si7agent.digital_neon.model.DigitalNeonWatchFaceStyle
import com.si7agent.digital_neon.model.WatchFaceBackgroundImage
import com.si7agent.digital_neon.model.WatchFaceTheme
import com.si7agent.digital_neon.model.WatchFaceFont

@DslMarker
annotation class WatchFaceStyleDSL

@WatchFaceStyleDSL
class WatchFaceFontBuilder(font: Int) {

    private val fontPicked: Int = font

    private val availableFonts: Array<MutableMap<String, Any?>> = arrayOf(
        mutableMapOf(
            "hourSize" to 60,
            "minuteSize" to 35,
            "secondSize" to 20,
            "labelSize" to 16,
            "font" to R.font.main_font_minaeff,
        ),
        mutableMapOf(
            "hourSize" to 43,
            "minuteSize" to 30,
            "secondSize" to 15,
            "labelSize" to 12,
            "font" to R.font.main_font_3am,
        ),
        mutableMapOf(
            "hourSize" to 65,
            "minuteSize" to 45,
            "secondSize" to 22,
            "labelSize" to 20,
            "font" to R.font.main_font_basis33,
        ),
        mutableMapOf(
            "hourSize" to 60,
            "minuteSize" to 35,
            "secondSize" to 18,
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

    fun watchFaceBackgroundImage(setup: WatchFaceBackgroundImageBuilder.() -> Unit) {
        val analogWatchFaceBackgroundImageBuilder = WatchFaceBackgroundImageBuilder()
        analogWatchFaceBackgroundImageBuilder.setup()
        watchFaceBackgroundImage = analogWatchFaceBackgroundImageBuilder.build()
    }


    fun build(): DigitalNeonWatchFaceStyle {
        return DigitalNeonWatchFaceStyle(
            watchFaceFont ?: throw InstantiationException("Must define watch face font in DSL."),
            watchFaceTheme ?: throw InstantiationException("Must define watch face theme in DSL."),
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
