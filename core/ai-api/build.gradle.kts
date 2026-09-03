plugins {
    alias(libs.plugins.offhand.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.ai.api"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            api(libs.coroutines.android)
        }
        getByName("androidUnitTest").dependencies {
            implementation(libs.junit)
            implementation(libs.coroutines.test)
            implementation(libs.mockk)
        }
    }
}
