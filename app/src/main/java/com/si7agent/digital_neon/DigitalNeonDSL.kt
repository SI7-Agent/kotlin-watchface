package com.si7agent.digital_neon

import com.si7agent.digital_neon.model.DigitalNeonWatchFaceStyle
import com.si7agent.digital_neon.service.AbstractKotlinWatchFace
import com.si7agent.digital_neon.service.digitalNeonWatchFaceStyle

class DigitalNeonDSL : AbstractKotlinWatchFace() {

    override fun getWatchFaceStyle(): DigitalNeonWatchFaceStyle {

        return digitalNeonWatchFaceStyle(currentTheme, currentFont) {
            watchFaceFont {

            }
            watchFaceTheme {

            }
            watchFaceDimensions {

            }
            watchFaceBackgroundImage {
                backgroundImageResource = R.drawable.watchface_service_bg
            }
        }

    }
}