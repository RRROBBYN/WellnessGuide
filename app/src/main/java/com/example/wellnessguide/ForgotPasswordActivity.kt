package com.example.wellnessguide

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.etEmail)

        findViewById<MaterialButton>(R.id.btnResetPassword).setOnClickListener {
            val emailText = email.text.toString().trim()

            if (emailText.isBlank()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(emailText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, task.exception?.message ?: "Failed to send reset email", Toast.LENGTH_LONG).show()
                    }
                }
        }

        findViewById<TextView>(R.id.txtBackToLogin).setOnClickListener {
            finish()
        }
    }
}