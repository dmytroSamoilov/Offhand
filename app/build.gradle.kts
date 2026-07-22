import com.android.build.api.artifact.SingleArtifact
import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
    alias(libs.plugins.offhand.android.application)
    alias(libs.plugins.offhand.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val sherpaOnnxAar: File =
    rootProject.file("libs/sherpa-onnx-${libs.versions.sherpaOnnx.get()}.aar")

android {
    namespace = "com.dmytrosamoilov.offhand"

    defaultConfig {
        applicationId = "com.dmytrosamoilov.offhand"
        versionCode = 8
        versionName = "0.9.2-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "environment"

    productFlavors {
        create("production") {
            dimension = "environment"
        }
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

googleServices {
    missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val variantName = variant.name.replaceFirstChar(Char::uppercase)
        val versionName = variant.outputs.single().versionName
        val exportMapping = tasks.register<Copy>("export${variantName}Mapping") {
            from(variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE))
            into(layout.projectDirectory.dir("${variant.flavorName}/${variant.buildType}-mapping"))
            rename { "mapping-${versionName.get()}.txt" }
        }
        listOf("assemble$variantName", "bundle$variantName").forEach { taskName ->
            tasks.matching { it.name == taskName }.configureEach { finalizedBy(exportMapping) }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(":core:ai-local:downloadSherpaOnnx")
}

dependencies {
    implementation(files(sherpaOnnxAar))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:ai-api"))
    implementation(project(":core:ai-local"))
    implementation(project(":core:security"))
    implementation(project(":core:data"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:recording"))
    implementation(project(":feature:notes"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    debugImplementation(libs.leakcanary.android)
}
