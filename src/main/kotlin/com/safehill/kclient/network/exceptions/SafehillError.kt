package com.safehill.kclient.network.exceptions

sealed class SafehillError(
    message: String?
) : Exception(message) {

    sealed class ClientError(
        val statusCode: Int,
        message: String,
    ) : SafehillError(message) {
        private fun readResolve(): Any = this

        data class BadRequest(override val message: String) : ClientError(400, message)

        data object Unauthorized : ClientError(401, "401 Unauthorized") {
            private fun readResolve(): Any = Unauthorized
        }

        data object PaymentRequired : ClientError(402, "402 Payment Required") {
            private fun readResolve(): Any = PaymentRequired
        }

        data object NotFound : ClientError(404, "404 Not Found") {
            private fun readResolve(): Any = NotFound
        }

        data object MethodNotAllowed : ClientError(405, "405 Method Not Allowed") {
            private fun readResolve(): Any = MethodNotAllowed
        }

        data object Conflict : ClientError(409, "409 Conflict") {
            private fun readResolve(): Any = Conflict
        }
    }

    sealed class ServerError(message: String) : SafehillError(message) {

        data class Generic(override val message: String) : ServerError(message)

        data object NotImplemented : ServerError("This functionality is not implemented yet") {
            private fun readResolve(): Any = NotImplemented
        }

        data object NoData : ServerError("Server returned no data") {
            private fun readResolve(): Any = NoData
        }

        data object UnSupportedOperation : ServerError("UnSupportedOperation") {
            private fun readResolve(): Any = UnSupportedOperation

        }

        class UnexpectedResponse(message: String) :
            ServerError("Unexpected response from server: $message")

        data object BadGateway : ServerError("The route doesn't exist on the server (yet)") {
            private fun readResolve(): Any = BadGateway
        }
    }

    sealed class TransportError(message: String?) : SafehillError(message) {
        data class Generic(val error: Throwable) : TransportError(error.message)
        data object TimedOut : TransportError("Request timed out") {
            private fun readResolve(): Any = TimedOut
        }
    }
}