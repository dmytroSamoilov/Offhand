plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.hilt)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:security"))

    api(libs.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.datastore.preferences)
    implementation(libs.timber)

    testImplementation(libs.junit)
}
