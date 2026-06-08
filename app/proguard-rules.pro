# ProGuard rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# OpenCSV
-dontwarn com.opencsv.**
-keep class com.opencsv.** { *; }
