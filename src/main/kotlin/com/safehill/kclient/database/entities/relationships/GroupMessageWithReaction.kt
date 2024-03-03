package com.safehill.snoog.core.datastore.database.entities.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.safehill.snoog.core.datastore.database.entities.GroupMessage
import com.safehill.snoog.core.datastore.database.entities.GroupReaction

data class GroupMessageWithReaction(
    @Embedded val asset: GroupMessage,
    @Relation(
        parentColumn = "id",
        entityColumn = "group_thread_id"
    )
    val reactions: List<GroupReaction>
)