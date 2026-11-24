plugins {
    alias(libs.plugins.offhand.kmp.library)
}

kotlin {
    android {
        namespace = "com.dmytrosamoilov.offhand.core.common"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.coroutines.core)
        }
        androidMain.dependencies {
            api(libs.coroutines.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
