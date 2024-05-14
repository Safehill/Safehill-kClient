package com.safehill.kclient.tasks

public interface BackgroundTask {
    suspend fun run()
}