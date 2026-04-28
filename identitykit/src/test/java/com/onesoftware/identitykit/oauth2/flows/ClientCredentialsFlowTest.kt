package com.onesoftware.identitykit.oauth2.flows

import com.onesoftware.identitykit.IdentityKit
import com.onesoftware.identitykit.KitConfiguration
import com.onesoftware.identitykit.MockNetworkClient
import com.onesoftware.identitykit.TestTokenStorage
import com.onesoftware.identitykit.authorization.BasicAuthorizer
import com.onesoftware.identitykit.authorization.BearerAuthorizer
import com.onesoftware.identitykit.errors.OAuth2Error
import com.onesoftware.identitykit.errors.OAuth2InvalidGrantError
import com.onesoftware.identitykit.network.NetworkClient
import com.onesoftware.identitykit.network.NetworkRequest
import com.onesoftware.identitykit.oauth2.DefaultTokenRefresher
import com.onesoftware.identitykit.storage.REFRESH_TOKEN
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Created by IvanVatov on 11/7/2017.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ClientCredentialsFlowTest {

    private val configuration = KitConfiguration(
        retryFlowAuthentication = false,
        authenticateOnFailedRefresh = false
    )

    /**
     * Unit test on asynchronous implementation in this depth cannot be provided by currently available test instruments
     */
    @Test
    fun successAuthorizeTest() = runTest {

        val kit: IdentityKit
        val networkClient: NetworkClient
        networkClient = MockNetworkClient()
        networkClient.setCase(MockNetworkClient.ResponseCase.CC200OK)

        val authorizer = BasicAuthorizer("client", "secret")
        val flow = ClientCredentialsFlow(
            "https://account.foo.bar/token",
            "read write openid email profile offline_access owner",
            authorizer,
            networkClient
        )
        kit = IdentityKit(
            configuration,
            flow,
            BearerAuthorizer.Method.HEADER,
            DefaultTokenRefresher("https://account.foo.bar/token", networkClient, authorizer),
            TestTokenStorage(),
            networkClient
        )

        val request = NetworkRequest(
            NetworkRequest.Method.GET,
            NetworkRequest.Priority.HIGH,
            "https://account.foo.bar/api/profile"
        )

        runBlocking {
            kit.authorize(request)
        }

        val authHeaderValue = request.headers["Authorization"]
        val responseAuthorization = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"

        Assert.assertTrue(authHeaderValue.equals(responseAuthorization))
    }

    @Test
    fun invalidGrandTest() = runBlockingTest {

        val kit: IdentityKit
        val networkClient: NetworkClient
        networkClient = MockNetworkClient()
        networkClient.setCase(MockNetworkClient.ResponseCase.INVALID_GRANT)

        // building IdentityKit
        val authorizer = BasicAuthorizer("client", "secret")
        val flow = ClientCredentialsFlow(
            "https://account.foo.bar/token",
            "read write openid email profile offline_access owner",
            authorizer,
            networkClient
        )
        kit = IdentityKit(
            configuration,
            flow,
            BearerAuthorizer.Method.HEADER,
            DefaultTokenRefresher("https://account.foo.bar/token", networkClient, authorizer),
            TestTokenStorage(),
            networkClient
        )

        val request = NetworkRequest(
            NetworkRequest.Method.GET,
            NetworkRequest.Priority.HIGH,
            "https://account.foo.bar/api/profile"
        )

        var oauth2Exception: OAuth2Error? = null

        try {

            runBlocking {
                kit.authorize(request)
            }
        } catch (e: OAuth2InvalidGrantError) {

            oauth2Exception = e
        }

        Assert.assertTrue(oauth2Exception is OAuth2InvalidGrantError)
    }

    /**
     * Unit test on asynchronous implementation in this depth cannot be provided by currently available test instruments
     */
    @Test
    fun refreshToken() = runBlockingTest {

        val kit: IdentityKit
        val networkClient: NetworkClient
        val tokenStorage = TestTokenStorage()
        networkClient = MockNetworkClient()
        tokenStorage.write(REFRESH_TOKEN, "4f2aw4gf5ge0c3aa3as2e4f8a958c6")
        networkClient.setCase(MockNetworkClient.ResponseCase.REFRESH200)

        val authorizer = BasicAuthorizer("client", "secret")
        val flow = ClientCredentialsFlow(
            "https://account.foo.bar/token",
            "read write openid email profile offline_access owner",
            authorizer,
            networkClient
        )
        kit = IdentityKit(
            configuration,
            flow,
            BearerAuthorizer.Method.HEADER,
            DefaultTokenRefresher("https://account.foo.bar/token", networkClient, authorizer),
            tokenStorage,
            networkClient
        )

        val request = NetworkRequest(
            NetworkRequest.Method.GET,
            NetworkRequest.Priority.HIGH,
            "https://account.foo.bar/api/profile"
        )

        runBlocking {
            kit.authorize(request)
        }

        val authHeaderValue = request.headers["Authorization"]
        val responseAuthorization = "Bearer TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"

        Assert.assertTrue(authHeaderValue.equals(responseAuthorization))
    }

}