package com.safehill.kclient.tasks.outbound.sharing

import com.safehill.kclient.models.users.ServerUser

sealed interface SharingState {

    data object Pending : SharingState

    sealed class InProgress : SharingState {
        data object GettingRecipients : InProgress()
        data class Sharing(
            val recipients: List<ServerUser>
        ) : InProgress()

    }

    data object Success : SharingState

    data class Failed(
        val error: Throwable,
        val errorPhase: Phase
    ) : Exception(error), SharingState {
        enum class Phase {
            GETTING_RECIPIENTS, SHARING, UNKNOWN
        }
    }

    /**
     * Sharing has been cancelled by user
     */
    data object Cancelled : SharingState
}


val SharingState.InProgress.getCorrespondingError
    get() = when (this) {
        SharingState.InProgress.GettingRecipients -> SharingState.Failed.Phase.GETTING_RECIPIENTS
        is SharingState.InProgress.Sharing -> SharingState.Failed.Phase.SHARING
    }