plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.koin)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.security"
}

dependencies {
    implementation(libs.coroutines.android)
    implementation(libs.tink.android)
    implementation(libs.timber)
}
