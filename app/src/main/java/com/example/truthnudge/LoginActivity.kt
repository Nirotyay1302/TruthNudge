package com.example.truthnudge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.truthnudge.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Ensure any lines like "import androidx.privacysandbox.tools.core.generator.build" are GONE.
// Ensure any lines like "private val tools: Any" (outside the class) are GONE.

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth: FirebaseAuth by lazy { Firebase.auth } // Use KTX version
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser != null) {
            goHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            if (email.isBlank() || pass.isBlank()) {
                showError("Enter email and password")
                return@setOnClickListener
            }
            signInWithEmail(email, pass)
        }

        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun signInWithEmail(email: String, pass: String) {
        binding.progress.isVisible = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { goHome() }
            .addOnFailureListener { showError(it.localizedMessage ?: "Sign in failed") }
            .addOnCompleteListener { binding.progress.isVisible = false }
    }

    private fun signInWithGoogle() {
        binding.progress.isVisible = true

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id)) // Make sure this string resource exists
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity,
                )
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val googleIdToken = credential.idToken
                    firebaseAuthWithGoogle(googleIdToken)
                } else {
                    showError("Unexpected credential type")
                    binding.progress.isVisible = false
                }

            } catch (e: GetCredentialException) {
                Log.e("LoginActivity", "GetCredentialException: ${e.message}", e)
                showError(e.message ?: "Google Sign-In failed")
                binding.progress.isVisible = false
            } catch (e: Exception) {
                Log.e("LoginActivity", "Google Sign-In Exception: ${e.message}", e)
                showError(e.message ?: "An unexpected error occurred during Google Sign-In.")
                binding.progress.isVisible = false
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                goHome()
            }
            .addOnFailureListener {
                showError(it.localizedMessage ?: "Firebase auth with Google failed")
            }
            .addOnCompleteListener {
                binding.progress.isVisible = false
            }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun showError(msg: String?) {
        Snackbar.make(binding.root, msg ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
    }
}
