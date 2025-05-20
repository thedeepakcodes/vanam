package `in`.qwicklabs.vanam

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import `in`.qwicklabs.vanam.databinding.ActivityLoginBinding
import `in`.qwicklabs.vanam.profile.BasicActivity
import `in`.qwicklabs.vanam.utils.Loader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val credentialManager by lazy { CredentialManager.create(this) }
    private lateinit var loader: Loader

    // For Moving to Dashboard or Profile Setup
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userCollection by lazy {
        firestore.collection("Vanam").document("Users").collection("Profile")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loader = Loader(this)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        setContentView(binding.root)

        binding.continueWithGoogle.setOnClickListener {
            loader.title.text = "Signing In"
            loader.dialog.show()
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = credentialManager.getCredential(this@LoginActivity, request)
                    withContext(Dispatchers.Main) { handleSignInResult(result) }
                } catch (e: GetCredentialException) {
                    loader.dialog.dismiss()
                    withContext(Dispatchers.Main) { handleSignInFailure(e) }
                }
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdToken = GoogleIdTokenCredential
                            .createFrom(credential.data)
                            .idToken

                        firebaseAuthWithGoogle(googleIdToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        loader.dialog.dismiss()
                        Toast.makeText(
                            this,
                            "Authentication error: Invalid token format",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            else -> {
                loader.dialog.dismiss()
                Toast.makeText(
                    this,
                    "Unsupported authentication method",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleSignInFailure(e: GetCredentialException) {
        when (e) {
            is GetCredentialCancellationException -> {
                Toast.makeText(this, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            }

            is NoCredentialException -> {
                showAccountSetupPrompt()
            }

            is GetCredentialProviderConfigurationException -> {
                Toast.makeText(
                    this,
                    "Configuration error. Please check app settings.",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                Toast.makeText(
                    this,
                    "Authentication failed: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    loader.dialog.dismiss()
                    navigateToProfile()
                } else {
                    loader.dialog.dismiss()
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun showAccountSetupPrompt() {

        val accountManager = AccountManager.get(this)
        accountManager.addAccount(
            "com.google",
            null,
            null,
            null,
            this,
            { future ->
                try {
                    val result = future.result
                    val accountName = result.getString(AccountManager.KEY_ACCOUNT_NAME)
                    if (accountName != null) {
                        loader.title.text = "Fetching account.."
                        loader.dialog.show()

                        binding.continueWithGoogle.performClick()

                    } else {
                        Toast.makeText(
                            this,
                            "You need to add a Google account first.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        "You cancelled the account setup.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            null
        )
    }

    private fun navigateToProfile() {
        FirebaseAuth.getInstance().uid?.let {
            userCollection.document(it).get()
                .addOnSuccessListener { doc ->
                    Toast.makeText(this, "Sign-in successful", Toast.LENGTH_SHORT).show()

                    if (doc.exists() && doc.getBoolean("isProfileSetupComplete") == true) {
                        getSharedPreferences("VanamPrefs", MODE_PRIVATE).edit()
                            .putBoolean("isProfileComplete", true).commit()

                        startActivity(Intent(this, Dashboard::class.java))
                        finish()
                    } else {
                        startActivity(Intent(this, BasicActivity::class.java))
                        finish()
                    }
                }
        }
    }
}