package com.safehill.safehillclient.utils.api.deviceid

fun interface DeviceIdProvider {
    fun getDeviceID(): String
}