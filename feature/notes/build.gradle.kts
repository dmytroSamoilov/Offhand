plugins {
    alias(libs.plugins.offhand.kmp.feature)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.notes"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ai-api"))
            implementation(project(":core:data"))
            implementation(project(":feature:recording"))
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(project(":core:audio"))
            implementation(project(":core:security"))
            implementation(libs.androidx.core.ktx)
            implementation(libs.play.review.ktx)
            implementation(libs.timber)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.androidx.material3.adaptive)
            implementation(libs.androidx.material3.adaptive.layout)
            implementation(libs.androidx.material3.adaptive.navigation)
        }
    }
}
