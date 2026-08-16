plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.koin)
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
}
