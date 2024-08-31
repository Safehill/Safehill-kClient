package com.safehill.kcrypto.image_engine.model

data class CDNEnvironment(
    val hostName: String,
) {

    companion object {

        val Production get() = CDNEnvironment("safehill-prod.s3.us-east-1.wasabisys.com")

        val Staging get() = CDNEnvironment("safehill-stage.s3.us-east-1.wasabisys.com")

        val Test get() = CDNEnvironment("safehill-test.s3.us-east-1.wasabisys.com")

        val Development get() = CDNEnvironment("safehill-dev.s3.us-east-1.wasabisys.com")
    }
}