package `in`.qwicklabs.vanam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import `in`.qwicklabs.vanam.databinding.ActivityLoginBinding
import `in`.qwicklabs.vanam.profile.CompleteProfile
import `in`.qwicklabs.vanam.utils.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val credentialManager = CredentialManager.create(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()

        setContentView(binding.root)

        binding.continueWithGoogle.setOnClickListener {
            val googleIdOption: GetGoogleIdOption =
                GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(true)
                    .setServerClientId(Strings.WEB_CLIENT_ID).build()

            val request: GetCredentialRequest =
                GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()


            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = credentialManager.getCredential(this@LoginActivity, request)
                    withContext(Dispatchers.Main) { googleSignIn(result) }
                } catch (e: GetCredentialException) {
                    withContext(Dispatchers.Main) { handleFailure(e) }
                }
            }
        }
    }

    private fun googleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("GetCredential", "Received an invalid google id token response", e)
                    }
                }
            }

            else -> {
                Log.e("GetCredential", "Unexpected type of credential")
            }
        }
    }

    private fun handleFailure(e: GetCredentialException) {
        when (e) {
            is androidx.credentials.exceptions.GetCredentialCancellationException -> {
                Log.w("GetCredential", "User canceled the sign-in process")
            }

            is androidx.credentials.exceptions.NoCredentialException -> {
                Log.w("GetCredential", "No matching credentials found")
            }

            is androidx.credentials.exceptions.GetCredentialUnknownException -> {
                Log.e("GetCredential", "Unknown error occurred", e)
            }

            is androidx.credentials.exceptions.GetCredentialProviderConfigurationException -> {
                Log.e("GetCredential", "Configuration error with credential provider", e)
            }

            else -> {
                Log.e("GetCredential", "Unhandled exception", e)
            }
        }
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    proceedToNextActivity()
                } else {
                    Log.e("Firebase", "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this@LoginActivity,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun proceedToNextActivity() {
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, CompleteProfile::class.java))
        finish()
    }
}