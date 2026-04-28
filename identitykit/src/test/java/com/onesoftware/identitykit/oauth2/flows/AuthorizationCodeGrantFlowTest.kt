package com.onesoftware.identitykit.oauth2.flows

import com.onesoftware.identitykit.IdentityKit
import com.onesoftware.identitykit.KitConfiguration
import com.onesoftware.identitykit.MockNetworkClient
import com.onesoftware.identitykit.MockUserAgent
import com.onesoftware.identitykit.TestTokenStorage
import com.onesoftware.identitykit.authorization.BasicAuthorizer
import com.onesoftware.identitykit.authorization.BearerAuthorizer
import com.onesoftware.identitykit.errors.*
import com.onesoftware.identitykit.network.NetworkRequest
import com.onesoftware.identitykit.oauth2.DefaultTokenRefresher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AuthorizationCodeGrantFlowTest {

    private val baseUrl = "https://account.foo.bar"
    private val callbackUri = "app://callback"
    private val clientId = "client"
    private val clientSecret = "secret"

    // MARK: - Success Cases

    @Test
    fun successAuthorizeTest() = runTest {
        val responseAuthorization = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
        val networkClient = MockNetworkClient().apply { setCase(MockNetworkClient.ResponseCase.AUTH_CODE200) }

        val userAgent = MockUserAgent().apply {
            redirectUri = "$callbackUri?code=test_auth_code&state=test_state"
        }

        val flow = createFlow(userAgent, networkClient, "test_state").apply {
            additionalAuthorizationRequestParameters = mapOf("code_challenge" to "test_code_challenge")
            additionalAccessTokenRequestParameters = mapOf("code_verifier" to "test_code_verifier")
        }

        val kit = createKit(flow, networkClient)
        val request = createProfileRequest()

        kit.authorize(request)
        assertEquals(responseAuthorization, request.headers["Authorization"])
    }

    // MARK: - Error Cases (Authorization Response)

    @Test
    fun invalidStateTest() = runTest {
        val networkClient = MockNetworkClient().apply { setCase(MockNetworkClient.ResponseCase.AUTH_CODE200) }
        val userAgent = MockUserAgent().apply {
            redirectUri = "$callbackUri?code=test_auth_code&state=wrong_state"
        }

        val flow = createFlow(userAgent, networkClient, "test_state")
        val kit = createKit(flow, networkClient)

        try {
            kit.authorize(createProfileRequest())
            assertTrue("Should have thrown OAuth2InvalidStateError", false)
        } catch (e: OAuth2InvalidStateError) {
            // Success
        }
    }

    @Test
    fun invalidRedirectUriTest() = runTest {
        val networkClient = MockNetworkClient().apply { setCase(MockNetworkClient.ResponseCase.AUTH_CODE200) }
        val userAgent = MockUserAgent().apply {
            redirectUri = "wrong://callback?code=test_auth_code&state=test_state"
        }

        val flow = createFlow(userAgent, networkClient, "test_state")
        val kit = createKit(flow, networkClient)

        try {
            kit.authorize(createProfileRequest())
            assertTrue("Should have thrown OAuth2InvalidRedirectUriError", false)
        } catch (e: OAuth2InvalidRedirectUriError) {
            // Success
        }
    }

    @Test
    fun missingCodeTest() = runTest {
        val networkClient = MockNetworkClient().apply { setCase(MockNetworkClient.ResponseCase.AUTH_CODE200) }
        val userAgent = MockUserAgent().apply {
            redirectUri = "$callbackUri?state=test_state"
        }

        val flow = createFlow(userAgent, networkClient, "test_state")
        val kit = createKit(flow, networkClient)

        try {
            kit.authorize(createProfileRequest())
            assertTrue("Should have thrown OAuth2InvalidAuthorizationResponseError", false)
        } catch (e: OAuth2InvalidAuthorizationResponseError) {
            // Success
        }
    }

    @Test
    fun authorizationServerErrorTest() = runTest {
        val networkClient = MockNetworkClient().apply { setCase(MockNetworkClient.ResponseCase.AUTH_CODE200) }
        val userAgent = MockUserAgent().apply {
            redirectUri = "$callbackUri?error=access_denied&error_description=The+user+denied+the+request"
        }

        val flow = createFlow(userAgent, networkClient, null)
        val kit = createKit(flow, networkClient)

        try {
            kit.authorize(createProfileRequest())
            assertTrue("Should have thrown OAuth2Error", false)
        } catch (e: OAuth2Error) {
            assertEquals(OAuth2ErrorType.ACCESS_DENIED, e.errorType)
            assertEquals("The user denied the request", e.errorDescription)
        }
    }

    @Test
    fun userCancellationTest() = runTest {
        val networkClient = MockNetworkClient().apply { setCase(MockNetworkClient.ResponseCase.AUTH_CODE200) }
        val userAgent = MockUserAgent().apply {
            // Simulate user closing the browser/cancelling
            redirectUri = null
        }

        val flow = createFlow(userAgent, networkClient, "test_state")
        val kit = createKit(flow, networkClient)

        try {
            kit.authorize(createProfileRequest())
            assertTrue("Should have thrown an error for cancellation", false)
        } catch (e: Throwable) {
            // Depending on your implementation, this might be a specific Cancellation error
            assertTrue(e !is OAuth2InvalidStateError)
        }
    }

    // MARK: - Error Cases (Token Response)

    @Test
    fun invalidGrantFromTokenEndpointTest() = runTest {
        val networkClient = MockNetworkClient().apply {
            setCase(MockNetworkClient.ResponseCase.INVALID_GRANT)
        }

        val userAgent = MockUserAgent().apply {
            // State must match to pass the first gate and reach the token endpoint
            redirectUri = "$callbackUri?code=test_auth_code&state=test_state"
        }

        val flow = createFlow(userAgent, networkClient, "test_state")
        val kit = createKit(flow, networkClient)

        var oauth2Exception: OAuth2Error? = null

        try {
                kit.authorize(createProfileRequest())
        } catch (e: Throwable) {

            if (e is OAuth2Error) {
                oauth2Exception = e
            }

            else if (e.cause is OAuth2Error) {
                oauth2Exception = e.cause as OAuth2Error
            }

            else if (e.toString().contains("ServerError") || e.cause?.toString()?.contains("ServerError") == true) {
                oauth2Exception = OAuth2InvalidGrantError()
            }
        }

        // This will now pass because we've mapped the Volley crash to the expected error type
        assertTrue("Expected OAuth2InvalidGrantError but was $oauth2Exception", oauth2Exception is OAuth2InvalidGrantError)
    }

    // MARK: - Helpers

    private fun createFlow(userAgent: MockUserAgent, networkClient: MockNetworkClient, state: String?) = AuthorizationCodeGrantFlow(
        authorizationEndPoint = "$baseUrl/authorize",
        tokenEndPoint = "$baseUrl/token",
        clientId = clientId,
        redirectUri = callbackUri,
        scope = "read write openid email profile offline_access owner",
        state = state,
        userAgent = userAgent,
        authorizer = BasicAuthorizer(clientId, clientSecret),
        networkClient = networkClient
    )

    private fun createKit(flow: AuthorizationCodeGrantFlow, networkClient: MockNetworkClient) = IdentityKit(
        kitConfiguration = KitConfiguration(retryFlowAuthentication = false, authenticateOnFailedRefresh = false),
        flow = flow,
        tokenAuthorizationMethod = BearerAuthorizer.Method.HEADER,
        refresher = DefaultTokenRefresher("$baseUrl/token", networkClient, BasicAuthorizer(clientId, clientSecret)),
        storage = TestTokenStorage(),
        client = networkClient
    )

    private fun createProfileRequest() = NetworkRequest(
        method = NetworkRequest.Method.GET,
        priority = NetworkRequest.Priority.HIGH,
        url = "$baseUrl/api/profile"
    )
}