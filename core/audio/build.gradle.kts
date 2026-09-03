plugins {
    alias(libs.plugins.offhand.kmp.library)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.audio"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
        }
        androidMain.dependencies {
            api(libs.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.timber)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
