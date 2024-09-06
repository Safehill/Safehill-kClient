package com.safehill.kclient.network.api.group

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.postRequestForObjectResponse

class GroupApiImpl(override val requestor: LocalUser) : GroupApi, BaseApi {
    override suspend fun deleteGroup(groupId: GroupId) {

    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): RecipientEncryptionDetailsDTO {
        return postRequestForObjectResponse(
            endPoint = "groups/retrieve/$groupId",
            request = null,
            authenticationRequired = true
        )
    }
}