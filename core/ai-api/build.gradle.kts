plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.ai.api"
}

dependencies {
    api(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
}
