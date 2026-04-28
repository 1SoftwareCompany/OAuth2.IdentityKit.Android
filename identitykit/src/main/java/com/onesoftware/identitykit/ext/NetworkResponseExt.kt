package com.onesoftware.identitykit.ext

import com.onesoftware.identitykit.errors.*
import com.onesoftware.identitykit.network.NetworkResponse
import com.onesoftware.identitykit.oauth2.Token

fun NetworkResponse.parseToken(): Token {

    if (this.statusCode in 400..499) {

        // try found OAuth2Error
        OAuth2Error.getError(this)?.let {
            throw it
        }
    }

    if (this.statusCode in 200..299) {

        val jsonObject = this.getJson()
            ?: throw OAuth2InvalidTokenResponseError()
        return Token(jsonObject)
    }

    throw Error(this.error?.message)
}