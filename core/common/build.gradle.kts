plugins {
    alias(libs.plugins.offhand.kmp.library)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.common"
}

kotlin {
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
        getByName("androidUnitTest").dependencies {
            implementation(libs.junit)
            implementation(libs.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
