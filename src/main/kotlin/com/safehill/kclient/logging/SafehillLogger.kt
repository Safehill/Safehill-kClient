package com.safehill.kclient.logging

fun interface SafehillLogger {
    fun log(message: String)
}

class DefaultSafehillLogger : SafehillLogger {
    override fun log(message: String) {
        println(message)
    }
}