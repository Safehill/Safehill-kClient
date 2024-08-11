package com.safehill.kclient.base64

import java.util.Base64


fun ByteArray.encodeBase64() = Base64.getEncoder().encode(this)

fun ByteArray.base64EncodedString() = String(this.encodeBase64())

fun ByteArray.decodeBase64() = Base64.getDecoder().decode(this)

fun ByteArray.base64DecodedString() = String(this.decodeBase64())

