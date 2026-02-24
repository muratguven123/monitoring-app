# ----- Retrofit -----
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ----- OkHttp -----
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ----- Gson -----
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ----- DTO Models (API Response) -----
-keep class com.monitoring.dashboard.data.remote.dto.** { *; }

# ----- Room -----
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# ----- Hilt -----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# ----- Coroutines -----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ----- Vico Charts -----
-keep class com.patrykandpatrick.vico.** { *; }

# ----- General -----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
