plugins {
    alias(libs.plugins.offhand.android.feature)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.notes"
}

dependencies {
    implementation(project(":core:ai-api"))
    implementation(project(":core:audio"))
    implementation(project(":core:data"))
    implementation(project(":core:security"))
    implementation(project(":feature:recording"))

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.layout)
    implementation(libs.androidx.material3.adaptive.navigation)
}
