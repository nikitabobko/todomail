package bobko.todomail.model

import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import bobko.todomail.login.createEmail
import bobko.todomail.util.InitializedLiveData
import bobko.todomail.util.MutableInitializedLiveData
import bobko.todomail.util.errorException
import bobko.todomail.util.mutableLiveDataOf
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
import java.util.concurrent.ThreadLocalRandom
import javax.mail.internet.MimeMessage
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object GoogleEmailCredential : EmailCredential() {
    private const val serverClientSecret = "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"
    private const val serverClientId =
        "473994673878-hpbjfm51euanc0molpthbesm82u3eatl.apps.googleusercontent.com"

    override val label: String
        get() = email?.let { "Google ($it)" } ?: "Sign in with Google"

    /**
     * https://developers.google.com/gmail/api/guides/sending
     * https://developers.google.com/gmail/api/reference/rest/v1/users.messages/send
     */
    override suspend fun sendEmail(
        activity: ComponentActivity,
        to: String,
        subject: String,
        body: String
    ) {
        val account = withContext(Dispatchers.Main) { signIn(activity) }
            ?: error("User isn't signed it")

        val accessToken = "https://accounts.google.com/o/oauth2/token"
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
            .access_token

        val raw = createEmail(to, subject, body).encodeToBase64()

        "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
            .httpPost()
            .header(
                "Authorization" to "Bearer $accessToken",
                "Accept" to "application/json",
                "Content-Type" to "application/json"
            )
            .objectBody(GmailMessageSendRequest(raw))
            .responseString()
            .value
    }

    suspend fun signIn(activity: ComponentActivity): GoogleSignInAccount? {
        GoogleSignIn.getLastSignedInAccount(activity)?.let {
            return updateSignInStatus(it)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .setLogSessionId(ThreadLocalRandom.current().nextInt().toString())
            .requestServerAuthCode(serverClientId)
            .requestScopes(Scope("https://www.googleapis.com/auth/gmail.send"))
            .requestEmail()
            .build()

        return suspendCoroutine { continuation ->
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
                    .addOnCompleteListener { task ->
                        try {
                            continuation.resume(updateSignInStatus(task.result))
                        } catch (ex: Throwable) {
                            // TODO log
                            continuation.resume(updateSignInStatus(null))
                        }
                    }
            }.launch(GoogleSignIn.getClient(activity, gso).signInIntent)
        }
    }

    private var email: String? = null
    private var _signed: MutableInitializedLiveData<Boolean> = mutableLiveDataOf(false)
    val signed: InitializedLiveData<Boolean>
        get() = _signed

    private fun updateSignInStatus(account: GoogleSignInAccount?) = account.also {
        email = it?.email
        _signed.value = it != null
    }

    fun signOut() {
        updateSignInStatus(null)
    }
}

private data class GoogleOauth2TokenResponse(val access_token: String)
private data class GmailMessageSendRequest(val raw: String)

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
