package com.onesoftware.identitykit.errors

import com.onesoftware.identitykit.network.NetworkResponse

abstract class OAuth2Error : Exception() {
    abstract val errorType: OAuth2ErrorType

    var errorDescription: String? = null

    override val message: String
        get() = errorDescription ?: errorType.message

    companion object {

        internal fun getError(response: NetworkResponse): OAuth2Error? {
            val json = response.getJson() ?: return null

            val error = json.optString("error").takeIf { it.isNotBlank() } ?: return null
            val errorDescription = json.optString("error_description").takeIf { it.isNotBlank() }

            val result = fromErrorCode(error) ?: OAuth2UnknownError(error)
            result.errorDescription = errorDescription

            return result
        }

        internal fun fromErrorCode(error: String): OAuth2Error? {
            return when (error) {
                "invalid_request" -> OAuth2InvalidRequestError()
                "invalid_client" -> OAuth2InvalidClientError()
                "invalid_grant" -> OAuth2InvalidGrantError()
                "unauthorized_client" -> OAuth2UnauthorizedClientError()
                "unsupported_grant_type" -> OAuth2UnsupportedGrantTypeError()
                "invalid_scope" -> OAuth2InvalidScopeError()
                "access_denied" -> OAuth2AccessDeniedError()
                "unsupported_response_type" -> OAuth2UnsupportedResponseTypeError()
                "server_error" -> OAuth2ServerError()
                "temporarily_unavailable" -> OAuth2TemporarilyUnavailableError()
                else -> null
            }
        }
    }
}