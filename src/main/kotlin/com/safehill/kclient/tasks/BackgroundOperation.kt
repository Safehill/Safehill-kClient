package com.safehill.kclient.tasks

import java.util.logging.Logger


interface BackgroundOperation : Runnable {
    val log: Logger?

    /**
     * Used when the same operation is recursed on the operation queue
     * @return a new object initialized exactly as Self was
     */
    fun clone(): BackgroundOperation
}
