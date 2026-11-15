import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.dmytrosamoilov.offhand.buildlogic.configureKotlinMultiplatformAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("com.android.lint")

            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    configureKotlinMultiplatformAndroid()
                }
                iosArm64()
                iosSimulatorArm64()
            }
        }
    }
}
