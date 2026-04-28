package com.onesoftware.identitykit.errors

class OAuth2InvalidGrantError : OAuth2Error() {

    override val errorType: OAuth2ErrorType = OAuth2ErrorType.INVALID_GRANT
}