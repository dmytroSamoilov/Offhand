import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.skie)
}

kotlin {
    val xcFramework = XCFramework("OffhandShared")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "OffhandShared"
            isStatic = true
            binaryOption("bundleId", "com.dmytrosamoilov.offhand.shared")
            export(project(":core:common"))
            export(project(":core:ai-api"))
            export(project(":core:device"))
            xcFramework.add(this)
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
            api(project(":core:ai-api"))
            api(project(":core:device"))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
    }
}

skie {
    analytics {
        enabled.set(false)
    }
}
