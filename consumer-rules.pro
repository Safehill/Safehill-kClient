# Consumer ProGuard rules for Safehill-kClient

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
