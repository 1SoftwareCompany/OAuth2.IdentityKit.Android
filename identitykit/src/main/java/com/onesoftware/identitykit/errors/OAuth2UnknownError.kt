package com.onesoftware.identitykit.errors

class OAuth2UnknownError(
    private val rawError: String
) : OAuth2Error() {
    override val errorType = OAuth2ErrorType.UNKNOWN

    override val message: String
        get() = errorDescription ?: "Unknown OAuth2 error: $rawError"
}