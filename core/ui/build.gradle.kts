plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.android.library.compose)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.ui"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.lifecycle.runtime.compose)
}
