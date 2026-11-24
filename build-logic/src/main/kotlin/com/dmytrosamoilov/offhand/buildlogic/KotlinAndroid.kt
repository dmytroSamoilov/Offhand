package com.dmytrosamoilov.offhand.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal const val COMPILE_SDK = 37
internal const val MIN_SDK = 31
internal const val TARGET_SDK = 36
internal val JVM_TARGET = JvmTarget.JVM_11

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.compileSdk = COMPILE_SDK
    commonExtension.defaultConfig.minSdk = MIN_SDK
    commonExtension.compileOptions.sourceCompatibility = JavaVersion.VERSION_11
    commonExtension.compileOptions.targetCompatibility = JavaVersion.VERSION_11

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions.jvmTarget.set(JVM_TARGET)
    }
}

internal fun KotlinMultiplatformAndroidLibraryTarget.configureKotlinMultiplatformAndroid() {
    compileSdk = COMPILE_SDK
    minSdk = MIN_SDK
    compilerOptions.jvmTarget.set(JVM_TARGET)
    localDependencySelection.selectBuildTypeFrom.set(listOf("debug", "release"))
    withHostTest { }
}
