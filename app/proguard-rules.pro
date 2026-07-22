# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers so Crashlytics and Play Console can show exact crash locations.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep class net.zetetic.database.** { *; }
-keep class com.google.ai.edge.litertlm.** { *; }
# sherpa-onnx JNI resolves config fields by name at runtime (GetFieldID).
-keep class com.k2fsa.sherpa.onnx.** { *; }
# Annotation referenced by play-review-ktx but not shipped in any GMS runtime artifact
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
