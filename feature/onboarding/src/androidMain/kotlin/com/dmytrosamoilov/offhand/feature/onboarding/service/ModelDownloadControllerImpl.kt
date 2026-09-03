package com.dmytrosamoilov.offhand.feature.onboarding.service

import android.content.Context
import com.dmytrosamoilov.offhand.core.common.ModelDownloadController

class ModelDownloadControllerImpl(
    private val context: Context,
) : ModelDownloadController {

    override fun start() {
        ModelDownloadService.start(context)
    }
}
