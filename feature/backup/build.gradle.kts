plugins {
    alias(libs.plugins.offhand.kmp.feature)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.dmytrosamoilov.offhand.feature.backup"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:data"))
            implementation(project(":core:security"))
            implementation(libs.okio)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
