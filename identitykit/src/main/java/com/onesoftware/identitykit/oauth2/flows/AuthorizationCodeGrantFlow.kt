package com.onesoftware.identitykit.oauth2.flows

import android.net.Uri
import com.onesoftware.identitykit.authorization.Authorizer
import com.onesoftware.identitykit.errors.OAuth2Error
import com.onesoftware.identitykit.errors.OAuth2InvalidAuthorizationResponseError
import com.onesoftware.identitykit.errors.OAuth2InvalidRedirectUriError
import com.onesoftware.identitykit.errors.OAuth2InvalidStateError
import com.onesoftware.identitykit.errors.OAuth2UnknownError
import com.onesoftware.identitykit.ext.parseToken
import com.onesoftware.identitykit.network.DEFAULT_CHARSET
import com.onesoftware.identitykit.network.NetworkClient
import com.onesoftware.identitykit.network.NetworkRequest
import com.onesoftware.identitykit.oauth2.Token
import com.onesoftware.identitykit.oauth2.UserAgent
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AuthorizationCodeGrantFlow(
    private val authorizationEndPoint: String,
    private val tokenEndPoint: String,
    private val clientId: String,
    private val redirectUri: String?,
    private val scope: String?,
    private val state: String?,
    private val userAgent: UserAgent,
    private val authorizer: Authorizer?,
    private val networkClient: NetworkClient
) : AuthorizationFlow {

    var additionalAuthorizationRequestParameters: Map<String, String> = emptyMap()
    var additionalAccessTokenRequestParameters: Map<String, String> = emptyMap()

    override suspend fun authenticate(): Token {
        val authorizationResponse = performAuthorizationRequest()
        validateAuthorizationResponse(authorizationResponse)

        val params = Uri.Builder()
            .appendQueryParameter("grant_type", "authorization_code")
            .appendQueryParameter("code", authorizationResponse.code)
            .apply {
                redirectUri?.let { appendQueryParameter("redirect_uri", it) }

                if (authorizer == null) {
                    appendQueryParameter("client_id", clientId)
                }

                additionalAccessTokenRequestParameters.forEach { (key, value) ->
                    appendQueryParameter(key, value)
                }
            }
            .build()
            .query

        val decodedQuery = params?.let {
            java.net.URLDecoder.decode(it, DEFAULT_CHARSET.name())
        }

        val request = NetworkRequest(
            NetworkRequest.Method.POST,
            NetworkRequest.Priority.IMMEDIATE,
            tokenEndPoint,
            HashMap(),
            decodedQuery?.toByteArray(DEFAULT_CHARSET)
        )

        authorizer?.authorize(request)

        return networkClient.execute(request).parseToken()
    }

    private suspend fun performAuthorizationRequest(): AuthorizationResponse {
        val authorizationUri = Uri.parse(authorizationEndPoint)
            .buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .apply {
                redirectUri?.let { appendQueryParameter("redirect_uri", it) }
                scope?.let { appendQueryParameter("scope", it) }
                state?.let { appendQueryParameter("state", it) }

                additionalAuthorizationRequestParameters.forEach { (key, value) ->
                    appendQueryParameter(key, value)
                }
            }
            .build()

        return suspendCoroutine { continuation ->
            try {
                userAgent.perform(authorizationUri, redirectUri) { redirect ->
                    try {
                        if (!canHandle(redirect)) {
                            continuation.resumeWithException(
                               OAuth2InvalidRedirectUriError()
                            )
                            return@perform
                        }

                        val response = authorizationResponseFrom(redirect)
                        continuation.resume(response)
                    } catch (e: Throwable) {
                        continuation.resumeWithException(e)
                    }
                }
            } catch (e: Throwable) {
                continuation.resumeWithException(e)
            }
        }
    }

    private fun canHandle(uri: Uri): Boolean {
        if (redirectUri != null) {
            val expected = Uri.parse(redirectUri)

            if (expected.scheme != uri.scheme) return false
            if (expected.host != uri.host) return false
            if ((expected.path ?: "") != (uri.path ?: "")) return false
        }

        return true
    }

    private fun authorizationResponseFrom(uri: Uri): AuthorizationResponse {
        val errorCode = uri.getQueryParameter("error")
        if (errorCode != null) {
            val error = OAuth2Error.fromErrorCode(errorCode) ?: OAuth2UnknownError(errorCode)
            error.errorDescription = uri.getQueryParameter("error_description")
            throw error
        }

        val code = uri.getQueryParameter("code")
            ?: throw OAuth2InvalidAuthorizationResponseError()

        val state = uri.getQueryParameter("state")

        return AuthorizationResponse(code, state)
    }


    private fun validateAuthorizationResponse(response: AuthorizationResponse) {
        if (response.state != state) {
            throw OAuth2InvalidStateError()
        }
    }

    data class AuthorizationResponse(
        val code: String,
        val state: String?
    )
}