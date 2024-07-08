package com.safehill.kclient.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class BackgroundTaskProcessor<T : BackgroundTask> {

    private val taskCounter = AtomicLong(0)

    private val mutex = Mutex()

    private val jobQueue = ConcurrentLinkedQueue<JobWithCounter>()

    suspend fun run(
        task: T,
        enqueueMode: EnqueueMode = EnqueueMode.DropLatest,
        repeatMode: RepeatMode = RepeatMode.Once
    ) {
        do {
            try {
                coroutineScope {
                    val jobId: Long = mutex.withLock {
                        enqueueMode.takeCorrespondingActions()
                        addTaskToQueue(task)
                    }
                    waitForCompletion(jobId)
                }
            } catch (_: TaskAlreadyOnQueue) {
            }
            delay(repeatMode.getWaitDuration())
        } while (coroutineContext.isActive && repeatMode is RepeatMode.Repeating)
    }

    private fun CoroutineScope.addTaskToQueue(task: T): Long {
        val jobId = taskCounter.incrementAndGet()
        val job = launch(
            start = CoroutineStart.LAZY
        ) {
            task.run()
        }
        jobQueue.add(job with jobId)
        job.invokeOnCompletion {
            jobQueue.removeIf { it.counter == jobId }
        }
        return jobId
    }

    private fun dropJobsInQueue() {
        while (jobQueue.isNotEmpty()) {
            jobQueue.poll()?.job?.cancel()
        }
    }

    private fun EnqueueMode.takeCorrespondingActions() {
        when (this) {
            EnqueueMode.DropOnGoing -> dropJobsInQueue()
            EnqueueMode.DropLatest -> if (jobQueue.isNotEmpty()) {
                throw TaskAlreadyOnQueue(jobIds = jobQueue.map { it.counter })
            }

            EnqueueMode.Enqueue -> {}
        }
    }

    private fun RepeatMode.getWaitDuration(): Duration {
        return when (this) {
            RepeatMode.Once -> 0.seconds
            is RepeatMode.Repeating -> this.interval
        }
    }

    private suspend fun waitForCompletion(jobId: Long) {
        var latestJob: Job?
        while (
            run {
                val jobWithCounter = jobQueue.peek()
                if (jobWithCounter == null) {
                    return
                } else {
                    latestJob = jobWithCounter.job
                    jobWithCounter.counter <= jobId && coroutineContext.isActive
                }
            }
        ) {
            latestJob?.join()
        }
    }
}

private data class TaskAlreadyOnQueue(val jobIds: List<Long>) : Exception()

private data class JobWithCounter(
    val job: Job,
    val counter: Long,
)

private infix fun Job.with(counter: Long) = JobWithCounter(
    counter = counter,
    job = this
)

enum class EnqueueMode {
    DropOnGoing,
    Enqueue,
    DropLatest;
}

sealed class RepeatMode {
    data object Once : RepeatMode()
    data class Repeating(val interval: Duration) : RepeatMode()
}