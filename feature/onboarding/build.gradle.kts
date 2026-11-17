plugins {
    alias(libs.plugins.offhand.kmp.feature)
}

kotlin {
    android {
        namespace = "com.dmytrosamoilov.offhand.feature.onboarding"
    }

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
            implementation(libs.timber)
        }
    }
}
