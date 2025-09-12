package com.safehill.kclient.tasks.upload

import com.safehill.kclient.network.exceptions.SafehillError
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Centralized error handling for upload operations
 */
object UploadErrorHandler {
    
    /**
     * Determines if an error is recoverable and should be retried
     */
    fun isRetriableError(error: Throwable): Boolean {
        return when (error) {
            is SocketTimeoutException -> true
            is ConnectException -> true
            is IOException -> true
            is SafehillError.ServerError -> true
            is SafehillError.TransportError -> true
            is SafehillError.ClientError -> {
                // Most client errors are not retriable except for specific cases
                false
            }
            else -> {
                // Check message for common recoverable errors
                error.message?.let { message ->
                    message.contains("timeout", ignoreCase = true) ||
                    message.contains("network", ignoreCase = true) ||
                    message.contains("connection", ignoreCase = true)
                } ?: false
            }
        }
    }
    
    /**
     * Categorizes errors for better user experience
     */
    fun categorizeError(error: Throwable): UploadErrorCategory {
        return when (error) {
            is SocketTimeoutException, 
            is ConnectException -> UploadErrorCategory.NETWORK
            
            is SafehillError.ClientError.Unauthorized -> UploadErrorCategory.AUTHENTICATION
            
            is SafehillError.ClientError.PaymentRequired -> UploadErrorCategory.AUTHORIZATION
            
            is SafehillError.ServerError -> UploadErrorCategory.SERVER
            
            is SafehillError.ClientError.BadRequest -> UploadErrorCategory.VALIDATION
            
            is SafehillError.TransportError -> UploadErrorCategory.NETWORK
            
            is IOException -> UploadErrorCategory.IO
            
            else -> UploadErrorCategory.UNKNOWN
        }
    }
    
    /**
     * Creates user-friendly error messages
     */
    fun getUserFriendlyMessage(error: Throwable): String {
        return when (categorizeError(error)) {
            UploadErrorCategory.NETWORK -> "Network connection issue. Please check your internet connection."
            UploadErrorCategory.AUTHENTICATION -> "Authentication failed. Please sign in again."
            UploadErrorCategory.AUTHORIZATION -> "You don't have permission to upload this content."
            UploadErrorCategory.SERVER -> "Server is temporarily unavailable. Please try again later."
            UploadErrorCategory.VALIDATION -> "Invalid upload data. Please check your content and try again."
            UploadErrorCategory.IO -> "File access issue. Please check the file and try again."
            UploadErrorCategory.UNKNOWN -> error.message ?: "An unexpected error occurred."
        }
    }
}

enum class UploadErrorCategory {
    NETWORK,
    AUTHENTICATION,
    AUTHORIZATION,
    SERVER,
    VALIDATION,
    IO,
    UNKNOWN
}