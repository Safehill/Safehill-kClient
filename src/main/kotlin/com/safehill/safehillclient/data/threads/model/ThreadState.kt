package com.safehill.safehillclient.data.threads.model

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.safehillclient.model.AppUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Instant

interface ThreadState : MessagesContainer {

    val threadId: String

    val assetIdentifiers: StateFlow<Set<AssetGlobalIdentifier>>

    val numOfSharedPhotos: StateFlow<Int>

    fun toThread(): Flow<Thread>
}

class MutableThreadState(
    override val threadId: String,
    private val selfUser: AppUser,
    private val creatorIdentifier: UserIdentifier,
    users: List<AppUser>,
    invitedPhoneNumbers: Map<String, Instant>,
    name: String?,
    lastUpdatedAt: Instant
) : ThreadState, MutableMessagesContainer() {

    private val _assetIdentifiers: MutableStateFlow<Set<AssetGlobalIdentifier>> =
        MutableStateFlow(setOf())
    override val assetIdentifiers: StateFlow<Set<AssetGlobalIdentifier>> =
        _assetIdentifiers.asStateFlow()

    private val _numOfSharedPhotos: MutableStateFlow<Int> = MutableStateFlow(0)
    override val numOfSharedPhotos: StateFlow<Int> = _numOfSharedPhotos.asStateFlow()

    private val _name: MutableStateFlow<String?> = MutableStateFlow(name)
    val name = _name.asStateFlow()

    private val _users: MutableStateFlow<List<AppUser>> = MutableStateFlow(users)
    val users: StateFlow<List<AppUser>> = _users.asStateFlow()

    private val _invitedPhoneNumbers: MutableStateFlow<Map<String, Instant>> =
        MutableStateFlow(invitedPhoneNumbers)
    val invitedPhoneNumbers = _invitedPhoneNumbers.asStateFlow()

    init {
        setLastUpdatedAt(lastUpdatedAt)
    }

    fun setAssetIdentifiers(identifiers: List<AssetGlobalIdentifier>) {
        _assetIdentifiers.update { identifiers.toSet() }
    }

    fun upsertAssetIdentifiers(identifiers: List<AssetGlobalIdentifier>) {
        _assetIdentifiers.update { initial -> initial + identifiers }
    }

    fun setTotalNumOfAssets(count: Int) {
        _numOfSharedPhotos.update { count }
    }

    fun setName(name: String?) {
        _name.update { name }
    }

    fun setUsers(users: List<AppUser>) {
        _users.update { users }
    }

    fun setInvitedPhoneNumbers(invitedPhoneNumbers: Map<String, Instant>) {
        _invitedPhoneNumbers.update { invitedPhoneNumbers }
    }

    fun update(
        name: String?,
        lastUpdatedAt: Instant,
        users: List<AppUser>,
        invitedPhoneNumbers: Map<String, Instant>
    ) {
        setName(name)
        setLastUpdatedAt(lastUpdatedAt)
        setUsers(users)
        setInvitedPhoneNumbers(invitedPhoneNumbers)
    }


    fun updateTotalNumOfSharedPhotos() {
        setTotalNumOfAssets(assetIdentifiers.value.size)
    }

    @Suppress("UNCHECKED_CAST")
    override fun toThread(): Flow<Thread> {
        return combine(
            messages,
            lastActiveDate,
            numOfSharedPhotos,
            name,
            users,
            invitedPhoneNumbers
        ) { values ->
            val messages = values[0] as List<Message>
            val lastActiveDate = values[1] as Instant
            val numOfSharedPhotos = values[2] as Int
            val name = values[3] as String?
            val users = values[4] as List<AppUser>
            val invitedPhoneNumbers = values[5] as Map<String, Instant>
            Thread(
                id = this.threadId,
                users = users,
                invitedPhoneNumbers = invitedPhoneNumbers.map { (phoneNumber, invitedAt) ->
                    ThreadInvitedPhoneNumber(
                        phoneNumber = phoneNumber,
                        invitedAt = invitedAt
                    )
                },
                name = name,
                // Since messages are sorted already
                recentMessage = (messages.lastOrNull {
                    it.messageType is MessageType.Text
                }?.messageType as? MessageType.Text)?.message,
                numOfSharedPhotos = numOfSharedPhotos,
                lastActiveTime = lastActiveDate,
                selfUser = selfUser,
                creatorIdentifier = creatorIdentifier
            )
        }
    }
}
