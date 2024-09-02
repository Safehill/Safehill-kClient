package com.safehill.kclient.network.api.reaction

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.ReactionInputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.RequestMethod
import com.safehill.kclient.network.api.fireRequestForObjectResponse
import com.safehill.kclient.network.api.postForResponseObject
import com.safehill.kclient.network.exceptions.SafehillError

class ReactionApiImpl(
    override val requestor: LocalUser
) : ReactionApi {

    override suspend fun addReactions(
        reactions: List<ReactionInputDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        require(reactions.size == 1) { throw SafehillError.ServerError.UnSupportedOperation }
        return postForResponseObject<ReactionInputDTO, ReactionOutputDTO>(
            endPoint = "interactions/assets-groups/$toGroupId/reactions",
            request = reactions.first(),
            authenticationRequired = true
        ).run(::listOf)
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