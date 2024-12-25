package com.example.streamease

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.example.streamease.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the database helper
        firebaseAuth=FirebaseAuth.getInstance()
        // Set up the login button
        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (validateInput(email, password)) {
                firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity2::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Show a detailed error for invalid credentials
                        binding.loginPassword.error = "Invalid email or password"
                    }
                }
            }
        }

        // Redirect to the Signup page
        binding.signupRedirectText.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
        }


    }


    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        // Validate email field
        if (TextUtils.isEmpty(email)) {
            binding.loginEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginEmail.error = "Enter a valid email"
            isValid = false
        }

        // Validate password field
        if (TextUtils.isEmpty(password)) {
            binding.loginPassword.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        if (firebaseAuth.currentUser!=null){
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
            finish()
        }
    }
}
