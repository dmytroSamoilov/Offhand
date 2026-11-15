plugins {
    alias(libs.plugins.offhand.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.dmytrosamoilov.offhand.core.ai.api"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
            api(project(":core:common"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
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
            implementation(libs.mockk)
        }
    }
}
