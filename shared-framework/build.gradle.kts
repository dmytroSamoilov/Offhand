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
            export(project(":core:audio"))
            export(project(":core:data"))
            export(project(":core:device"))
            export(project(":core:security"))
            export(project(":feature:notes"))
            export(project(":feature:onboarding"))
            export(project(":feature:recording"))
            export(project(":feature:settings"))
            xcFramework.add(this)
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
            api(project(":core:ai-api"))
            api(project(":core:audio"))
            api(project(":core:data"))
            api(project(":core:device"))
            api(project(":core:security"))
            api(project(":feature:notes"))
            api(project(":feature:onboarding"))
            api(project(":feature:recording"))
            api(project(":feature:settings"))
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
