package com.safehill.kclient.errors

sealed class CipherError : Exception() {
    data class UnexpectedData(val data: Any?) : CipherError()

}