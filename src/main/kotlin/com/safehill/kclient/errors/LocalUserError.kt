package com.safehill.kclient.errors

sealed class LocalUserError : Exception() {

    class InvalidKeychainEntry : LocalUserError() {
        override fun getLocalizedMessage() = "invalid entry in the keychain"
    }

    class FailedToRemoveKeychainEntry : LocalUserError() {
        override fun getLocalizedMessage() = "failed to remove keychain entry"
    }

    class MissingProtocolSalt : LocalUserError() {
        override fun getLocalizedMessage() = "protocol salt was never retrieved from server"
    }

    class NotAuthenticated : LocalUserError() {
        fun getLocalizedMessage() = "you need to be authenticated to perform this operation"
    }

    class UnAvailable : LocalUserError()
}