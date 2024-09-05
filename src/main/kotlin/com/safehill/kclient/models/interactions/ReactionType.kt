package com.safehill.kclient.models.interactions

enum class ReactionType(val serverValue: Int) {
    Like(1), Sad(2), Love(3), Funny(4);

    fun toServerValue(): Int {
        return this.serverValue
    }

    companion object {
        fun fromServerValue(value: Int): ReactionType {
            return ReactionType.entries.first { it.serverValue == value }
        }
    }
}
