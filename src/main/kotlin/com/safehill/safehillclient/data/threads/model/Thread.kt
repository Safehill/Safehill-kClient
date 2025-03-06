package com.safehill.safehillclient.data.threads.model

import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.safehillclient.model.user.AppUser
import java.time.Instant

data class Thread(
    val id: String,
    val users: List<AppUser>,
    val name: String?,
    val invitedPhoneNumbers: List<ThreadInvitedPhoneNumber>,
    val recentMessage: String?,
    val numOfSharedPhotos: Int,
    val lastActiveTime: Instant?,
    val selfUser: AppUser,
    val creatorIdentifier: UserIdentifier
) {
    val otherUsers = users.filter { it.identifier != selfUser.identifier }

    val isMultiUser: Boolean
        get() = otherUsers.count() != 1 || invitedPhoneNumbers.isNotEmpty()

    companion object {
        fun isValidNewName(
            initialName: String,
            newName: String
        ): Boolean {
            return newName != initialName && (newName.isBlank() || (newName.length in 4..20))
        }
    }
}

data class ThreadInvitedPhoneNumber(
    val phoneNumber: String,
    val invitedAt: Instant
)

sealed class RecentMessage {
    data class Image(val numOfSharedPhotos: Int) : RecentMessage()
    data class Text(val message: String) : RecentMessage()
    data object NoMessage : RecentMessage()
}
