package com.onesoftware.identitykit

import com.android.volley.NetworkError
import com.android.volley.ServerError
import com.onesoftware.identitykit.network.NetworkClient
import com.onesoftware.identitykit.network.NetworkRequest
import com.onesoftware.identitykit.network.NetworkResponse
import java.nio.charset.Charset

class MockNetworkClient : NetworkClient {

    private var responseCase = ResponseCase.NO_INTERNET

    enum class ResponseCase {
        TOKEN_EXCHANGE200,
        AUTH_CODE200,
        CC200OK,
        OK200,
        REFRESH200,
        INVALID_GRANT,
        NO_INTERNET,
    }

    fun setCase(case: ResponseCase) {
        responseCase = case
    }

    override suspend fun execute(request: NetworkRequest): NetworkResponse {
        val bodyString = request.body?.toString(Charset.defaultCharset())
        val authorizationHeader = request.headers["Authorization"]

        return when {
            isPasswordGrantRequest(request, bodyString, authorizationHeader) -> {
                when (responseCase) {
                    ResponseCase.OK200 -> response200()
                    ResponseCase.INVALID_GRANT -> invalidGrant()
                    else -> internalServerError()
                }
            }

            isRefreshTokenRequest(request, bodyString, authorizationHeader) -> {
                when (responseCase) {
                    ResponseCase.OK200 -> response200()
                    ResponseCase.REFRESH200 -> response200refresh()
                    ResponseCase.INVALID_GRANT -> invalidGrant()
                    ResponseCase.NO_INTERNET -> noInternet()
                    else -> internalServerError()
                }
            }

            isClientCredentialsRequest(request, bodyString, authorizationHeader) -> {
                when (responseCase) {
                    ResponseCase.CC200OK -> response200CC()
                    ResponseCase.OK200 -> response200()
                    ResponseCase.REFRESH200 -> response200refresh()
                    ResponseCase.INVALID_GRANT -> invalidGrant()
                    else -> internalServerError()
                }
            }

            isAuthorizationCodeRequest(request, bodyString, authorizationHeader) -> {
                when (responseCase) {
                    ResponseCase.AUTH_CODE200 -> response200AuthorizationCode()
                    ResponseCase.OK200 -> response200()
                    ResponseCase.INVALID_GRANT -> invalidGrant()
                    ResponseCase.NO_INTERNET -> noInternet()
                    else -> internalServerError()
                }
            }

            isTokenExchangeRequest(request, bodyString, authorizationHeader) -> {
                when (responseCase) {
                    ResponseCase.TOKEN_EXCHANGE200 -> response200TokenExchange()
                    ResponseCase.OK200 -> response200()
                    ResponseCase.INVALID_GRANT -> invalidGrant()
                    ResponseCase.NO_INTERNET -> noInternet()
                    else -> internalServerError()
                }
            }

            isProfileRequest(request, authorizationHeader) -> {
                responseProfile()
            }

            else -> {
                internalServerError()
            }
        }
    }

    private fun isPasswordGrantRequest(
        request: NetworkRequest,
        bodyString: String?,
        authorizationHeader: String?
    ): Boolean {
        return request.method == NetworkRequest.Method.POST &&
                bodyString.equals(
                    "grant_type=password&username=gg@eldersoss.com&password=ggPass123&scope=read write openid email profile offline_access owner",
                    ignoreCase = true
                ) &&
                authorizationHeader.equals("Basic Y2xpZW50OnNlY3JldA==", ignoreCase = true)
    }

    private fun isRefreshTokenRequest(
        request: NetworkRequest,
        bodyString: String?,
        authorizationHeader: String?
    ): Boolean {
        return request.method == NetworkRequest.Method.POST &&
                bodyString.equals(
                    "grant_type=refresh_token&refresh_token=4f2aw4gf5ge0c3aa3as2e4f8a958c6",
                    ignoreCase = true
                ) &&
                authorizationHeader.equals("Basic Y2xpZW50OnNlY3JldA==", ignoreCase = true)
    }

    private fun isClientCredentialsRequest(
        request: NetworkRequest,
        bodyString: String?,
        authorizationHeader: String?
    ): Boolean {
        return request.method == NetworkRequest.Method.POST &&
                bodyString.equals(
                    "grant_type=client_credentials&scope=read write openid email profile offline_access owner",
                    ignoreCase = true
                ) &&
                authorizationHeader.equals("Basic Y2xpZW50OnNlY3JldA==", ignoreCase = true)
    }

    private fun isAuthorizationCodeRequest(
        request: NetworkRequest,
        bodyString: String?,
        authorizationHeader: String?
    ): Boolean {
        return request.method == NetworkRequest.Method.POST &&
                bodyString.equals(
                    "grant_type=authorization_code&code=test_auth_code&redirect_uri=app://callback&code_verifier=test_code_verifier",
                    ignoreCase = true
                ) &&
                authorizationHeader.equals("Basic Y2xpZW50OnNlY3JldA==", ignoreCase = true)
    }

