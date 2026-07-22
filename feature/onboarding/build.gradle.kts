plugins {
    alias(libs.plugins.offhand.android.feature)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.onboarding"
}

dependencies {
    implementation(project(":core:device"))
    implementation(project(":core:ai-api"))
    implementation(project(":core:data"))
    implementation(project(":core:security"))
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.timber)
}
