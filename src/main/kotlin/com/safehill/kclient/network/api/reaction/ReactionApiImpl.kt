package com.safehill.kclient.network.api.reaction

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.ReactionInputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.RequestMethod
import com.safehill.kclient.network.api.fireRequestForObjectResponse
import com.safehill.kclient.network.api.postForResponseObject

class ReactionApiImpl(
    override val requestor: LocalUser
) : ReactionApi {

    override suspend fun addReactions(
        reactions: List<ReactionInputDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        return postForResponseObject(
            endPoint = "interactions/assets-groups/$toGroupId/reactions",
            request = reactions,
            authenticationRequired = true
        )
    }

    override suspend fun removeReaction(reaction: ReactionOutputDTO, fromGroupId: GroupId) {
        return fireRequestForObjectResponse(
            requestMethod = RequestMethod.Delete,
            endPoint = "interactions/assets-groups/$fromGroupId/reactions",
            request = reaction,
            authenticationRequired = true
        )
    }
}