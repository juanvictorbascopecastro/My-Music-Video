# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep models for Firestore serialization
-keep class com.youtube.musica.models.** { *; }

# Firebase Crashlytics
-keepattributes *Annotation*
-keep class com.google.firebase.crashlytics.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# AndroidX Media
-keep class android.support.v4.media.** { *; }

# Android YouTube Player
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }

# Google Cast
-keep class * implements com.google.android.gms.cast.framework.OptionsProvider
-keep class com.youtube.musica.CastOptionsProvider { *; }