package com.safehill.kclient.tasks

import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

class BackgroundTaskProcessorTests {

    // Test implementation of BackgroundTask
    class TestBackgroundTask(private val id: Int, private val delayMillis: Long) : BackgroundTask {
        override suspend fun run() {
            println("Task $id started.")
            delay(delayMillis)
            println("Task $id completed.")
        }
    }

    @Test
    fun testSingleTaskExecution() {
        val processor = BackgroundTaskProcessor<BackgroundTask>()
        val task = TestBackgroundTask(1, 100)
        processor.run(task)
        Thread.sleep(500) // Wait for tasks to complete
        assertFalse(processor.isProcessing)
    }

    @Test
    fun testMultipleTaskExecution() {
        val processor = BackgroundTaskProcessor<BackgroundTask>()
        val task1 = TestBackgroundTask(1, 100)
        val task2 = TestBackgroundTask(2, 200)
        val task3 = TestBackgroundTask(3, 300)
        processor.run(task1)
        processor.run(task2)
        processor.run(task3)
        Thread.sleep(1000) // Wait for tasks to complete
        assertFalse(processor.isProcessing)
    }

    @Test
    fun testRepeatedTaskExecution() {
        val processor = BackgroundTaskProcessor<BackgroundTask>()
        val counter = AtomicInteger(0)
        val task = object : BackgroundTask {
            override suspend fun run() {
                println("Task started. Counter: ${counter.incrementAndGet()}")
                delay(100)
                println("Task completed.")
            }
        }
        processor.runRepeatedly(task, 200)
        Thread.sleep(1000) // Wait for tasks to complete
        assertEquals(5, counter.get()) // Should execute 5 times in 1 second
        assertFalse(processor.isProcessing)
    }

    @Test
    fun testNoTwoTasksRunning() {
        val processor = BackgroundTaskProcessor<BackgroundTask>()
        val counter = AtomicInteger(0)
        val task = object : BackgroundTask {
            override suspend fun run() {
                println("Task started. Counter: ${counter.incrementAndGet()}")
                delay(250)
                println("Task completed.")
            }
        }
        processor.runRepeatedly(task, 100)
        Thread.sleep(500) // Wait for tasks to complete
        assertEquals(2, counter.get()) // Should execute 3 times in 500 ms
        assert(processor.isProcessing)
    }
}