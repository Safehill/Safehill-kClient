package com.safehill.kclient.tasks

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger


open class BackgroundOperationProcessor<T : AbstractBackgroundOperation>(
    delayedStartInSeconds: Int,
    dispatchIntervalInSeconds: Int?
) {
    private val log: Logger = Logger.getLogger("com.gf.safehill.bop")
    private val dispatchIntervalInSeconds: Int?
    private val delayedStartInSeconds: Int
    private var started = false
    private val stateLock = Any()
    private val timerLock = Any()
    private var timer: Timer? = null
    private val timerExecutor = Executors.newScheduledThreadPool(1)
    // TODO: Review thread pool size
    private val operationQueue: ExecutorService = Executors.newFixedThreadPool(10)

    init {
        require(delayedStartInSeconds >= 0) { "Delayed start must be non-negative." }
        if (dispatchIntervalInSeconds != null && dispatchIntervalInSeconds < 0) {
            throw IllegalArgumentException("Dispatch interval must be non-negative.")
        }
        this.delayedStartInSeconds = delayedStartInSeconds
        this.dispatchIntervalInSeconds = dispatchIntervalInSeconds
    }

    fun repeat(operation: T) {
        synchronized(stateLock) {
            if (started) return
            started = true
        }
        scheduleCoroutine(operation, delayedStartInSeconds)
    }

    private fun scheduleCoroutine(operation: T, seconds: Int) {
        if (!started) return
        if (operationQueue.isTerminated) {
            runBlocking {
                launch {
                    if (!operation.isExecuting && started && operationQueue.isTerminated) {
                        operationQueue.submit(operation.clone())
                    }
                    delay(1000L)
                }
            }
        }
        if (dispatchIntervalInSeconds != null) {
            val dispatchInterval = Math.max(dispatchIntervalInSeconds, seconds)
            timerExecutor.schedule({ scheduleCoroutine(operation, 0) }, dispatchInterval.toLong(), TimeUnit.SECONDS)
        } else {
            log.warning("No dispatchIntervalInSeconds set. The operation will not repeat.")
        }
    }

    fun stopRepeat() {
        synchronized(stateLock) {
            started = false
            operationQueue.shutdownNow()
        }
        synchronized(timerLock) {
            if (timer != null) {
                timer!!.cancel()
            }
        }
    }
}


