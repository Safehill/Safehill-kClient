package com.safehill.safehillclient.data.threads.registry

import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.models.users.getOrNull
import com.safehill.safehillclient.data.threads.model.MutableThreadState
import com.safehill.safehillclient.data.threads.model.ThreadState
import com.safehill.safehillclient.model.user.toAppUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ThreadStateRegistry(
    private val userController: UserController,
    private val userProvider: UserProvider
) {

    private val _threadStates: MutableStateFlow<Map<String, MutableThreadState>> =
        MutableStateFlow(mapOf())
    val threadStates: StateFlow<Map<String, ThreadState>> = _threadStates.asStateFlow()

    fun getMutableThreadState(threadID: String): MutableThreadState? {
        return _threadStates.value[threadID]
    }

    suspend fun upsertThreadStates(threadDtos: List<ConversationThreadOutputDTO>) {
        val threadStates = threadDtos.toThreadStates()
        _threadStates.update { initialMap ->
            initialMap + threadStates.associateBy { it.threadId }
        }
    }

    suspend fun setThreadStates(threadDtos: List<ConversationThreadOutputDTO>): List<ThreadState> {
        val threadStates = threadDtos.toThreadStates()
        _threadStates.update {
            threadStates.associateBy { it.threadId }
        }
        return threadStates
    }

    private suspend fun List<ConversationThreadOutputDTO>.toThreadStates(): List<MutableThreadState> {
        val userIdentifiers = this.flatMap { it.membersPublicIdentifier }.distinct()
        val usersResult = userController.getUsers(userIdentifiers).getOrNull() ?: return listOf()
        return this.mapNotNull { threadDto ->
            val currentUser = userProvider.getOrNull() ?: return@mapNotNull null
            val users = usersResult
                .getValues(threadDto.membersPublicIdentifier)
                ?.map(ServerUser::toAppUser) ?: return@mapNotNull null

            val state = _threadStates.value[threadDto.threadId] ?: run {
                MutableThreadState(
                    threadId = threadDto.threadId,
                    users = users,
                    invitedPhoneNumbers = threadDto.invitedUsersPhoneNumbers,
                    selfUser = currentUser.toAppUser(),
                    lastUpdatedAt = threadDto.lastUpdatedAt,
                    name = threadDto.name,
                    creatorIdentifier = threadDto.creatorPublicIdentifier
                )
            }
            state.update(
                name = threadDto.name,
                lastUpdatedAt = threadDto.lastUpdatedAt,
                users = users,
                invitedPhoneNumbers = threadDto.invitedUsersPhoneNumbers
            )
            state
        }
    }


    // Returns null if all keys cannot be found.
    private fun <T, K> Map<T, K>.getValues(keys: List<T>): List<K>? {
        return keys.map { key ->
            this[key] ?: return null
        }
    }

    fun deleteThreadState(threadId: String) {
        _threadStates.update { initialMap ->
            initialMap - threadId
        }
    }

    fun clear() {
        _threadStates.update { mapOf() }
    }
}