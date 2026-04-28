package com.onesoftware.identitykit.errors

class OAuth2TemporarilyUnavailableError : OAuth2Error() {
    override val errorType = OAuth2ErrorType.TEMPORARILY_UNAVAILABLE
}