package com.example.truthnudge

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.truthnudge.databinding.ActivitySignupBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser != null) { goHome(); return }
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            if (email.isBlank() || pass.length < 6) {
                showError("Enter valid email and password (min 6 chars)")
                return@setOnClickListener
            }
            binding.progress.isVisible = true
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { goHome() }
                .addOnFailureListener { showError(it.localizedMessage) }
                .addOnCompleteListener { binding.progress.isVisible = false }
        }

        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun showError(msg: String?) {
        Snackbar.make(binding.root, msg ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
    }
}
