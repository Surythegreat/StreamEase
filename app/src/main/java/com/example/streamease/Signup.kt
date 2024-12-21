package com.example.streamease

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.streamease.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth

class Signup : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseauth:FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseauth=FirebaseAuth.getInstance()
        binding.loginRedirectText.setOnClickListener { val intent = Intent(this,login::class.java)
            startActivity(intent) }
        // Password Strength Checker
        binding.signupPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                updatePasswordStrength(password)
            }
        })

        // Sign Up Button Click
        binding.signupButton.setOnClickListener {
            val email = binding.signupEmail.text.toString().trim()
            val password = binding.signupPassword.text.toString().trim()
            val confirmPassword = binding.signupConfirm.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are mandatory", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordStrong(password)) {
                Toast.makeText(this, "Password is too weak", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            firebaseauth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{
                if(it.isSuccessful){
                    val intent = Intent(this,login::class.java)
                    startActivity(intent)
                }else{
                    Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()

                }
            }
            // Perform sign-up logic here
            Toast.makeText(this, "Sign-Up Successful!", Toast.LENGTH_SHORT).show()
        }
    }

    // Update Password Strength
    private fun updatePasswordStrength(password: String) {
        val strengthText = when {
            password.length < 6 -> {
                binding.passwordStrength.setTextColor(Color.RED)
                "Weak"
            }
            password.length < 10 || !password.matches(".*[A-Za-z].*".toRegex()) || !password.matches(".*\\d.*".toRegex()) -> {
                binding.passwordStrength.setTextColor(Color.YELLOW)
                "Moderate"
            }
            else -> {
                binding.passwordStrength.setTextColor(Color.GREEN)
                "Strong"
            }
        }
        "Password Strength: $strengthText".also { binding.passwordStrength.text = it }
    }

    // Check if the password is strong
    private fun isPasswordStrong(password: String): Boolean {
        return password.length >= 8 &&
                password.matches(".*[A-Za-z].*".toRegex()) &&
                password.matches(".*\\d.*".toRegex()) &&
                password.matches(".*[!@#\$%^&*(),.?\":{}|<>].*".toRegex())
    }
}
