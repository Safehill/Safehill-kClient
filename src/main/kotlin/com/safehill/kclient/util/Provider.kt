package com.safehill.kclient.util

interface Provider<T> {
    fun get(): T
}

fun <T> Provider(block: () -> T) = object : Provider<T> {
    override fun get(): T {
        return block()
    }
}