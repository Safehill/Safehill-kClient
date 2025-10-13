package com.safehill.safehillclient.phone_number_verification

import com.safehill.safehillclient.SafehillClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface PhoneNumberVerificationStatusBroadcaster {
    /**
     * This will emit after a call to signIn is a success
     * null means verificationStatus is unknown
     */
    val verificationStatus: Flow<Boolean?>
}

class PhoneNumberVerificationStatusBroadcasterImpl : PhoneNumberVerificationStatusBroadcaster {

    private val _verificationStatus = MutableSharedFlow<Boolean?>(
        extraBufferCapacity = 64
    )
    override val verificationStatus: Flow<Boolean?> = _verificationStatus.asSharedFlow()


    fun broadCastVerificationStatus(isVerified: Boolean?) {
        _verificationStatus.tryEmit(isVerified)
    }

}

val SafehillClient.phoneNumberVerificationStatusBroadcaster
    get() = this.authenticationCoordinator.phoneNumberVerificationStatusBroadcaster