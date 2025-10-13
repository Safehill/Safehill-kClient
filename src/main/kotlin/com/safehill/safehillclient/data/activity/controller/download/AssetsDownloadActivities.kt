package com.safehill.safehillclient.data.activity.controller.download

import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.GroupInfo
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.safehillclient.data.activity.model.DownloadRequest
import com.safehill.safehillclient.data.message.model.Message
import com.safehill.safehillclient.data.message.model.MessageImageState
import com.safehill.safehillclient.data.message.model.MessageStatus
import com.safehill.safehillclient.data.message.model.MessageType
import com.safehill.safehillclient.data.user.model.AppUser
import com.safehill.safehillclient.data.user.model.toAppUser
import com.safehill.safehillclient.manager.dependencies.UserObserver
import com.safehill.safehillclient.module.client.UserScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class AssetsDownloadActivities(
    assetDescriptorsCache: AssetDescriptorsCache,
    private val userController: UserController,
    private val userScope: UserScope
) : UserObserver {

    private val assetDescriptors = assetDescriptorsCache.assetDescriptors

    private val _downloads = MutableStateFlow<Map<GroupId, DownloadRequest>>(mapOf())
    val downloads = _downloads.asStateFlow()

    private fun startCreationOfDownloadRequests(currentUser: LocalUser) {
        userScope.launch {
            assetDescriptors.collect { descriptors ->
                val senderUsersIdentifiers = descriptors.flatMap {
                    it.sharingInfo.groupIdsByRecipientUserIdentifier.keys
                }
                userController.getUsers(
                    userIdentifiers = senderUsersIdentifiers
                ).onSuccess { usersMap ->
                    val downloadRequests = createDownloadRequests(
                        assetDescriptors = descriptors,
                        usersMap = usersMap.mapValues { it.value.toAppUser() },
                        currentUser = currentUser
                    )
                    val newRequests = mutableListOf<DownloadRequest>()
                    downloadRequests.forEach { (groupId, downloadRequest) ->
                        _downloads.value[groupId]?.update(
                            shareInfo = downloadRequest.shareInfo.value,
                            assetIdentifiers = downloadRequest.assetIdentifiers.value
                        ) ?: run {
                            newRequests.add(
                                downloadRequest.also {
                                    it.addInitialPhotoMessage(currentUser)
                                }
                            )
                        }
                    }
                    _downloads.update { initial ->
                        initial + newRequests.associateBy { it.groupId }
                    }
                }
            }
        }
    }

    private fun DownloadRequest.addInitialPhotoMessage(
        currentUser: LocalUser
    ) {
        upsertMessage(
            Message(
                id = UUID.randomUUID().toString(),
                senderIdentifier = this.eventOriginator.identifier,
                createdDate = this.createdAt,
                status = MessageStatus.Sent,
                messageType = MessageType.Images(
                    groupId = this.groupId,
                    messageImageStates = this.assetIdentifiers.value.map { globalId ->
                        MessageImageState.Completed(globalId)
                    }
                ),
                userIdentifier = currentUser.identifier
            )
        )
    }


    private fun createDownloadRequests(
        assetDescriptors: List<AssetDescriptor>,
        usersMap: Map<String, AppUser>,
        currentUser: LocalUser
    ): Map<GroupId, DownloadRequest> {
        val assetIdentifierMap = mutableMapOf<GroupId, List<GlobalIdentifier>>()
        val groupInfoMap = mutableMapOf<GroupId, GroupInfo>()
        val senderUserMap = mutableMapOf<GroupId, AppUser>()
        val shareInfoMap = mutableMapOf<GroupId, Map<AppUser, Instant>>()

        fillInCorrespondingMaps(
            assetIdentifierMap = assetIdentifierMap,
            groupInfoMap = groupInfoMap,
            senderUserMap = senderUserMap,
            shareInfoMap = shareInfoMap,
            usersMap = usersMap,
            assetDescriptors = assetDescriptors,
            currentUser = currentUser
        )

        return assetIdentifierMap.mapNotNull { (groupId, assetIdentifiers) ->
            val eventOriginator = senderUserMap[groupId] ?: return@mapNotNull null
            val createdAt = shareInfoMap[groupId]
                ?.entries
                ?.firstOrNull { it.key.identifier == currentUser.identifier }
                ?.value ?: return@mapNotNull null
            val shareInfo = shareInfoMap[groupId] ?: return@mapNotNull null
            DownloadRequest(
                assetIdentifiers = assetIdentifiers.distinct(),
                groupId = groupId,
                eventOriginator = eventOriginator,
                shareInfo = shareInfo,
                createdAt = createdAt
            )
        }.associateBy { it.groupId }
    }

    private fun fillInCorrespondingMaps(
        assetIdentifierMap: MutableMap<GroupId, List<GlobalIdentifier>>,
        groupInfoMap: MutableMap<GroupId, GroupInfo>,
        senderUserMap: MutableMap<GroupId, AppUser>,
        shareInfoMap: MutableMap<GroupId, Map<AppUser, Instant>>,
        usersMap: Map<String, AppUser>,
        assetDescriptors: List<AssetDescriptor>,
        currentUser: LocalUser
    ) {
        assetDescriptors.forEach { descriptor ->
            if (descriptor.sharingInfo.sharedByUserIdentifier != currentUser.identifier) {
                val senderUser =
                    usersMap[descriptor.sharingInfo.sharedByUserIdentifier] ?: return@forEach
                descriptor.sharingInfo.groupIdsByRecipientUserIdentifier.forEach recipientsLoop@{ (userIdentifier, groupIds) ->
                    if (userIdentifier == senderUser.identifier) return@recipientsLoop
                    val receiver = usersMap[userIdentifier] ?: return@recipientsLoop
                    groupIds.forEach groupLoop@{ groupId ->
                        val groupInfo =
                            descriptor.sharingInfo.groupInfoById[groupId] ?: return@groupLoop
                        assetIdentifierMap.appendToExistingValue(
                            groupId,
                            descriptor.globalIdentifier
                        )
                        shareInfoMap.appendToExistingValue(groupId, receiver to groupInfo.createdAt)
                        groupInfoMap[groupId] = groupInfo
                        senderUserMap[groupId] = senderUser
                    }
                }
            }
        }
    }

    private fun <T, R> MutableMap<T, List<R>>.appendToExistingValue(key: T, value: R) {
        this[key] = this.getOrDefault(key, listOf()) + value
    }

    private fun <T, R, S> MutableMap<T, Map<R, S>>.appendToExistingValue(
        key: T,
        value: Pair<R, S>
    ) {
        this[key] = this.getOrDefault(key, mapOf()) + value
    }

    override suspend fun userLoggedIn(user: LocalUser) {
        startCreationOfDownloadRequests(currentUser = user)
    }

    override fun userLoggedOut() {}

}