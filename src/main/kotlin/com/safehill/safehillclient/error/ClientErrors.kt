package com.safehill.safehillclient.error

class UserContextMismatch :
    Exception("The user has been initialized for a different user. Sign out from the previous user to set a new user.")