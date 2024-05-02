package com.safehill.kclient.tasks

import java.util.concurrent.Executors
import java.util.function.Consumer


abstract class AbstractBackgroundOperation : BackgroundOperation {
    enum class State(val stringValue: String) {
        READY("Ready"),
        EXECUTING("Executing"),
        FINISHED("Finished");

        val keyPath: String
            get() = "is$stringValue"
    }

    private val stateExecutor = Executors.newSingleThreadExecutor()
    var state = State.READY
        set(newState) {
            val oldState = state
            stateExecutor.submit {
                field = newState
                // TODO: firePropertyChange(oldState.keyPath, newState.keyPath)
            }
        }

    abstract fun run(completionHandler: Consumer<Result<Unit>>)

    override fun run() {
        if (Thread.currentThread().isInterrupted) {
            state = State.FINISHED
            return
        }
        state = State.EXECUTING
        run( completionHandler = { Result.success { state = State.FINISHED } })
    }

    fun start() {
        if (Thread.currentThread().isInterrupted) {
            state = State.FINISHED
            return
        }
        state = State.READY
        run()
    }

    val isExecuting: Boolean
        get() = state == State.EXECUTING
    val isFinished: Boolean
        get() = state == State.FINISHED
}

