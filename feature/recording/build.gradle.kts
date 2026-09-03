plugins {
    alias(libs.plugins.offhand.kmp.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.recording"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ai-api"))
            implementation(project(":core:audio"))
            implementation(project(":core:data"))
            implementation(project(":core:security"))
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.timber)
        }
    }
}
