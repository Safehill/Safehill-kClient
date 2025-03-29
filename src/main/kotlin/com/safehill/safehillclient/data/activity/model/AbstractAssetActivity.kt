package com.safehill.safehillclient.data.activity.model

import com.safehill.kclient.models.interactions.ReactionType
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.safehillclient.data.message.model.MutableMessagesContainer
import com.safehill.safehillclient.data.user.model.AppUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

// todo have a mutable state and an immutable state..
// The mutable state should be mutated only by the corresponding interactor
// and non immutable state should be exposed to the app

abstract class AbstractAssetActivity(
    val groupId: String,
    val eventOriginator: AppUser,
    val createdAt: Instant,
    assetIdentifiers: List<GlobalIdentifier>,
    shareInfo: Map<AppUser, Instant>,
) : MutableMessagesContainer() {

    val assetIdentifiers = MutableStateFlow(assetIdentifiers)
    val shareInfo = MutableStateFlow(shareInfo)
    val reactions = MutableStateFlow(mapOf<ReactionType, List<UserIdentifier>>())
    val numOfComments = MutableStateFlow(0)

    fun update(
        shareInfo: Map<AppUser, Instant>,
        assetIdentifiers: List<GlobalIdentifier>
    ) {
        this.shareInfo.update { shareInfo }
        this.assetIdentifiers.update { assetIdentifiers }
    }

}