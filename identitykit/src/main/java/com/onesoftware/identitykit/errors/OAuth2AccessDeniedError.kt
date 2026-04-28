package com.onesoftware.identitykit.errors

class OAuth2AccessDeniedError : OAuth2Error() {
    override val errorType = OAuth2ErrorType.ACCESS_DENIED
}