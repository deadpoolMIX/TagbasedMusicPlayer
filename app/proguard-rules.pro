# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entities
-keep class com.tagplayer.musicplayer.data.local.entity.** { *; }

# Keep Hilt components
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
