package com.dmytrosamoilov.offhand.feature.notes.domain.review

import android.content.Context

class AppInstallInfoProvider(
    private val context: Context,
) {

    val installedAtMs: Long
        get() = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .firstInstallTime
}
