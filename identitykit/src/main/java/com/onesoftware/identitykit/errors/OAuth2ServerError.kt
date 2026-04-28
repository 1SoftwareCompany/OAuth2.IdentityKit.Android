package com.onesoftware.identitykit.errors

class OAuth2ServerError : OAuth2Error() {
    override val errorType = OAuth2ErrorType.SERVER_ERROR
}