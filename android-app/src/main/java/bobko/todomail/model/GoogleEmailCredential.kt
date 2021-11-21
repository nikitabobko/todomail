package bobko.todomail.model

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import bobko.todomail.login.createEmail
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.util.*
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.result.Result
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.mail.internet.MimeMessage
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class GoogleEmailCredential(
    private val googleAccessToken: String,
    private val googleRefreshToken: String,
    override val email: String
) : EmailCredential() {

    override val isEmpty: Boolean get() = false

    companion object {
        private const val serverClientSecret = "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"
        private const val serverClientId = "473994673878-hpbjfm51euanc0molpthbesm82u3eatl.apps.googleusercontent.com"

        private var continuation: Continuation<GoogleSignInAccount?>? = null // TODO It looks hacky. actor pattern? channel?
        fun registerActivityForResult(activity: ComponentActivity): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                    .addOnCompleteListener { task ->
                        val continuationLocal = continuation
                        check(continuationLocal != null)
                        try {
                            continuationLocal.resume(task.result)
                        } catch (ex: Throwable) {
                            // TODO log
                            continuationLocal.resume(null)
                        }
                    }
            }
        }

        private val gso
            get() = GoogleSignInOptions.Builder()
                .requestServerAuthCode(serverClientId, /*forceCodeForRefreshToken = */true)
                .requestScopes(Scope("https://www.googleapis.com/auth/gmail.send"))
                .requestEmail()
                .build()

        suspend fun signIn(context: Context, launcher: ActivityResultLauncher<Intent>): GoogleEmailCredential? {
            try {
                val account = suspendCoroutine<GoogleSignInAccount?> { cont ->
                    continuation = cont
                    launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                }.also { continuation = null } ?: return null

                val accessToken = withContext(Dispatchers.IO) {
                    "https://accounts.google.com/o/oauth2/token"
                        .httpPost(
                            parameters = listOf(
                                "client_id" to serverClientId,
                                "client_secret" to serverClientSecret,
                                "grant_type" to "authorization_code",
                                "code" to account.serverAuthCode!!
                            )
                        )
                        .responseObject(jacksonDeserializerOf<GoogleOauth2TokenResponse>())
                        .value
                }

                return GoogleEmailCredential(
                    accessToken.access_token,
                    accessToken.refresh_token ?: error("Google API didn't return refresh token"),
                    account.email!!
                )
            } finally {
                suspendCoroutine<Unit> { continuation ->
                    GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    fun tryRefreshOauthToken() = try {
        val response = "https://oauth2.googleapis.com/token"
            .httpPost(
                parameters = listOf(
                    "client_id" to serverClientId,
                    "client_secret" to serverClientSecret,
                    "grant_type" to "refresh_token",
                    "refresh_token" to googleRefreshToken
                )
            )
            .responseObject(jacksonDeserializerOf<GoogleOauth2TokenResponse>())
            .value
        copy(googleAccessToken = response.access_token)
    } catch (ex: Throwable) {
        null
    }

    class Pref(index: Int) : SharedPref<GoogleEmailCredential>(null) {
        private val googleAccessToken by stringSharedPref("", index.toString())
        private val googleRefreshToken by stringSharedPref("", index.toString())
        private val email by stringSharedPref("", index.toString())

        override fun PrefWriterDslReceiver.writeImpl(value: GoogleEmailCredential?) {
            googleAccessToken.write(value?.googleAccessToken)
            googleRefreshToken.write(value?.googleRefreshToken)
            email.write(value?.email)
        }

        override fun PrefReaderDslReceiver.read(): GoogleEmailCredential {
            return GoogleEmailCredential(
                googleAccessToken.read(),
                googleRefreshToken.read(),
                email.read()
            )
        }
    }

    /**
     * https://developers.google.com/gmail/api/guides/sending
     * https://developers.google.com/gmail/api/reference/rest/v1/users.messages/send
     */
    override fun sendEmail(
        context: Context,
        to: String,
        subject: String,
        body: String
    ) {
        val raw = createEmail(to, subject, body).encodeToBase64()

        "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
            .httpPost()
            .header(
                "Authorization" to "Bearer $googleAccessToken",
                "Accept" to "application/json",
                "Content-Type" to "application/json"
            )
            .objectBody(GmailMessageSendRequest(raw))
            .responseString()
            .value
    }

    override val label: String get() = "Google ($email)"

    override suspend fun signOut(context: Context) {
        suspendCoroutine<Unit> { continuation ->
            GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                continuation.resume(Unit)
            }
        }
    }
}

data class GoogleOauth2TokenResponse(val access_token: String, val refresh_token: String? = null)
data class GmailMessageSendRequest(val raw: String)

private fun MimeMessage.encodeToBase64() = ByteArrayOutputStream().use {
    writeTo(it)
    Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP)
}

private val <T : Any> ResponseResultOf<T>.value: T
    get() {
        val (request, response, result) = this
        if (!response.isSuccessful) {
            error(
                """
                    Request failed with ${response.statusCode}
                    request=$request
                    response=$response
                """.trimIndent()
            )
        }
        return when (result) {
            is Result.Success -> {
                result.value
            }
            is Result.Failure -> {
                errorException(result.getException())
            }
        }
    }
