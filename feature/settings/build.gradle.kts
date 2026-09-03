plugins {
    alias(libs.plugins.offhand.kmp.feature)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.settings"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ai-api"))
            implementation(project(":core:data"))
            implementation(project(":core:security"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.material.icons.extended)
        }
    }
}