    private fun isTokenExchangeRequest(
        request: NetworkRequest,
        bodyString: String?,
        authorizationHeader: String?
    ): Boolean {
        return request.method == NetworkRequest.Method.POST &&
                authorizationHeader.equals("Basic Y2xpZW50OnNlY3JldA==", ignoreCase = true) &&
                bodyString?.contains("grant_type=shopify_token_exchange") == true &&
                bodyString.contains("scope=") &&
                bodyString.contains("subject_token_type=id_token") &&
                bodyString.contains("subject_token=eyJraWQiOiJpZF8wIiwiYWxnIjoiUlMyNTYifQ")
    }

    private fun isProfileRequest(
        request: NetworkRequest,
        authorizationHeader: String?
    ): Boolean {
        return request.method == NetworkRequest.Method.POST &&
                authorizationHeader.equals(
                    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9",
                    ignoreCase = true
                )
    }

    private fun response200AuthorizationCode(): NetworkResponse {
        val response = NetworkResponse()
        response.statusCode = 200
        val headers: MutableMap<String, String> = mutableMapOf()
        putStandardHeaders(headers)
        response.headers = headers
        response.data =
            """
            {
              "access_token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9",
              "expires_in":3600,
              "token_type":"Bearer",
              "refresh_token":"4f2aw4gf5ge0c3aa3as2e4f8a958c6",
              "id_token":"eyJraWQiOiJpZF8wIiwiYWxnIjoiUlMyNTYifQ"
            }
            """.trimIndent().toByteArray()
        return response
    }

    private fun response200TokenExchange(): NetworkResponse {
        val response = NetworkResponse()
        response.statusCode = 200
        val headers: MutableMap<String, String> = mutableMapOf()
        putStandardHeaders(headers)
        response.headers = headers
        response.data =
            """
            {
              "access_token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9",
              "expires_in":3600,
              "token_type":"Bearer",
              "refresh_token":"4f2aw4gf5ge0c3aa3as2e4f8a958c6"
            }
            """.trimIndent().toByteArray()
        return response
    }

    private fun response200(): NetworkResponse {
        val response = NetworkResponse()
        response.statusCode = 200
        val headers: MutableMap<String, String> = mutableMapOf()
        putStandardHeaders(headers)
        response.headers = headers
        response.data =
            """
            {
              "access_token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9",
              "expires_in":3600,
              "token_type":"Bearer",
              "refresh_token":"4f2aw4gf5ge0c3aa3as2e4f8a958c6"
            }
            """.trimIndent().toByteArray()
        return response
    }

    private fun response200CC(): NetworkResponse {
        val response = NetworkResponse()
        response.statusCode = 200
        val headers: MutableMap<String, String> = mutableMapOf()
        putStandardHeaders(headers)
        response.headers = headers
        response.data =
            """
            {
              "access_token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9",
              "expires_in":3600,
              "token_type":"Bearer"
            }
            """.trimIndent().toByteArray()
        return response
    }

    private fun response200refresh(): NetworkResponse {
        val response = NetworkResponse()
        response.statusCode = 200
        val headers: MutableMap<String, String> = mutableMapOf()
        putStandardHeaders(headers)
        response.headers = headers
        response.data =
            """
            {
              "access_token":"TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ",
              "expires_in":3600,
              "token_type":"Bearer",
              "refresh_token":"4f2aw4gf5ge0c3aa3as2e4f8a958c6"
            }
            """.trimIndent().toByteArray()
        return response
    }

    private fun invalidGrant(): NetworkResponse {
        val response = NetworkResponse()
        response.statusCode = 400
        val headers: MutableMap<String, String> = mutableMapOf()
        putStandardHeaders(headers)
        response.headers = headers
        response.data = """{"error":"invalid_grant"}""".toByteArray()
        return response
    }

    private fun responseProfile(): NetworkResponse {
        val response = NetworkResponse()
        response.statusCode = 200
        val headers: MutableMap<String, String> = mutableMapOf()
        putStandardHeaders(headers)
        response.headers = headers
        response.data =
            """
            {
              "result": [
                {"type": "profileid", "value": "123"},
                {"type": "name", "value": "Identity Kit"}
              ]
            }
            """.trimIndent().toByteArray()
        return response
    }

    private fun internalServerError(): NetworkResponse {
        val response = NetworkResponse()
        response.error = Error(ServerError())
        response.statusCode = 500
        val headers: MutableMap<String, String> = mutableMapOf()
        putStandardHeaders(headers)
        response.headers = headers
        response.data = "Internal Server Error".toByteArray()
        return response
    }

    private fun noInternet(): NetworkResponse {
        val response = NetworkResponse()
        response.error = Error(NetworkError())
        response.statusCode = null
        response.headers = null
        response.data = null
        return response
    }

    companion object {
        fun putStandardHeaders(headers: MutableMap<String, String>): MutableMap<String, String> {
            headers["Cache-Control"] = "no-store, no-cache, max-age=0, private"
            headers["Pragma"] = "no-cache"
            headers["Content-Length"] = "1000"
            headers["Content-Type"] = "application/json; charset=utf-8"
            headers["Server"] = "Microsoft-IIS/10.0"
            headers["X-AspNet-Version"] = "4.0.30319"
            headers["X-Powered-By"] = "ASP.NET"
            headers["Date"] = "Tue, 22 Aug 2017 12:00:00 GMT"
            headers["Connection"] = "Keep-alive"
            return headers
        }
    }
}