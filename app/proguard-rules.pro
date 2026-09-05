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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- TMDB network models (Gson reflects over these; don't let R8 rename/strip fields) ---
-keep class com.ahsan.movieapp.data.remote.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# --- Room ---
-keep class com.ahsan.movieapp.data.local.entity.** { *; }

# --- Retrofit / OkHttp / Gson (standard rules for the versions this project uses) ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}