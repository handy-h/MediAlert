# ── 通用 ──
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions

# ── Kotlin ──
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# ── ViewModel（AndroidViewModelFactory 通过反射创建 ViewModel） ──
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(android.app.Application);
}

# ── 保留枚举（Room 实体中的枚举字段需要） ──
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── OpenCSV ──
-dontwarn com.opencsv.**
-keep class com.opencsv.** { *; }
