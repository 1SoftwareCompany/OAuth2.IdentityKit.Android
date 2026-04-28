package com.onesoftware.identitykit.oauth2.flows

import com.onesoftware.identitykit.*
import com.onesoftware.identitykit.authorization.BasicAuthorizer
import com.onesoftware.identitykit.authorization.BearerAuthorizer
import com.onesoftware.identitykit.errors.OAuth2Error
import com.onesoftware.identitykit.errors.OAuth2InvalidGrantError
import com.onesoftware.identitykit.network.NetworkClient
import com.onesoftware.identitykit.network.NetworkRequest
import com.onesoftware.identitykit.oauth2.DefaultTokenRefresher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class TokenExchangeFlowTest {

    private val configuration = KitConfiguration(
        retryFlowAuthentication = false,
        authenticateOnFailedRefresh = false
    )

    private val tokenEndpoint = "https://account.foo.bar/token"

    @Test
    fun successTest() = runTest {
        val networkClient = MockNetworkClient().apply {
            setCase(MockNetworkClient.ResponseCase.TOKEN_EXCHANGE200)
        }

        val flow = createFlow(networkClient)
        val kit = createKit(flow, networkClient)

        val request = createProfileRequest()
        kit.authorize(request)

        Assert.assertNotNull(request.headers["Authorization"])
        Assert.assertTrue(request.headers["Authorization"]!!.contains("Bearer"))
    }

    @Test
    fun invalidGrantTest() = runTest {
        var providerError: Throwable? = null

        val networkClient = MockNetworkClient().apply {
            setCase(MockNetworkClient.ResponseCase.INVALID_GRANT)
        }

        val flow = createFlow(networkClient) { providerError = it }
        val kit = createKit(flow, networkClient)

        var caught: Throwable? = null
        try {
            kit.authorize(createProfileRequest())
        } catch (e: Throwable) {
            caught = e
        }

        Assert.assertNotNull("Provider should be notified", providerError)
        Assert.assertTrue(caught is OAuth2InvalidGrantError || caught is OAuth2Error)
    }

    @Test
    fun providerFailureTest() = runTest {
        val networkClient = MockNetworkClient()

        val flow = TokenExchangeFlow(
            tokenEndpoint,
            "read write openid email profile offline_access owner",
            BasicAuthorizer("client", "secret"),
            networkClient,
            object : TokenProvider {
                override fun provideToken(handler: (String) -> Unit) {
                    // Simulate a failure in the provider itself
                    throw RuntimeException("Provider failed")
                }

                override fun onAuthenticationException(throwable: Throwable) {
                }
            }
        )

        val kit = createKit(flow, networkClient)
        var caught: Throwable? = null

        try {
            kit.authorize(createProfileRequest())
        } catch (e: Throwable) {
            caught = e
        }

        Assert.assertTrue(caught?.message == "Provider failed")
    }

    // --- Helpers ---

    private fun createFlow(
        networkClient: NetworkClient,
        onAuthException: (Throwable) -> Unit = {}
    ): TokenExchangeFlow {
        val flow = TokenExchangeFlow(
            tokenEndpoint,
            // 1. MUST match the mock's scope string exactly
            "read write openid email profile offline_access owner",
            BasicAuthorizer("client", "secret"),
            networkClient,
            object : TokenProvider {
                override fun provideToken(handler: (String) -> Unit) {
                    // 2. MUST match the mock's subject_token exactly
                    handler.invoke("eyJraWQiOiJpZF8wIiwiYWxnIjoiUlMyNTYifQ")
                }

                override fun onAuthenticationException(throwable: Throwable) {
                    onAuthException(throwable)
                }
            }
        )

        return flow
    }

    private fun createKit(flow: TokenExchangeFlow, networkClient: NetworkClient): IdentityKit {
        return IdentityKit(
            configuration,
            flow,
            BearerAuthorizer.Method.HEADER,
            DefaultTokenRefresher(tokenEndpoint, networkClient, BasicAuthorizer("client", "secret")),
            TestTokenStorage(),
            networkClient
        )
    }

    private fun createProfileRequest() = NetworkRequest(
        NetworkRequest.Method.GET,
        NetworkRequest.Priority.HIGH,
        "https://account.foo.bar/api/profile"
    )
}