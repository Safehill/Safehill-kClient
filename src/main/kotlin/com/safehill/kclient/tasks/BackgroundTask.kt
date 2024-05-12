package com.safehill.kclient.tasks

interface BackgroundTask {
    suspend fun run()
}