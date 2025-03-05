package com.safehill.safehillclient.sdk.backgroundsync

interface BackgroundTasksRegistryFactory {
    fun create(): BackgroundTasksRegistry
}