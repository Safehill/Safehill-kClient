package com.safehill.kclient.network.api.thread

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.serialization.responseObject
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.dtos.CreateOrUpdateThreadDTO
import com.safehill.kclient.models.dtos.HashedPhoneNumber
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.dtos.RetrieveThreadDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.getOrElseOnSafehillError
import com.safehill.kclient.network.api.getOrThrow
import com.safehill.kclient.network.api.postForResponseObject
import com.safehill.kclient.network.exceptions.SafehillError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

class ThreadApiImpl(override val requestor: LocalUser) : ThreadApi, BaseApi {
    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return listThreads(null)
    }

    private suspend fun listThreads(retrieveThreadDTO: RetrieveThreadDTO?): List<ConversationThreadOutputDTO> {
        return postForResponseObject<RetrieveThreadDTO, List<ConversationThreadOutputDTO>>(
            endPoint = "/threads/retrieve",
            request = retrieveThreadDTO,
            authenticationRequired = true
        )
    }

    override suspend fun retrieveThread(
        usersIdentifiers: List<UserIdentifier>,
        phoneNumbers: List<HashedPhoneNumber>
    ): ConversationThreadOutputDTO? {
        return listThreads(
            RetrieveThreadDTO(
                byUsersPublicIdentifiers = usersIdentifiers,
                byInvitedPhoneNumbers = phoneNumbers
            )
        ).firstOrNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun retrieveThread(threadId: String): ConversationThreadOutputDTO? {
        val bearerToken = this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        return "/threads/retrieve/$threadId".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .responseObject(
                ConversationThreadOutputDTO.serializer(),
                Json {
                    explicitNulls = false
                    ignoreUnknownKeys = true
                }
            )
            .getOrElseOnSafehillError {
                if (it is SafehillError.ClientError.NotFound) {
                    null
                } else {
                    throw it
                }
            }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun createOrUpdateThread(
        name: String?,
        recipientsEncryptionDetails: List<RecipientEncryptionDetailsDTO>
    ): ConversationThreadOutputDTO {
        val bearerToken = this.requestor.authToken ?: throw SafehillError.ClientError.Unauthorized

        val request = CreateOrUpdateThreadDTO(
            name = name,
            recipients = recipientsEncryptionDetails
        )
        val json = Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }


        return "/threads/upsert".httpPost()
            .header(mapOf("Authorization" to "Bearer $bearerToken"))
            .body(json.encodeToString(CreateOrUpdateThreadDTO.serializer(), request))
            .responseObject<ConversationThreadOutputDTO>(json)
            .getOrThrow()

    }

}