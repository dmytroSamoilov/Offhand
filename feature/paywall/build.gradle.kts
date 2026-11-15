plugins {
    alias(libs.plugins.offhand.kmp.feature)
}

kotlin {
    android {
        namespace = "com.dmytrosamoilov.offhand.feature.paywall"
    }

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
