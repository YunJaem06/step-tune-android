package hs.project.steptune.feature.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import hs.project.steptune.Config

internal suspend fun requestGoogleIdToken(context: Context): String {
    val googleSignInOption = GetSignInWithGoogleOption.Builder(
        serverClientId = Config.GOOGLE_WEB_CLIENT_ID
    ).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleSignInOption)
        .build()
    val result = CredentialManager.create(context).getCredential(
        context = context,
        request = request
    )
    val credential = result.credential
    require(
        credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        "Google ID Token credential이 아닙니다."
    }
    return try {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    } catch (exception: Exception) {
        throw IllegalArgumentException("Google ID Token을 해석할 수 없습니다.", exception)
    }
}
