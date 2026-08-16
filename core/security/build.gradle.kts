plugins {
    alias(libs.plugins.offhand.kmp.library)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.security"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            api(libs.coroutines.android)
            implementation(libs.tink.android)
            implementation(libs.timber)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
