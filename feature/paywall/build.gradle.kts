plugins {
    alias(libs.plugins.offhand.kmp.feature)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.paywall"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:data"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.material.icons.extended)
        }
    }
}
