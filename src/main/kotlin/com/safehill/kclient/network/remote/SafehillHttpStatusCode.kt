package com.safehill.kclient.network.remote

enum class SafehillHttpStatusCode(val statusCode: Int) {
    UnAuthorized(401),
    PaymentRequired(402),
    NotFound(404),
    MethodNotAllowed(405),
    Conflict(409);

    companion object {
        fun fromInt(value: Int): SafehillHttpStatusCode? {
            return entries.firstOrNull() { it.statusCode == value }
        }
    }
}