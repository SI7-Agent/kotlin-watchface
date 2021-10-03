package com.si7agent.digital_neon

import android.graphics.Color
import com.si7agent.digital_neon.model.DigitalNeonWatchFaceStyle
import com.si7agent.digital_neon.service.AbstractKotlinWatchFace
//import com.si7agent.digital_neon.service.


class DigitalNeonDSL : AbstractKotlinWatchFace() {
    override fun getWatchFaceStyle(): DigitalNeonWatchFaceStyle {
//
//        /**
//         * Initializes colors and dimensions of watch face. Review [AnalogWatchFaceStyle] for
//         * detailed explanation of each field.
//         */
//        return analogWatchFaceStyle {
//            watchFaceColors {
//                main = Color.CYAN
//                highlight = Color.parseColor("#ffa500")
//                background = Color.WHITE
//            }
//            watchFaceDimensions {
//                hourHandRadiusRatio = 0.2f
//                minuteHandRadiusRatio = 0.5f
//                secondHandRadiusRatio = 0.9f
//            }
//            watchFaceBackgroundImage {
//                backgroundImageResource = R.drawable.background_image
//            }
//        }
        return DigitalNeonWatchFaceStyle("test_font", 10, 1)
    }
}