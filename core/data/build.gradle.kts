plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.koin)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:security"))

    api(libs.coroutines.android)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.datastore.preferences)
    implementation(libs.timber)
}
