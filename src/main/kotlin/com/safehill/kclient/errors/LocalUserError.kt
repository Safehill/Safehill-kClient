package com.safehill.kclient.errors

sealed class SHLocalUserError : Exception() {

    data object InvalidKeychainEntry : SHLocalUserError() {
        private fun readResolve(): Any = InvalidKeychainEntry
        override fun getLocalizedMessage() = "invalid entry in the keychain"
    }

    data object FailedToRemoveKeychainEntry : SHLocalUserError() {
        private fun readResolve(): Any = FailedToRemoveKeychainEntry
        override fun getLocalizedMessage() = "failed to remove keychain entry"
    }

    data object MissingProtocolSalt : SHLocalUserError() {
        private fun readResolve(): Any = MissingProtocolSalt
        override fun getLocalizedMessage() = "protocol salt was never retrieved from server"
    }

    data object NotAuthenticated : SHLocalUserError() {
        private fun readResolve(): Any = NotAuthenticated
        override fun getLocalizedMessage() = "you need to be authenticated to perform this operation"
    }
}