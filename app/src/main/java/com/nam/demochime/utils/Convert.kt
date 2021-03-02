package com.nam.demochime.utils

import android.content.Context
import android.util.DisplayMetrics

class Convert {

    companion object {
        fun convertDpToPixel(dp: Float, context: Context): Float {
            return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }
}