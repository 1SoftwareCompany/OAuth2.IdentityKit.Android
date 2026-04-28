package com.onesoftware.identitykit.errors

enum class OAuth2ErrorType(val message: String) {
    // OAuth2 errors
    INVALID_REQUEST("The request is missing a required parameter"),
    INVALID_CLIENT("Unknown client, no client authentication included, or unsupported authentication method"),
    INVALID_GRANT("The provided authorization grant or refresh token is invalid"),
    UNAUTHORIZED_CLIENT("The authenticated client is not authorized to use this authorization grant type"),
    UNSUPPORTED_GRANT_TYPE("The authorization grant type is not supported by the authorization server"),
    INVALID_SCOPE("The requested scope is invalid"),

    ACCESS_DENIED("The resource owner denied the access request"),
    UNSUPPORTED_RESPONSE_TYPE("The authorization server does not support this response type"),
    SERVER_ERROR("The authorization server encountered an unexpected condition"),
    TEMPORARILY_UNAVAILABLE("The authorization server is temporarily unavailable"),

    INVALID_TOKEN_RESPONSE("The received access token response is not valid"),
    INVALID_METHOD("The requested method is not valid for this authorization"),
    INVALID_CONTENT_TYPE("Invalid content type for this authorization"),
    INVALID_REDIRECT_URI("The redirect URI is invalid or does not match"),
    INVALID_AUTHORIZATION_RESPONSE("The authorization response is invalid"),
    INVALID_STATE("The authorization response state is invalid"),
    UNKNOWN("Unknown OAuth2 error")
}