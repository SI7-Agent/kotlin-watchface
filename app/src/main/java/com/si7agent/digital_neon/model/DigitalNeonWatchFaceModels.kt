package com.si7agent.digital_neon.model

data class DigitalNeonWatchFaceStyle(
    val font: WatchFaceFont,
    val theme: WatchFaceTheme,
    val dimensions: WatchFaceDimensions,
    val backgroundImage: WatchFaceBackgroundImage
)

data class WatchFaceTheme(
    val labelColor: Int,
    val themeNamePic: String
)

data class WatchFaceFont(
    val hourSize: Int,
    val minuteSize: Int,
    val secondSize: Int,
    val labelSize: Int,
    val fontName: String
)

data class WatchFaceDimensions(
    val hourHandRadiusRatio:Float,
    val minuteHandRadiusRatio:Float,
    val secondHandRadiusRatio:Float,
    val hourHandWidth:Float,
    val minuteHandWidth:Float,
    val secondHandWidth:Float,
    val shadowRadius:Float,
    val innerCircleRadius:Float,
    val innerCircleToArmsDistance:Float
)

data class WatchFaceBackgroundImage(val backgroundImageResource: Int) {
    companion object {
        const val EMPTY_IMAGE_RESOURCE = 0
    }
}
