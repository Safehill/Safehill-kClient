package com.safehill.kclient.models

enum class SHReactionType {
    Like, Sad, Love, Funny;

    fun toInt(): Int {
        return when (this) {
            Like -> 1
            Sad -> 2
            Love -> 3
            Funny -> 4
        }
    }
}
