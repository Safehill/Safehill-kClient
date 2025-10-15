# Consumer ProGuard rules for Safehill-kClient

# ===========================
# Kotlin Serialization
# ===========================
-keep,includedescriptorclasses class com.safehill.kclient.**$$serializer { *; }
-keepclassmembers class com.safehill.kclient.** {
    *** Companion;
}
-keepclasseswithmembers class com.safehill.kclient.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===========================
# Cryptography & Security
# ===========================
-keep class java.security.** { *; }
-keep class javax.crypto.** { *; }
-keep class sun.security.** { *; }
-keep class com.android.org.conscrypt.** { *; }

# Keep all enums
-keep enum * { *; }

# ===========================
# Bouncy Castle - Keep Everything
# ===========================
-keep class org.bouncycastle.** { *; }
-keepclassmembers class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
