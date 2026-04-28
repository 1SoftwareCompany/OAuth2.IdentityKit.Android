package com.onesoftware.identitykit.oauth2

import android.net.Uri

interface UserAgent {

    fun perform(request: Uri, redirectUri: String?, redirectionHandler: (Uri) -> Unit)
}