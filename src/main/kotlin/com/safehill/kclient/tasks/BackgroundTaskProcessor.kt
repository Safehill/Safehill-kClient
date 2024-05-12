package com.safehill.kclient.tasks

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class BackgroundTaskProcessor<T: BackgroundTask> {

    private val taskQueue = ConcurrentLinkedQueue<T>()
    internal var isProcessing = false

    fun run(task: T) {
        taskQueue.add(task)
        processTasks()
    }

    private fun processTasks() {
        if (!isProcessing) {
            isProcessing = true
            GlobalScope.launch {
                while (taskQueue.isNotEmpty()) {
                    val task = taskQueue.poll()
                    task?.run()
                }
                isProcessing = false
            }
        }
    }

    fun runRepeatedly(task: T, intervalMillis: Long) {
        GlobalScope.launch {
            while (true) {
                run(task)
                delay(intervalMillis)
            }
        }
    }
}