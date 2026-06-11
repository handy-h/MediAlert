# ProGuard rules
# -dontoptimize 禁用 R8 优化以保护 Kotlin 协程状态机不被 -allowaccessmodification 破坏（等效旧版 proguard-android.txt）
-dontoptimize

# ── 注解/签名保留（Kotlin + Room 运行时必需）──
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, Exceptions

# ── Room ──
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ── Kotlin 协程（防止 R8 破坏状态机）──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**

# Kotlin 标准库
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# ── OpenCSV ──
-dontwarn com.opencsv.**
-keep class com.opencsv.** { *; }
