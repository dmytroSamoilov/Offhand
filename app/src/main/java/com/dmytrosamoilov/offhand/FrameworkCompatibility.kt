package com.dmytrosamoilov.offhand

import android.content.res.Configuration
import android.os.Build

// A modified system image can report an SDK level it does not implement: the
// 1.4.1 crash 5c3f7672 came from an Android 11 framework claiming 31 or newer,
// where Compose reads Configuration.fontWeightAdjustment and dies with a
// NoSuchFieldError before the first screen is drawn. Checked before setContent.
object FrameworkCompatibility {

    fun isComposeSupported(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasFontWeightAdjustment()

    private fun hasFontWeightAdjustment(): Boolean =
        runCatching { Configuration::class.java.getField("fontWeightAdjustment") }.isSuccess
}
