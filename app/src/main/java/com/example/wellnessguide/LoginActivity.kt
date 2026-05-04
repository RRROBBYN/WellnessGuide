package com.example.wellnessguide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val securityPrefsName = "security_prefs"
    private val biometricEnabledKey = "biometric_enabled"

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_login)

        // ---------------- Google Sign-In Setup ----------------
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // required for Firebase
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleButton = findViewById<MaterialButton>(R.id.btnGoogle)
        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
        // ------------------------------------------------------

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isBlank() || passwordText.isBlank()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (isBiometricLoginEnabled()) {
                            showBiometricPromptAfterLogin()
                        } else {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            goToMain()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Login failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        findViewById<TextView>(R.id.txtForgot).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        findViewById<TextView>(R.id.txtSignup).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // ✅ Remove the second click listener that showed "Google login soon"
        // It was overriding the real Google sign-in listener
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        // User logged in automatically after selecting account
                        Toast.makeText(this, "Welcome ${account.displayName}", Toast.LENGTH_SHORT).show()
                        goToMain()
                    } else {
                        Toast.makeText(
                            this,
                            "Firebase login failed: ${authTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isBiometricLoginEnabled(): Boolean {
        val prefs = getSharedPreferences(securityPrefsName, Context.MODE_PRIVATE)
        return prefs.getBoolean(biometricEnabledKey, false)
    }

    private fun showBiometricPromptAfterLogin() {
        val biometricManager = BiometricManager.from(this)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuthenticate = biometricManager.canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            auth.signOut()
            Toast.makeText(
                this,
                "Biometric or device lock verification is enabled, but no supported fingerprint, face unlock, PIN, pattern, or password is available.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@LoginActivity, "Identity verified", Toast.LENGTH_SHORT).show()
                    goToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    auth.signOut()
                    Toast.makeText(this@LoginActivity, "Verification cancelled.", Toast.LENGTH_LONG).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity, "Not recognized. Try fingerprint, face unlock, or device lock.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify your identity")
            .setSubtitle("Use fingerprint, face unlock, or your phone lock")
            .setDescription("This adds extra security after login.")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}