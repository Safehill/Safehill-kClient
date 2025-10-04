package com.safehill.auto_sign_in

import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kclient.util.retry.RetryManager
import com.safehill.kclient.util.retry.RetryPolicy
import com.safehill.kclient.util.retry.RetryState
import com.safehill.safehillclient.SafehillClient
import com.safehill.safehillclient.model.SignInResponse
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.seconds

class SignInRetryHandler(
    private val safehillClient: SafehillClient,
    retryPolicy: RetryPolicy = signInRetryPolicy
) {

    private val retryManager = RetryManager(retryPolicy)
    val retryState: StateFlow<RetryState> = retryManager.retryState

    suspend fun attemptAutoSignIn(): Result<SignInResponse> {
        return retryManager.executeResult {
            safehillClient.signIn()
        }
    }

}

private val signInRetryPolicy = RetryPolicy.ExponentialBackoff(
    maxAttempts = Int.MAX_VALUE,
    initialDelay = 1.seconds,
    maxDelay = 5.seconds,
    retryOn = {
        it is SafehillError.NetworkUnAvailable
    }
)