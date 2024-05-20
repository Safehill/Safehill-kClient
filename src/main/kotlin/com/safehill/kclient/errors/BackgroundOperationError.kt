package com.safehill.kclient.errors

sealed class BackgroundOperationError : Exception() {
    data class UnexpectedData(val data: Any?) : BackgroundOperationError() {
        override fun getLocalizedMessage() = "unexpectedData: $data"
    }


    data class MissingAssetInLocalServer(val globalIdentifier: String) : BackgroundOperationError() {
        override fun getLocalizedMessage() = "Missing $globalIdentifier in local server assets"
    }

    data class FatalError(val errorString: String) : BackgroundOperationError() {
        override fun getLocalizedMessage() = "Fatal error: $errorString"
    }

}
