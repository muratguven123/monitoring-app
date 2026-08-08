# =============================================================================
# Monitoring Dashboard — Production ProGuard Rules
# =============================================================================

# ----- General Kotlin -----
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin Metadata (required for reflection-based libraries)
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    <fields>;
    <init>(...);
    <methods>;
}

# Kotlin sealed classes & data classes used in when expressions
-keepclassmembers class * {
    ** INSTANCE;
}

# ----- Retrofit -----
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ----- OkHttp / Okio -----
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okio.** { *; }

# ----- Gson -----
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# ----- DTO Models (API Response) -----
# Keep all DTO models used by Gson + Retrofit — obfuscation would break deserialization
-keep class com.monitoring.dashboard.data.remote.dto.** { *; }

# ----- Room Database -----
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# ----- Hilt / Dagger -----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-dontwarn dagger.hilt.**
-dontwarn dagger.**

# ----- Coroutines -----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ----- WorkManager -----
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ----- Security / EncryptedSharedPreferences -----
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ----- DataStore -----
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ----- Coil (Image Loading) -----
-keep class coil.** { *; }
-dontwarn coil.**

# ----- Vico Charts -----
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ----- Timber Logging -----
# Strip all debug/verbose Timber calls in release builds
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static timber.log.Timber$Tree tag(...);
}
-keep class timber.log.Timber { *; }

# ----- Accompanist -----
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ----- Jetpack Compose -----
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ----- Navigation -----
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ----- Source file info for crash stack traces -----
# Keeps file names and line numbers in stack traces while still obfuscating class names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ----- Remove Android Log calls in release -----
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ----- Firebase Crashlytics -----
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ----- Prevent obfuscation of Application / Activity entry points -----
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

