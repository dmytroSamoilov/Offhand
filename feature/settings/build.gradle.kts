plugins {
    alias(libs.plugins.offhand.android.feature)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.settings"
}

dependencies {
    implementation(project(":core:ai-api"))
    implementation(project(":core:data"))
}
