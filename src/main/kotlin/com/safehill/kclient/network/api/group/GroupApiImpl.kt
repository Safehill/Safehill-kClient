package com.safehill.kclient.network.api.group

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.api.postForResponseObject

class GroupApiImpl(override val requestor: LocalUser) : GroupApi {
    override suspend fun deleteGroup(groupId: GroupId) {

    }

    override suspend fun retrieveGroupUserEncryptionDetails(groupId: GroupId): RecipientEncryptionDetailsDTO {
        return postForResponseObject(
            endPoint = "groups/retrieve/$groupId",
            request = null,
            authenticationRequired = true
        )
    }
}