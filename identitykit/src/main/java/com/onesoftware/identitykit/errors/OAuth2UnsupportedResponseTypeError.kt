package com.onesoftware.identitykit.errors

class OAuth2UnsupportedResponseTypeError : OAuth2Error() {
    override val errorType = OAuth2ErrorType.UNSUPPORTED_RESPONSE_TYPE
}