package com.example.streamease

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.streamease.databinding.ActivitySignupBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Signup : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        binding.signupButton.setOnClickListener {
            val email = binding.signupEmail.text.toString().trim()
            val password = binding.signupPassword.text.toString().trim()
            val confirmPassword = binding.signupConfirm.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.all_fields_mandatory), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, getString(R.string.invalid_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perform database operations in background thread
            CoroutineScope(Dispatchers.IO).launch {
                val checkUserEmail = databaseHelper.checkEmail(email)

                if (!checkUserEmail) {
                    val insert = databaseHelper.insertData(email, password)

                    withContext(Dispatchers.Main) {
                        if (insert) {
                            Toast.makeText(this@Signup, getString(R.string.signup_successful), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@Signup, login::class.java))
                        } else {
                            Toast.makeText(this@Signup, getString(R.string.signup_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Signup, getString(R.string.user_exists), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.loginRedirectText.setOnClickListener {
            startActivity(Intent(this@Signup, login::class.java))
        }
    }
}
