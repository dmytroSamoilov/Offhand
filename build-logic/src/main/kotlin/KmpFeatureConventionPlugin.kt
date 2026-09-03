import com.android.build.api.dsl.LibraryExtension
import com.dmytrosamoilov.offhand.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("offhand.kmp.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            extensions.configure<ComposeCompilerGradlePluginExtension> {
                targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":core:common"))
                    implementation(project.dependencies.platform(libs.findLibrary("koin-bom").get()))
                    implementation(libs.findLibrary("koin-core").get())
                    implementation(libs.findLibrary("koin-core-viewmodel").get())
                    implementation(libs.findLibrary("coroutines-core").get())
                    implementation(libs.findLibrary("kermit").get())
                }
                sourceSets.getByName("androidMain").dependencies {
                    implementation(project(":core:designsystem"))
                    implementation(project(":core:ui"))
                    implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                    implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                    implementation(libs.findLibrary("koin-androidx-compose").get())
                    implementation(libs.findLibrary("coroutines-android").get())
                    val composeBom = libs.findLibrary("androidx-compose-bom").get()
                    implementation(project.dependencies.platform(composeBom))
                    implementation(libs.findLibrary("androidx-ui").get())
                    implementation(libs.findLibrary("androidx-ui-graphics").get())
                    implementation(libs.findLibrary("androidx-material3").get())
                    implementation(libs.findLibrary("androidx-ui-tooling-preview").get())
                }
                sourceSets.getByName("androidUnitTest").dependencies {
                    implementation(libs.findLibrary("junit").get())
                    implementation(libs.findLibrary("mockk").get())
                    implementation(libs.findLibrary("turbine").get())
                    implementation(libs.findLibrary("coroutines-test").get())
                }
            }

            dependencies {
                add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())
            }
        }
    }
}
