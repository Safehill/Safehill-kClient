package com.safehill.kclient.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue

public class BackgroundTaskProcessor<T: BackgroundTask>(private val coroutineScope: CoroutineScope) {

    internal val taskQueue = ConcurrentLinkedQueue<T>()
    private val processingMutex = Mutex()
    private var currentJob: Job? = null
    private var repeatingLifecycle: Job? = null

    init {
        coroutineScope.launch {
            while (isActive) {
                if (currentJob?.isActive != true && taskQueue.isNotEmpty()) {
                    processTasks()
                }
            }
        }
    }

    private fun processTasks() {
        currentJob = coroutineScope.launch {
            processingMutex.withLock {
                val task = taskQueue.poll()
                task?.run()
            }
        }
    }

    fun addTask(task: T) {
        taskQueue.add(task)
    }

    fun addTaskRepeatedly(task: T, repeatingIntervalMillis: Long) {
        repeatingLifecycle = coroutineScope.launch {
            while (isActive) {
                if (currentJob?.isActive != true && taskQueue.isEmpty()) {
                    addTask(task)
                } else {
                    // Skip this cycle, as a task of the same kind is already running
                }
                delay(repeatingIntervalMillis)
            }
        }
    }

    fun stopRepeat() {
        repeatingLifecycle?.cancel()
    }

    fun cancelCurrent() {
        currentJob?.cancel()
    }
}