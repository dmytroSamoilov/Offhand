plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.hilt)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.security"
}

dependencies {
    implementation(libs.coroutines.android)
    implementation(libs.tink.android)
    implementation(libs.timber)
}
