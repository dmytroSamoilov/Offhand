plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.android.library.compose)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.designsystem"
}

dependencies {
    api(libs.androidx.graphics.shapes)
    implementation(libs.androidx.material.icons.extended)
}
