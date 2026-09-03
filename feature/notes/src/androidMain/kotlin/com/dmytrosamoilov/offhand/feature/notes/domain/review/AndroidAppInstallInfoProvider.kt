package com.dmytrosamoilov.offhand.feature.notes.domain.review

import android.content.Context

class AndroidAppInstallInfoProvider(
    private val context: Context,
) : AppInstallInfoProvider {

    override val installedAtMs: Long
        get() = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .firstInstallTime
}
