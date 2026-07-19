import com.android.build.api.dsl.ApplicationExtension
import com.dmytrosamoilov.offhand.buildlogic.configureAndroidCompose
import com.dmytrosamoilov.offhand.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureAndroidCompose(this)
                defaultConfig.targetSdk = 36
                buildFeatures.buildConfig = true
            }
        }
    }
}
