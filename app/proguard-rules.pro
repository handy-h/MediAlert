# ProGuard rules
# 注：不再使用 -dontoptimize。构建默认已采用 proguard-android-optimize.txt 启用 R8 优化，
# 全局禁用会使 APK 体积增大约 10-20%。下方 Room / 协程的 -keep 规则足以保护运行时结构，
# 现代 R8 对 Kotlin 协程状态机的优化是安全的。
# 如需最大化混淆可保留默认；如发布后遇到稀有问题，可针对具体类补充 -keep。

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
