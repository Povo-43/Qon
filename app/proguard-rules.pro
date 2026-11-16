# Hilt
-keep class dagger.hilt.** { *; }
-keep class com.example.ailearning.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.HiltViewModel

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.migration.Migration
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep @androidx.room.TypeConverters class *

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Retrofit/OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-keep class coil3.** { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Security Crypto
-keep class androidx.security.crypto.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializables
-keep class * implements java.io.Serializable

# Keep annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
