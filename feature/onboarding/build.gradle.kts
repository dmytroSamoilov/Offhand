plugins {
    alias(libs.plugins.offhand.kmp.feature)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.onboarding"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ai-api"))
            implementation(project(":core:data"))
            implementation(project(":core:device"))
            implementation(project(":core:security"))
        }
        androidMain.dependencies {
            implementation(project(":core:ai-api"))
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.timber)
        }
    }
}
