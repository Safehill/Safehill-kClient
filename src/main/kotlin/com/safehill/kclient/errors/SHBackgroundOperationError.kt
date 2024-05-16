package com.safehill.kclient.errors

sealed class SHBackgroundOperationError : Exception() {
    data class UnexpectedData(val data: Any?) : SHBackgroundOperationError() {
        override fun getLocalizedMessage() = "unexpectedData: $data"
    }


    data class MissingAssetInLocalServer(val globalIdentifier: String) : SHBackgroundOperationError() {
        override fun getLocalizedMessage() = "Missing $globalIdentifier in local server assets"
    }

    data class FatalError(val errorString: String) : SHBackgroundOperationError() {
        override fun getLocalizedMessage() = "Fatal error: $errorString"
    }

}
