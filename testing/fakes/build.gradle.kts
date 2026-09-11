plugins {
    alias(libs.plugins.offhand.kmp.library)
}

android {
    namespace = "com.dmytrosamoilov.offhand.testing.fakes"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ai-api"))
            implementation(project(":core:audio"))
            implementation(project(":core:data"))
            implementation(project(":core:device"))
            implementation(project(":feature:recording"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.coroutines.core)
        }
    }
}
