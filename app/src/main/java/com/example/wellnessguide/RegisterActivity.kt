package com.example.wellnessguide

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val name = findViewById<EditText>(R.id.etName)
        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            val nameText = name.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val confirmText = confirmPassword.text.toString().trim()

            when {
                nameText.isBlank() || emailText.isBlank() || passwordText.isBlank() || confirmText.isBlank() -> {
                    Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
                }

                passwordText != confirmText -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }

                passwordText.length < 6 -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    auth.createUserWithEmailAndPassword(emailText, passwordText)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                                val user = hashMapOf(
                                    "uid" to uid,
                                    "name" to nameText,
                                    "email" to emailText,
                                    "createdAt" to System.currentTimeMillis()
                                )

                                db.collection("users").document(uid)
                                    .set(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Account created. Please log in.", Toast.LENGTH_SHORT).show()
                                        auth.signOut()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, e.message ?: "Profile save failed", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(this, task.exception?.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
        }

        findViewById<TextView>(R.id.txtLogin).setOnClickListener {
            finish()
        }
    }
}