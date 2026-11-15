import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.offhand.android.library)
    alias(libs.plugins.offhand.koin)
    alias(libs.plugins.kotlin.serialization)
}

val sherpaOnnxSha256 = "a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7"
val sherpaOnnxVersion: String = libs.versions.sherpaOnnx.get()
val sherpaOnnxAar: File = rootProject.file("libs/sherpa-onnx-$sherpaOnnxVersion.aar")

fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(readBytes())
    .joinToString("") { byte -> "%02x".format(byte) }

val downloadSherpaOnnx = tasks.register("downloadSherpaOnnx") {
    outputs.file(sherpaOnnxAar)
    outputs.upToDateWhen { sherpaOnnxAar.exists() && sherpaOnnxAar.sha256() == sherpaOnnxSha256 }
    doLast {
        if (sherpaOnnxAar.exists() && sherpaOnnxAar.sha256() == sherpaOnnxSha256) return@doLast
        sherpaOnnxAar.parentFile.mkdirs()
        val url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/" +
            "v$sherpaOnnxVersion/sherpa-onnx-$sherpaOnnxVersion.aar"
        logger.lifecycle("Downloading sherpa-onnx AAR from $url")
        URI(url).toURL().openStream().use { input ->
            sherpaOnnxAar.outputStream().use { output -> input.copyTo(output) }
        }
        val actual = sherpaOnnxAar.sha256()
        if (actual != sherpaOnnxSha256) {
            sherpaOnnxAar.delete()
            error("sherpa-onnx AAR checksum mismatch: expected $sherpaOnnxSha256, got $actual")
        }
    }
}

android {
    namespace = "com.dmytrosamoilov.offhand.core.ai.local"

    buildFeatures {
        buildConfig = true
    }
}

tasks.named("preBuild") {
    dependsOn(downloadSherpaOnnx)
}

dependencies {
    api(project(":core:ai-api"))
    implementation(project(":core:device"))

    compileOnly(files(sherpaOnnxAar))
    implementation(libs.litertlm)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
