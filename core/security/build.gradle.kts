plugins {
    alias(libs.plugins.offhand.kmp.library)
}

kotlin {
    android {
        namespace = "com.dmytrosamoilov.offhand.core.security"
    }

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
        getByName("androidHostTest").dependencies {
            implementation(libs.mockk)
        }
    }
}
