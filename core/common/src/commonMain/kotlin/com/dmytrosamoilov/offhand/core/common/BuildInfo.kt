package com.dmytrosamoilov.offhand.core.common

data class BuildInfo(
    val isDeveloperBuild: Boolean,
    val appVersion: String = "",
    val platform: String = "",
)
