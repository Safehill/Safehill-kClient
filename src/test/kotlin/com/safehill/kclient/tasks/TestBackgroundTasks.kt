package com.safehill.kclient.tasks

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Test implementation of BackgroundTask with a counter (useful for repeated tasks)
class TestBackgroundTaskWithInternalCounter(
    private val duration: Duration = 100.seconds
) : BackgroundTask {
    private val counter = AtomicInteger(0)
    override suspend fun run() {
        println("Task count $counter started")
        delay(duration)
        val counter = counter.incrementAndGet()
        println("Task count $counter completed.")
    }

    fun getCounterValue(): Int {
        return counter.get()
    }
}

class TestBackgroundTaskWithAtomicCounter(
    private val counter: AtomicInteger,
    private val duration: Long
) : BackgroundTask {
    override suspend fun run() {
        val counter = counter.incrementAndGet()
        println("Task count $counter started")
        delay(duration)
        println("Task count $counter completed.")
    }
}

class PostIncrementBackgroundTask(
    private val id: Int,
    private val counter: AtomicInteger,
    private val duration: Long
) : BackgroundTask {
    override suspend fun run() {
        val beforeCounter = counter.get()
        println("Task $id started. Counter: $beforeCounter")
        delay(duration)
        val afterCounter = counter.incrementAndGet()
        println("Task $id completed. Counter: $afterCounter")
    }
}