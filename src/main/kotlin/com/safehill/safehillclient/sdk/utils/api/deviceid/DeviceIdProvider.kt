package com.safehill.safehillclient.sdk.utils.api.deviceid

fun interface DeviceIdProvider {
    fun getDeviceID(): String
}