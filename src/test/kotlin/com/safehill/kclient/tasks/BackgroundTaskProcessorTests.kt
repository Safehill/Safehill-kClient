package com.safehill.kclient.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class BackgroundTaskProcessorTests {

    // Test implementation of BackgroundTask
    class TestBackgroundTask(private val id: Int, private val duration: Long) : BackgroundTask {
        override suspend fun run() {
            println("Task $id started.")
            delay(duration)
            println("Task $id completed.")
        }
    }

    // Test implementation of BackgroundTask with a counter (useful for repeated tasks)
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

    @Test
    fun testSingleTaskExecution() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>(coroutineScope)
        val task = TestBackgroundTask(1, 100)
        processor.addTask(task)
        Thread.sleep(500) // Wait for tasks to complete
        coroutineScope.cancel()

        assert(processor.taskQueue.isEmpty())
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun testMultipleTaskExecution() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>(coroutineScope)
        val task1 = TestBackgroundTask(1, 100)
        val task2 = TestBackgroundTask(2, 200)
        val task3 = TestBackgroundTask(3, 300)
        processor.addTask(task1)
        processor.addTask(task2)
        processor.addTask(task3)
        Thread.sleep(1000) // Wait for tasks to complete
        coroutineScope.cancel()

        assert(processor.taskQueue.isEmpty())
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun testRepeatedTaskExecution() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>(coroutineScope)
        val counter = AtomicInteger(0)
        val task = TestBackgroundTaskWithAtomicCounter(counter, 100)
        processor.addTaskRepeatedly(task, 200.milliseconds)
        Thread.sleep(1000) // Wait for tasks to complete
        coroutineScope.cancel()

        assertEquals(5, counter.get()) // Should execute 5 times in 1 second
        assert(processor.taskQueue.isEmpty())
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun `multiple tasks added should be executed sequentially rather than in parallel`() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>(coroutineScope)
        val counter = AtomicInteger(0)
        val task = TestBackgroundTaskWithAtomicCounter(counter, 100)
        for (i in 1..20) {
            processor.addTask(task)
        }

        Thread.sleep(1000) // Wait for tasks to complete
        coroutineScope.cancel()

        assertEquals(10, counter.get()) // Should execute 5 times in 1 second
        assertFalse(coroutineScope.isActive)
    }


    @Test
    fun testNoTwoTasksRunning() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>(coroutineScope)
        val counter = AtomicInteger(0)
        val task = TestBackgroundTaskWithAtomicCounter(counter, 250)
        processor.addTaskRepeatedly(task, 100.milliseconds)
        Thread.sleep(500) // Wait for tasks to complete
        processor.stopRepeat()
        coroutineScope.cancel()

        assertEquals(2, counter.get()) // Should execute 3 times in 500 ms
        assert(processor.taskQueue.isEmpty())
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun testCancelTaskImmediately() = runBlocking {
        val counter = AtomicInteger(0)

        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>(coroutineScope)

        val task1 = PostIncrementBackgroundTask(1, counter, 250)
        processor.addTask(task1)
        processor.cancelCurrent()
        coroutineScope.cancel()

        assertEquals(0, counter.get())
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun testCancelTaskBeforeItFinishes() = runBlocking {
        val counter = AtomicInteger(0)

        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>(coroutineScope)

        val task2 = PostIncrementBackgroundTask(2, counter, 1000)
        processor.addTask(task2)
        delay(50) // Wait for task to start
        processor.cancelCurrent()
        delay(500) // Wait for task to complete
        coroutineScope.cancel()

        assertEquals(0, counter.get())
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun testCancelTaskAfterFinished() = runBlocking {
        val counter = AtomicInteger(0)

        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>(coroutineScope)

        val task3 = PostIncrementBackgroundTask(3, counter, 200)
        processor.addTask(task3)
        delay(500) // Wait for task to complete
        processor.cancelCurrent()
        coroutineScope.cancel()

        assertEquals(1, counter.get())
        assert(processor.taskQueue.isEmpty())
        assertFalse(coroutineScope.isActive)
    }
}