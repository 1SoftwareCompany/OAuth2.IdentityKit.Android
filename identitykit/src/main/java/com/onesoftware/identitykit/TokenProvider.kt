package com.onesoftware.identitykit


typealias Token = (String) -> Unit

/**
 * Required for Bearer authorization
 */
interface TokenProvider {

    fun provideToken(handler: Token)

    fun onAuthenticationException(throwable: Throwable)
}

