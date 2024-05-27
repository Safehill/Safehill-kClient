package com.safehill.kclient.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

class BackgroundTaskProcessor<T : BackgroundTask>(
    processorScope: CoroutineScope,
    private val jobScope: CoroutineScope
) {

    internal val taskQueue = ConcurrentLinkedQueue<T>()
    private val processingMutex = Mutex()
    private var currentJob: Job? = null
    private var repeatingLifecycle: Job? = null

    init {
        processorScope.launch {
            while (isActive) {
                if (currentJob?.isActive != true && taskQueue.isNotEmpty()) {
                    processTasks()
                }
            }
        }
    }

    private fun processTasks() {
        currentJob = jobScope.launch {
            processingMutex.withLock {
                val task = taskQueue.poll()
                task?.run()
            }
        }
    }

    fun addTask(task: T) {
        taskQueue.add(task)
    }

    fun addTaskRepeatedly(task: T, repeatingIntervalDuration: Duration) {
        repeatingLifecycle = jobScope.launch {
            while (isActive) {
                if (currentJob?.isActive != true && taskQueue.isEmpty()) {
                    addTask(task)
                } else {
                    // Skip this cycle, as a task of the same kind is already running
                }
                delay(repeatingIntervalDuration)
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