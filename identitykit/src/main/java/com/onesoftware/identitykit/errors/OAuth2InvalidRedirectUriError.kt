package com.onesoftware.identitykit.errors

class OAuth2InvalidRedirectUriError : OAuth2Error() {
    override val errorType = OAuth2ErrorType.INVALID_REDIRECT_URI
}