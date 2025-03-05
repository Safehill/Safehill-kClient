package com.safehill.safehillclient.sdk.data.user_discovery

import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.util.safeApiCall
import com.safehill.safehillclient.sdk.model.AppUser
import com.safehill.safehillclient.sdk.model.toAppUser
import com.safehill.safehillclient.sdk.utils.api.dispatchers.SdkDispatchers
import kotlinx.coroutines.withContext

class UserDiscoveryRepository(
    private val serverProxy: ServerProxy,
    private val sdkDispatchers: SdkDispatchers,
) {

    suspend fun getUsers(hashedPhoneNumbers: List<String>): Result<Map<String, AppUser>> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.getUsersWithPhoneNumber(hashedPhoneNumbers)
            }.map { map ->
                map.mapValues { it.value.toAppUser() }
            }
        }
    }

    suspend fun searchUsers(
        searchQuery: String,
        page: Int,
        per: Int
    ): Result<List<AppUser>> {
        return withContext(sdkDispatchers.io) {
            safeApiCall {
                serverProxy.searchUsers(
                    query = searchQuery,
                    per = per,
                    page = page
                ).map {
                    it.toAppUser()
                }
            }
        }
    }

}