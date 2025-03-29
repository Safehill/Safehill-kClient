package com.safehill.safehillclient.manager.dependencies

import com.safehill.safehillclient.data.activity.repository.ActivityRepository
import com.safehill.safehillclient.data.authorization.UserAuthorizationRepository
import com.safehill.safehillclient.data.threads.ThreadsRepository
import com.safehill.safehillclient.data.user_discovery.UserDiscoveryRepository

interface Repositories : UserObserver {

    val userAuthorizationRepository: UserAuthorizationRepository

    val threadsRepository: ThreadsRepository

    val userDiscoveryRepository: UserDiscoveryRepository

    val activityRepository: ActivityRepository

}