plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.hilt)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.device"
}

dependencies {
    implementation(libs.timber)

    testImplementation(libs.junit)
}
