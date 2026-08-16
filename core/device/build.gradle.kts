plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.koin)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.device"
}

dependencies {
    implementation(libs.timber)

    testImplementation(libs.junit)
}
