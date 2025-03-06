package com.safehill.safehillclient.dependencies

import com.safehill.safehillclient.data.authorization.UserAuthorizationRepository
import com.safehill.safehillclient.data.threads.ThreadsRepository
import com.safehill.safehillclient.data.user_discovery.UserDiscoveryRepository

interface Repositories : UserObserver {

    val userAuthorizationRepository: UserAuthorizationRepository

    val threadsRepository: ThreadsRepository

    val userDiscoveryRepository: UserDiscoveryRepository

}