package com.example.streamease

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.streamease.databinding.ActivityLoginBinding

class login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the database helper
        databaseHelper = DatabaseHelper(this)

        // Set up the login button
        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (validateInput(email, password)) {
                if (databaseHelper.checkEmailPassword(email, password)) {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Show a detailed error for invalid credentials
                    binding.loginPassword.error = "Invalid email or password"
                }
            }
        }

        // Redirect to the Signup page
        binding.signupRedirectText.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
        }

        // Forgot Password functionality (optional placeholder)
        binding.forgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot Password functionality coming soon!", Toast.LENGTH_SHORT).show()
            // Future Implementation: Navigate to ResetPassword activity
        }
    }

    /**
     * Validate the user input fields.
     * @param email: User's email
     * @param password: User's password
     * @return Boolean indicating if input is valid
     */
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
}
