plugins {
    alias(libs.plugins.offhand.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.recording"
}

dependencies {
    implementation(project(":core:audio"))
    implementation(project(":core:security"))
    implementation(project(":core:ai-api"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
