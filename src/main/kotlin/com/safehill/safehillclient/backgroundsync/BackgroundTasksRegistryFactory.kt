package com.safehill.safehillclient.backgroundsync

interface BackgroundTasksRegistryFactory {
    fun create(): BackgroundTasksRegistry
}