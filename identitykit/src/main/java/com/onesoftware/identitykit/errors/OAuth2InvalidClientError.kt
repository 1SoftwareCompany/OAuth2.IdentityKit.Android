package com.onesoftware.identitykit.errors

class OAuth2InvalidClientError : OAuth2Error() {

    override val errorType: OAuth2ErrorType = OAuth2ErrorType.INVALID_CLIENT
}