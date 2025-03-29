package com.safehill.safehillclient.utils.extensions

fun <T, R> Map<T, List<R>>.appendToValue(key: T, value: R): Map<T, List<R>> {
    return this + (key to (this.getOrDefault(key, listOf()) + value))
}

fun <T, R> Map<T, List<R>>.removeFromValue(key: T, value: R): Map<T, List<R>> {
    val removedValue = this.getOrElse(key) { return this } - value
    return this + (key to removedValue)
}

fun <T> List<T>.moveItemToFirst(predicate: (T) -> Boolean): List<T> {
    val mutableList = this.toMutableList()
    val item = mutableList.firstOrNull {
        predicate(it)
    }
    if (item != null) {
        if (mutableList.remove(item)) {
            mutableList.add(0, item)
        }
    }
    return mutableList.toList()
}