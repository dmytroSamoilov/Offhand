package com.dmytrosamoilov.offhand.feature.onboarding.service

import android.content.Context
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ModelDownloadController {

    override fun start() {
        ModelDownloadService.start(context)
    }
}
