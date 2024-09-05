package com.safehill.kclient.network.api.group

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.network.api.BaseApi

interface GroupApi : BaseApi {


    /// Delete a group, related messages and reactions, given its id
    /// - Parameters:
    ///   - groupId: the group identifier
    suspend fun deleteGroup(groupId: GroupId)

    /// Retrieved the E2EE details for a group, if one exists
    /// - Parameters:
    ///   - groupId: the group identifier
    ///   - completionHandler: the callback method
    suspend fun retrieveGroupUserEncryptionDetails(
        groupId: GroupId,
    ): RecipientEncryptionDetailsDTO
}