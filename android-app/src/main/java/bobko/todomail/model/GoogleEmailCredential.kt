package bobko.todomail.model

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import bobko.todomail.R
import bobko.todomail.login.createEmail
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.util.concurrent.ThreadLocalRandom
import javax.mail.internet.MimeMessage
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object GoogleEmailCredential : EmailCredential() {
    private const val serverClientSecret = "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"
    private const val serverClientId =
        "473994673878-hpbjfm51euanc0molpthbesm82u3eatl.apps.googleusercontent.com"

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
        val account = getSignedAccount(context) ?: error("User isn't signed it")

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

    class SignInActivityForResult(val launcher: ActivityResultLauncher<Intent>, val deferred: Deferred<GoogleSignInAccount?>)

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
        get() = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .setLogSessionId(ThreadLocalRandom.current().nextInt().toString())
            .requestServerAuthCode(serverClientId)
            .requestScopes(Scope("https://www.googleapis.com/auth/gmail.send"))
            .requestEmail()
            .build()

    private var continuation: Continuation<GoogleSignInAccount?>? = null // TODO It looks hacky. actor pattern? channel?

    suspend fun signIn(context: Context, launcher: ActivityResultLauncher<Intent>): GoogleSignInAccount? {
        return updateSignInStatus(
            context,
            suspendCoroutine<GoogleSignInAccount?> { continuation ->
                this.continuation = continuation
                launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
            }.also { continuation = null }
        )
    }

    override var email: String? = null
        private set
    private var _signed: MutableInitializedLiveData<Boolean> = mutableLiveDataOf(false)

    private fun getSignedAccount(context: Context) = GoogleSignIn.getLastSignedInAccount(context)

    fun isSigned(context: Context): InitializedLiveData<Boolean> {
        initSignInStatus(getSignedAccount(context))
        return _signed
    }

    override fun getLabel(context: Context): String {
        initSignInStatus(getSignedAccount(context))
        return email?.let { "Google ($it)" } ?: "Sign in with Google"
    }

    private fun initSignInStatus(account: GoogleSignInAccount?) {
        email = account?.email
        _signed.value = account != null
    }

    private fun updateSignInStatus(context: Context, account: GoogleSignInAccount?): GoogleSignInAccount? {
        initSignInStatus(account)
        context.writePref {
            val current = UniqueEmailCredential.All.read()
            when {
                account != null && current.none { it.credential == GoogleEmailCredential } -> {
                    current + UniqueEmailCredential.new(GoogleEmailCredential, context)
                }
                account == null && current.any { it.credential == GoogleEmailCredential } -> {
                    current.filter { it.credential != GoogleEmailCredential }
                }
                else -> null
            }?.let { UniqueEmailCredential.All.write(it) }
        }
        return account
    }

    override suspend fun signOut(context: Context) {
        suspendCoroutine<Unit> { continuation ->
            GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                continuation.resume(Unit)
            }
        }
        updateSignInStatus(context, null)
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
