package com.si7agent.digital_neon.model

data class DigitalNeonWatchFaceStyle(
    val font: WatchFaceFont,
    val theme: WatchFaceTheme,
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
    val font: Int
)

data class WatchFaceBackgroundImage(val backgroundImageResource: Int) {
    companion object {
        const val EMPTY_IMAGE_RESOURCE = 0
    }
}
