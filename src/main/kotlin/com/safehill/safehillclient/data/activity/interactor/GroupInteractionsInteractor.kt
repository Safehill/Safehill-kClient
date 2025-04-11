package com.safehill.safehillclient.data.activity.interactor

import com.safehill.kclient.controllers.UserInteractionController
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.InteractionsGroupSummaryDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.interactions.ReactionType
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.models.users.getOrNull
import com.safehill.safehillclient.data.activity.model.AbstractAssetActivity
import com.safehill.safehillclient.data.message.factory.MessageInteractorFactory
import com.safehill.safehillclient.data.message.interactor.MessageInteractor
import com.safehill.safehillclient.module.client.UserScope
import com.safehill.safehillclient.utils.extensions.appendToValue
import com.safehill.safehillclient.utils.extensions.removeFromValue
import com.safehill.safehillclient.utils.extensions.toFailure
import com.safehill.safehillclient.utils.extensions.toSuccess
import com.safehill.safehillclient.utils.extensions.toUnitResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupInteractionsInteractor(
    private val groupId: GroupId,
    private val abstractAssetActivity: AbstractAssetActivity,
    private val interactionController: UserInteractionController,
    private val userScope: UserScope,
    private val userProvider: UserProvider
) : MessageInteractor by MessageInteractorFactory(
    interactionController = interactionController,
    userScope = userScope,
    userProvider = userProvider
).create(
    anchorId = groupId,
    interactionAnchor = InteractionAnchor.GROUP,
    mutableMessagesContainer = abstractAssetActivity
) {

    suspend fun addReaction(reactionType: ReactionType): Result<Unit> {
        return userScope.async {
            val currentUser = userProvider.getOrNull()
            if (currentUser != null) {
                val ownReaction =
                    abstractAssetActivity.reactions.value.getReactionOfUser(currentUser)
                if (ownReaction != reactionType) {
                    addReactionToState(reactionType = reactionType)
                    interactionController.addReaction(
                        reactionType = reactionType, groupId = groupId
                    ).onFailure {
                        removeReactionFromState(reactionType = reactionType)
                    }.toUnitResult()
                } else {
                    Unit.toSuccess()
                }
            } else {
                "User Not Found".toFailure()
            }
        }.await()
    }

    private fun addReactionToState(reactionType: ReactionType) {
        abstractAssetActivity.reactions.update { initial ->
            val currentUser = userProvider.getOrNull()
            if (currentUser != null) {
                initial
                    .mapValues { it.value - currentUser.identifier }
                    .appendToValue(reactionType, currentUser.identifier)
            } else {
                initial
            }
        }
    }

    private fun removeReactionFromState(reactionType: ReactionType) {
        abstractAssetActivity.reactions.update { initial ->
            val currentUser = userProvider.getOrNull()
            if (currentUser != null) {
                initial.removeFromValue(reactionType, currentUser.identifier)
            } else {
                initial
            }
        }
    }

    suspend fun removeReaction(reactionType: ReactionType): Result<Unit> {
        return userScope.async {
            removeReactionFromState(reactionType)
            interactionController.removeReaction(reactionType, groupId)
                .onFailure {
                    addReactionToState(reactionType)
                }
        }.await()
    }

    suspend fun updateGroupSummary(groupInteractionsGroupSummaryDTO: InteractionsGroupSummaryDTO) {
        coroutineScope {
            launch {
                val reactions = groupInteractionsGroupSummaryDTO
                    .reactions
                    .groupBy { it.reactionType }
                    .mapKeys { ReactionType.fromServerValue(it.key) }
                    .mapValues { reactionDtos -> reactionDtos.value.map { it.senderUserIdentifier } }
                abstractAssetActivity.reactions.update { reactions }
            }
            launch {
                abstractAssetActivity.numOfComments.update { groupInteractionsGroupSummaryDTO.numComments }
            }
            launch {
                upsertMessageDTO(listOf(groupInteractionsGroupSummaryDTO.firstEncryptedMessage))
            }
        }
    }

    fun increaseCommentsCount(increment: Int) {
        abstractAssetActivity.numOfComments.update { it + increment }
    }

    companion object {
        fun Map<ReactionType, List<UserIdentifier>>.getReactionOfUser(user: ServerUser): ReactionType? {
            return this.entries
                .firstOrNull { reactionsMap ->
                    reactionsMap.value.any { it == user.identifier }
                }?.key
        }
    }
}

