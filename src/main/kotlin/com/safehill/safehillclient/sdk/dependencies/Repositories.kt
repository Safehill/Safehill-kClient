package com.safehill.safehillclient.sdk.dependencies

import com.safehill.safehillclient.sdk.data.authorization.UserAuthorizationRepository
import com.safehill.safehillclient.sdk.data.threads.ThreadsRepository
import com.safehill.safehillclient.sdk.data.user_discovery.UserDiscoveryRepository

interface Repositories : UserObserver {

    val userAuthorizationRepository: UserAuthorizationRepository

    val threadsRepository: ThreadsRepository

    val userDiscoveryRepository: UserDiscoveryRepository

}