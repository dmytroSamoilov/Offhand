plugins {
    alias(libs.plugins.offhand.android.library)
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.common"
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
