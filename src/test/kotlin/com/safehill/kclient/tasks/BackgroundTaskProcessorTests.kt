package com.safehill.kclient.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BackgroundTaskProcessorTests {

    @Test
    fun testSingleTaskExecution() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>()
        val counter = AtomicInteger(0)
        val task = TestBackgroundTaskWithAtomicCounter(counter, 100)
        coroutineScope.launch {
            processor.run(task)
        }
        Thread.sleep(500) // Wait for tasks to complete
        coroutineScope.cancel()

        assert(counter.get() == 1)
    }

    @Test
    fun testMultipleTaskExecution() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>()
        val task1 = TestBackgroundTaskWithInternalCounter(duration = 100.milliseconds)
        val task2 = TestBackgroundTaskWithInternalCounter(duration = 100.milliseconds)
        val task3 = TestBackgroundTaskWithInternalCounter(duration = 100.milliseconds)
        coroutineScope.launch {
            launch {
                processor.run(task1)
            }
            launch {
                processor.run(task2)
            }
            launch {
                processor.run(task3)
            }
        }
        Thread.sleep(1000) // Wait for tasks to complete
        assert(task1.getCounterValue() == 1)
        assert(task2.getCounterValue() == 0)
        assert(task2.getCounterValue() == 0)
    }

    @Test
    fun `test multiple task execution with various enqueue mode`() {
        runBlocking {
            val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
            val processor = BackgroundTaskProcessor<BackgroundTask>()
            val task1 = TestBackgroundTaskWithInternalCounter(duration = 100.seconds)
            val task2 = TestBackgroundTaskWithInternalCounter(duration = 100.seconds)

            // enqueue mode for the second task with
            // final expected values for first task and second task
            val listOfEnqueueForFirstAndSecondTask = listOf(
                (EnqueueMode.Enqueue) to (1 to 1),
                (EnqueueMode.DropLatest) to (1 to 0),
                (EnqueueMode.DropOnGoing) to (0 to 1),
            )
            coroutineScope.launch {
                listOfEnqueueForFirstAndSecondTask.forEach { (enqueueMode, expectedValues) ->
                    launch { processor.run(task1) }
                    delay(1.seconds)
                    launch { processor.run(task2, enqueueMode = enqueueMode) }
                    assert(task1.getCounterValue() == expectedValues.first)
                    assert(task1.getCounterValue() == expectedValues.second)
                }
            }.join()
        }
    }

    @Test
    fun testRepeatedTaskExecution() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>()
        val counter = AtomicInteger(0)
        val task = TestBackgroundTaskWithAtomicCounter(counter, 100)
        coroutineScope.launch {
            processor.run(task, repeatMode = RepeatMode.Repeating(200.milliseconds))
        }
        Thread.sleep(1000) // Wait for tasks to complete
        coroutineScope.cancel()

        assertEquals(
            4,
            counter.get()
        ) // Should execute 4 times in 1 second since each takes (200 + 100) = 300 ms
        assertFalse(coroutineScope.isActive)
    }


    @Test
    fun `bunch of tasks added with drop latest mode should only complete the initial task`() {
        runBlocking {
            val scope = CoroutineScope(Dispatchers.Default)
            val counter = AtomicInteger(0)
            val task = TestBackgroundTaskWithAtomicCounter(counter, 100)
            val processor = BackgroundTaskProcessor<BackgroundTask>()
            scope.launch {
                for (i in 1..10) {
                    launch {
                        processor.run(task, EnqueueMode.DropLatest)
                    }
                }
            }.join()
            assert(counter.get() == 1)
        }
    }

    @Test
    fun `tasks added drop latest mode should only complete the initial task`() {
        runBlocking {
            val scope = CoroutineScope(Dispatchers.Default)
            val counter = AtomicInteger(0)
            val task = TestBackgroundTaskWithAtomicCounter(counter, 100)
            val processor = BackgroundTaskProcessor<BackgroundTask>()
            scope.launch {
                for (i in 1..100) {
                    launch {
                        processor.run(task, EnqueueMode.DropLatest)
                    }
                }
            }.join()
            assert(counter.get() == 1)
        }
    }

    @Test
    fun `tasks added drop ongoing mode should only complete the final task`() {
        runBlocking {
            val scope = CoroutineScope(Dispatchers.Default)
            val firstCounter = AtomicInteger(0)
            val firstTask = TestBackgroundTaskWithAtomicCounter(firstCounter, 100)

            val processor = BackgroundTaskProcessor<BackgroundTask>()

            val secondCounter = AtomicInteger(10)
            val secondTask = PostIncrementBackgroundTask(2, secondCounter, 100)

            val job = scope.launch {
                for (i in 1..100) {
                    launch {
                        processor.run(firstTask, EnqueueMode.DropLatest)
                    }
                }
                delay(50)
                launch {
                    processor.run(secondTask, EnqueueMode.DropOnGoing)
                }
            }

            job.join()
            assert(firstCounter.get() == 1)
            assert(secondCounter.get() == 11)
        }
    }

    @Test
    fun `multiple tasks added should be executed sequentially rather than in parallel`() {
        runBlocking {
            val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
            val processor = BackgroundTaskProcessor<BackgroundTask>()
            val counter = AtomicInteger(0)
            val task = TestBackgroundTaskWithAtomicCounter(counter, 100)
            coroutineScope.launch {
                for (i in 1..20) {
                    launch {
                        processor.run(task, enqueueMode = EnqueueMode.Enqueue)
                    }
                }
            }

            delay(1000) // Wait for tasks to complete
            coroutineScope.cancel()

            assertEquals(10, counter.get()) // Should execute 5 times in 1 second
            assertFalse(coroutineScope.isActive)
        }

    }


    @Test
    fun testNoTwoTasksRunning() {
        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>()
        val counter = AtomicInteger(0)
        val task = TestBackgroundTaskWithAtomicCounter(counter, 250)
        coroutineScope.launch {
            processor.run(task, repeatMode = RepeatMode.Repeating(100.milliseconds))
        }
        Thread.sleep(500) // Wait for tasks to complete
        coroutineScope.cancel()

        assertEquals(2, counter.get()) // Should execute 3 times in 500 ms
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun testCancelTaskImmediately() = runBlocking {
        val counter = AtomicInteger(0)

        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>()

        val task1 = PostIncrementBackgroundTask(1, counter, 250)
        coroutineScope.launch {
            processor.run(task1)
        }
        coroutineScope.cancel()

        assertEquals(0, counter.get())
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun testCancelTaskBeforeItFinishes() = runBlocking {
        val counter = AtomicInteger(0)

        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>()

        val task2 = PostIncrementBackgroundTask(2, counter, 1000)
        coroutineScope.launch {
            processor.run(task2)
        }
        delay(500) // Wait for task to complete
        coroutineScope.cancel()

        assertEquals(0, counter.get())
        assertFalse(coroutineScope.isActive)
    }

    @Test
    fun testCancelTaskAfterFinished() = runBlocking {
        val counter = AtomicInteger(0)

        val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)
        val processor = BackgroundTaskProcessor<BackgroundTask>()

        val task3 = PostIncrementBackgroundTask(3, counter, 200)

        coroutineScope.launch {
            processor.run(task3)
        }.join()
        delay(500) // Wait for task to complete

        coroutineScope.cancel()

        assertEquals(1, counter.get())
        assertFalse(coroutineScope.isActive)
    }
}