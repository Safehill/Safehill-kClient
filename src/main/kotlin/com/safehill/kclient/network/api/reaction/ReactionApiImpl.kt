package com.safehill.kclient.network.api.reaction

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.ReactionInputDTO
import com.safehill.kclient.models.dtos.ReactionOutputDTO
import com.safehill.kclient.models.dtos.RemoveReactionInputDTO
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.RequestMethod
import com.safehill.kclient.network.api.fireRequestForStringResponse
import com.safehill.kclient.network.api.postRequestForObjectResponse
import com.safehill.kclient.network.exceptions.SafehillError

class ReactionApiImpl(
    baseApi: BaseApi
) : ReactionApi, BaseApi by baseApi {

    override suspend fun addReactions(
        reactions: List<ReactionInputDTO>,
        toGroupId: GroupId
    ): List<ReactionOutputDTO> {
        require(reactions.size == 1) { throw SafehillError.ServerError.UnSupportedOperation }
        return postRequestForObjectResponse<ReactionInputDTO, ReactionOutputDTO>(
            endPoint = "interactions/assets-groups/$toGroupId/reactions",
            request = reactions.first(),
            authenticationRequired = true
        ).run(::listOf)
    }

    override suspend fun removeReaction(reaction: RemoveReactionInputDTO, fromGroupId: GroupId) {
        fireRequestForStringResponse(
            requestMethod = RequestMethod.Delete,
            endPoint = "interactions/assets-groups/$fromGroupId/reactions",
            request = reaction,
            authenticationRequired = true
        )
    }
}