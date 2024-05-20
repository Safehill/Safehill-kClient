package com.safehill.kclient.errors

sealed class LocalUserError : Exception() {

    data object InvalidKeychainEntry : LocalUserError() {
        private fun readResolve(): Any = InvalidKeychainEntry
        override fun getLocalizedMessage() = "invalid entry in the keychain"
    }

    data object FailedToRemoveKeychainEntry : LocalUserError() {
        private fun readResolve(): Any = FailedToRemoveKeychainEntry
        override fun getLocalizedMessage() = "failed to remove keychain entry"
    }

    data object MissingProtocolSalt : LocalUserError() {
        private fun readResolve(): Any = MissingProtocolSalt
        override fun getLocalizedMessage() = "protocol salt was never retrieved from server"
    }

    data object NotAuthenticated : LocalUserError() {
        private fun readResolve(): Any = NotAuthenticated
        override fun getLocalizedMessage() = "you need to be authenticated to perform this operation"
    }
}