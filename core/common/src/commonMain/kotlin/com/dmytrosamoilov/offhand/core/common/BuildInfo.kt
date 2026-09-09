package com.dmytrosamoilov.offhand.core.common

data class BuildInfo(
    val isDebugBuild: Boolean,
    val appVersion: String = "",
    val platform: String = "",
)
