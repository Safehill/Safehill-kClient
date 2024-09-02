package com.safehill.kclient.network.api.reaction

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.ReactionInputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.network.api.BaseApi

interface ReactionApi : BaseApi {


    /// Adds reactions to a share (group)
    /// - Parameters:
    ///   - reactions: the reactions details
    ///   - groupId: the group identifier
    /// - Returns:
    ///   - the list of reactions added
    suspend fun addReactions(
        reactions: List<ReactionInputDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO>


    /// Removes a reaction to an asset or a message
    /// - Parameters:
    ///   - reaction: the reaction type and references to remove
    ///   - fromGroupId: the group the reaction belongs to
    suspend fun removeReaction(reaction: ReactionOutputDTO, fromGroupId: GroupId)
}
