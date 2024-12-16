package com.example.streamease

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.streamease.databinding.ActivityLoginBinding

class login : AppCompatActivity() {
    var binding: ActivityLoginBinding? = null
    var databaseHelper: DatabaseHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        databaseHelper = DatabaseHelper(this)

        binding!!.loginButton.setOnClickListener(View.OnClickListener {
            val email: String = binding!!.loginEmail.getText().toString()
            val password: String = binding!!.loginPassword.getText().toString()
            if (email == "" || password == "") Toast.makeText(
                this@login,
                "All fields are mandatory",
                Toast.LENGTH_SHORT
            ).show()
            else {
                val checkCredentials = databaseHelper!!.checkEmailPassword(email, password)

                if (checkCredentials == true) {
                    Toast.makeText(this@login, "Login Successfully!", Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(
                        applicationContext,
                        MainActivity::class.java
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(this@login, "Invalid Credentials", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })

        binding!!.signupRedirectText.setOnClickListener(View.OnClickListener {
            val intent = Intent(
                this@login,
                Signup::class.java
            )
            startActivity(intent)
        })
    }
}