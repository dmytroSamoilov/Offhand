package com.dmytrosamoilov.offhand.feature.recording.domain.review

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInstallInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val installedAtMs: Long
        get() = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .firstInstallTime
}
