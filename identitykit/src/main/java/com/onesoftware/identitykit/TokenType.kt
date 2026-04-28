package com.onesoftware.identitykit

data class TokenType(val value: String) {

    override fun toString(): String = value

    companion object {
        val ACCESS_TOKEN = TokenType("access_token")
        val REFRESH_TOKEN = TokenType("refresh_token")
        val ID_TOKEN = TokenType("id_token")
        val SAML1 = TokenType("saml1")
        val SAML2 = TokenType("saml2")
    }
}