plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.koin)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.audio"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)
    api(libs.coroutines.android)
}
