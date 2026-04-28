package com.onesoftware.identitykit.errors

class OAuth2InvalidAuthorizationResponseError: OAuth2Error() {
    override val errorType: OAuth2ErrorType = OAuth2ErrorType.INVALID_AUTHORIZATION_RESPONSE
}