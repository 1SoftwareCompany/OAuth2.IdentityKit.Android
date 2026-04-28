package com.onesoftware.identitykit

import android.net.Uri
import com.onesoftware.identitykit.oauth2.UserAgent

class MockUserAgent : UserAgent {

    var redirectUri: String? = null

    override fun perform(
        request: Uri,
        redirectUri: String?,
        redirectionHandler: (Uri) -> Unit
    ) {
        redirectionHandler.invoke(Uri.parse(this.redirectUri))
    }
}