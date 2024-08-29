package com.safehill.kclient.network.remote

sealed class RemoteServerEnvironment(
    open val hostName: String,
    open val port: Int
) {
    class Development(
        override val hostName: String = "localhost",
        override val port: Int = 8080
    ) : RemoteServerEnvironment(hostName, port)

    class Staging(
        override val hostName: String = "safehill-stage-1-ec0cd53b3592.herokuapp.com",
        override val port: Int = 443
    ) : RemoteServerEnvironment(hostName, port)

    data object Production : RemoteServerEnvironment(
        hostName = "app.safehill.io",
        port = 443
    )
}