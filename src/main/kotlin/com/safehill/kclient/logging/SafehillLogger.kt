package com.safehill.kclient.logging

interface SafehillLogger {
    fun log(message: String)

    fun info(message: String)

    fun debug(message: String)

    fun error(message: String)

    fun verbose(message: String)
}

class DefaultSafehillLogger : SafehillLogger {
    override fun log(message: String) {
        println(message)
    }

    override fun info(message: String) {
        println(message)
    }

    override fun debug(message: String) {
        println(message)
    }

    override fun error(message: String) {
        println(message)
    }

    override fun verbose(message: String) {
        println(message)
    }
}