package com.safehill.kclient.network.api.group

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.postRequestForResponse

class GroupApiImpl(
    baseApi: BaseApi
) : GroupApi, BaseApi by baseApi {

    override suspend fun deleteGroup(groupId: GroupId) {

    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): RecipientEncryptionDetailsDTO {
        return postRequestForResponse(
            endPoint = "groups/retrieve/$groupId",
            request = null
        )
    }
}