package com.onesoftware.identitykit.errors

class OAuth2InvalidStateError : OAuth2Error() {
    override val errorType = OAuth2ErrorType.INVALID_STATE
}