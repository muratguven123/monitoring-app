# Instrumentation test APK when testBuildType = release.
# The app-under-test APK is fully R8-shrunk via proguard-rules.pro.
# Disable all R8 transforms here — minifying mockk/espresso/kotlin-reflect breaks at runtime.
-dontshrink
-dontoptimize
-dontobfuscate

# Optional test-framework references (would otherwise fail R8 classpath checks).
-dontwarn androidx.concurrent.futures.SuspendToFutureAdapter
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.sun.jna.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.lang.instrument.**
-dontwarn javax.tools.**
-dontwarn kotlinx.serialization.SealedSerializationApi
-dontwarn org.apiguardian.api.**
-dontwarn org.mockito.internal.creation.bytebuddy.inject.MockMethodDispatcher
-dontwarn org.slf4j.**

-keep class kotlin.reflect.** { *; }
-keep interface kotlin.reflect.** { *; }
