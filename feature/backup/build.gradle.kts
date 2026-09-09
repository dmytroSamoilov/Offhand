plugins {
    alias(libs.plugins.offhand.kmp.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dmytrosamoilov.offhand.feature.backup"
}

kotlin {
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
            implementation(libs.androidx.material.icons.extended)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
