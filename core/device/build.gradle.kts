plugins {
    alias(libs.plugins.offhand.kmp.library)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.device"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.timber)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
