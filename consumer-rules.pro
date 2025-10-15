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
# Bouncy Castle (loaded as security provider)
# ===========================
-keep class org.bouncycastle.jcajce.provider.digest.** { *; }
-keep class org.bouncycastle.jcajce.provider.symmetric.** { *; }
-keep class org.bouncycastle.jcajce.provider.keystore.** { *; }
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep class org.bouncycastle.jsse.provider.BouncyCastleJsseProvider { *; }
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper
-dontwarn javax.naming.**
