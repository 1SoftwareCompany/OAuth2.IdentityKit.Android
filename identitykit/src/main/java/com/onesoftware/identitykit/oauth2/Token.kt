/*
 * Copyright (c) 2017. Elders LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onesoftware.identitykit.oauth2

import com.onesoftware.identitykit.errors.OAuth2InvalidTokenResponseError
import com.onesoftware.identitykit.ext.getOptString
import org.json.JSONObject

/**
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">Successful Response</a>
 *  @constructor Access token object
 */
class Token(val jsonObject: JSONObject) {
    val accessToken: String
    val tokenType: String
    val expiresIn: Long?
    val refreshToken: String?
    val scope: String?

    // Contains any additional parameters from the token response
    val additionalParameters: Map<String, Any?>

    private val creationTime = System.currentTimeMillis() / 1000

    init {
        try {
            accessToken = jsonObject.getString("access_token")
            tokenType = jsonObject.getString("token_type")

            expiresIn =
                if (jsonObject.has("expires_in") && !jsonObject.isNull("expires_in")) {
                    jsonObject.getLong("expires_in")
                } else {
                    null
                }

            refreshToken =
                if (jsonObject.has("refresh_token") && !jsonObject.isNull("refresh_token")) {
                    jsonObject.getString("refresh_token")
                } else {
                    null
                }

            scope =
                if (jsonObject.has("scope") && !jsonObject.isNull("scope")) {
                    jsonObject.getString("scope")
                } else {
                    null
                }

            val knownKeys = setOf(
                "access_token",
                "token_type",
                "expires_in",
                "refresh_token",
                "scope"
            )

            val extras = mutableMapOf<String, Any?>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key !in knownKeys) {
                    extras[key] = jsonObject.opt(key)
                }
            }

            additionalParameters = extras
        } catch (e: Throwable) {
            throw OAuth2InvalidTokenResponseError()
        }
    }

    val isExpired: Boolean
        get() {
            val expiresIn = this.expiresIn ?: return false
            return System.currentTimeMillis() / 1000 >= creationTime + expiresIn
        }
}