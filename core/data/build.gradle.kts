plugins {
    alias(libs.plugins.offhand.kmp.library)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.data"
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:security"))
            api(libs.coroutines.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.room.runtime)
            implementation(libs.datastore.preferences.core)
        }
        androidMain.dependencies {
            api(libs.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.sqlcipher.android)
            implementation(libs.okio)
            implementation(libs.timber)
        }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
