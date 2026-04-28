package com.onesoftware.identitykit.oauth2.flows

import android.net.Uri
import com.onesoftware.identitykit.TokenProvider
import com.onesoftware.identitykit.TokenType
import com.onesoftware.identitykit.authorization.Authorizer
import com.onesoftware.identitykit.errors.OAuth2Error
import com.onesoftware.identitykit.ext.parseToken
import com.onesoftware.identitykit.network.DEFAULT_CHARSET
import com.onesoftware.identitykit.network.NetworkClient
import com.onesoftware.identitykit.network.NetworkRequest
import com.onesoftware.identitykit.network.NetworkResponse
import com.onesoftware.identitykit.oauth2.Token
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TokenExchangeFlow(
    private val tokenEndPoint: String,
    private val scope: String? = null,
    private val authorizer: Authorizer,
    private val networkClient: NetworkClient,
    private val tokenProvider: TokenProvider
) : AuthorizationFlow {

    var additionalAccessTokenRequestParameters: Map<String, String> = emptyMap()

    override suspend fun authenticate(): Token {
        try {
            val subjectToken = provideToken()

            val accessTokenRequest = AccessTokenRequest(
                scope = scope,
                subjectToken = subjectToken
            )

            val request = urlRequest(accessTokenRequest)

            authorizer.authorize(request)

            val response = perform(request)

            OAuth2Error.getError(response)?.let { throw it }

            val token = accessTokenResponse(response)

            validate(token)


            return token
        } catch (e: Throwable) {
            tokenProvider.onAuthenticationException(e)
            throw e
        }
    }

    private suspend fun perform(request: NetworkRequest): NetworkResponse {
        return networkClient.execute(request)
    }

    open fun accessTokenResponse(response: NetworkResponse): Token {
        return response.parseToken()
    }

    open fun validate(token: Token) {
        // nothing to validate here
    }

    open fun parameters(accessTokenRequest: AccessTokenRequest): Map<String, String?> {
        return accessTokenRequest.dictionary + additionalAccessTokenRequestParameters
    }

    open fun urlRequest(accessTokenRequest: AccessTokenRequest): NetworkRequest {
        val builder = Uri.Builder()
        parameters(accessTokenRequest).forEach { (key, value) ->
            value?.let { builder.appendQueryParameter(key, it) }
        }

        return NetworkRequest(
            NetworkRequest.Method.POST,
            NetworkRequest.Priority.IMMEDIATE,
            tokenEndPoint,
            HashMap(),
            builder.build().query?.toByteArray(DEFAULT_CHARSET)
        )
    }

    private suspend fun provideToken(): String {

        return suspendCoroutine { continuation ->

            try {

                tokenProvider.provideToken { token  ->

                    continuation.resumeWith(Result.success(token))
                }

            } catch (e: Throwable) {

                continuation.resumeWithException(e)
            }
        }
    }

    data class AccessTokenRequest(
        val grantType: String = "shopify_token_exchange",
        val tokenType: TokenType = TokenType.ID_TOKEN,
        val scope: String? = null,
        val subjectToken: String
    ) {
        val dictionary: Map<String, String?>
            get() = mapOf(
                "grant_type" to grantType,
                "scope" to scope,
                "subject_token_type" to tokenType.value,
                "subject_token" to subjectToken
            )
    }
}