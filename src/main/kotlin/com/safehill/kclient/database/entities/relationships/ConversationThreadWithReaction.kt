package com.safehill.snoog.core.datastore.database.entities.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.safehill.snoog.core.datastore.database.entities.ConversationThread
import com.safehill.snoog.core.datastore.database.entities.GroupReaction

data class ConversationThreadWithReaction(
    @Embedded val asset: ConversationThread,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversation_thread_id"
    )
    val reactions: List<GroupReaction>
)