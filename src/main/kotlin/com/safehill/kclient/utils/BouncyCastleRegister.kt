package com.safehill.kclient.utils

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

fun setupBouncyCastle() {
    // Android registers its own BC provider. As it might be outdated and might not include
    // all needed ciphers, we substitute it with a known BC bundled in the app.
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
    }
    Security.addProvider(BouncyCastleProvider())
}