package com.si7agent.digital_neon.model

data class DigitalNeonWatchFaceStyle(
    val font: String,
    val second: Int,
    val theme: Int
)

data class WatchFaceBackgroundImage(val backgroundImageResource: Int) {
    companion object {
        const val EMPTY_IMAGE_RESOURCE = 0
    }
}
