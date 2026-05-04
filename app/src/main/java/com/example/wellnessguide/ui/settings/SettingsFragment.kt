package com.example.wellnessguide.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.wellnessguide.LoginActivity
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.profile.LocalProfileImageStore
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedAvatar = "🌿"

    private lateinit var imgProfilePreview: ImageView
    private lateinit var btnChooseProfilePicture: MaterialButton
    private lateinit var btnRemoveProfilePicture: MaterialButton

    private lateinit var txtBiometricStatus: TextView
    private lateinit var btnBiometricToggle: MaterialButton

    private val securityPrefsName = "security_prefs"
    private val biometricEnabledKey = "biometric_enabled"

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                saveProfilePictureLocally(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        view.findViewById<TextView>(R.id.btnMenuSettings).setOnClickListener {
            (requireActivity() as MainActivity).openDrawer()
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val txtUserName = view.findViewById<TextView>(R.id.txtUserName)
        val txtUserEmail = view.findViewById<TextView>(R.id.txtUserEmail)
        val txtUserId = view.findViewById<TextView>(R.id.txtUserId)
        val txtDateCreated = view.findViewById<TextView>(R.id.txtDateCreated)
        val txtCurrentAvatar = view.findViewById<TextView>(R.id.txtCurrentAvatar)

        imgProfilePreview = view.findViewById(R.id.imgProfilePreview)
        btnChooseProfilePicture = view.findViewById(R.id.btnChooseProfilePicture)
        btnRemoveProfilePicture = view.findViewById(R.id.btnRemoveProfilePicture)

        txtBiometricStatus = view.findViewById(R.id.txtBiometricStatus)
        btnBiometricToggle = view.findViewById(R.id.btnBiometricToggle)

        val user = auth.currentUser

        if (user == null) {
            goToLogin()
            return view
        }

        txtUserEmail.text = "Email: ${user.email ?: "No email"}"
        txtUserId.text = "User ID: ${user.uid}"

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name")
                    ?: user.displayName
                    ?: user.email?.substringBefore("@")
                    ?: "User"

                val createdAt = document.getLong("createdAt") ?: 0L
                selectedAvatar = document.getString("avatar") ?: "🌿"

                txtUserName.text = "Name: $name"
                txtCurrentAvatar.text = "Current Avatar: $selectedAvatar"

                txtDateCreated.text = if (createdAt > 0L) {
                    val formatted = SimpleDateFormat(
                        "MMM dd, yyyy - hh:mm a",
                        Locale.getDefault()
                    ).format(Date(createdAt))

                    "Date Created: $formatted"
                } else {
                    "Date Created: Not available"
                }

                updateProfilePreview()
            }
            .addOnFailureListener {
                txtUserName.text = "Name: User"
                txtDateCreated.text = "Date Created: Not available"
                txtCurrentAvatar.text = "Current Avatar: 🌿"
                updateProfilePreview()
            }

        fun saveAvatar(avatar: String) {
            selectedAvatar = avatar
            txtCurrentAvatar.text = "Current Avatar: $avatar"

            LocalProfileImageStore.deleteImage(requireContext(), user.uid)
            updateProfilePreview()

            db.collection("users")
                .document(user.uid)
                .update("avatar", avatar)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Avatar updated", Toast.LENGTH_SHORT).show()
                    (requireActivity() as MainActivity).loadDrawerUserInfo()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        e.message ?: "Failed to update avatar",
                        Toast.LENGTH_LONG
                    ).show()

                    (requireActivity() as MainActivity).loadDrawerUserInfo()
                }
        }

        view.findViewById<TextView>(R.id.avatarLeaf).setOnClickListener {
            saveAvatar("🌿")
        }

        view.findViewById<TextView>(R.id.avatarHeart).setOnClickListener {
            saveAvatar("💙")
        }

        view.findViewById<TextView>(R.id.avatarMeditate).setOnClickListener {
            saveAvatar("🧘")
        }

        view.findViewById<TextView>(R.id.avatarMoon).setOnClickListener {
            saveAvatar("🌙")
        }

        view.findViewById<TextView>(R.id.avatarStrong).setOnClickListener {
            saveAvatar("💪")
        }

        view.findViewById<TextView>(R.id.avatarSmile).setOnClickListener {
            saveAvatar("😊")
        }

        btnChooseProfilePicture.setOnClickListener {
            imagePicker.launch("image/*")
        }

        btnRemoveProfilePicture.setOnClickListener {
            removeProfilePicture()
        }

        setupBiometricSection()

        view.findViewById<MaterialButton>(R.id.btnChangePassword).setOnClickListener {
            val email = user.email

            if (email.isNullOrBlank()) {
                Toast.makeText(requireContext(), "No email found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Password reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        e.message ?: "Failed to send reset email",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        return view
    }

    private fun saveProfilePictureLocally(uri: Uri) {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(requireContext(), "Please login first.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            LocalProfileImageStore.saveImage(
                context = requireContext(),
                userId = user.uid,
                uri = uri
            )

            updateProfilePreview()

            Toast.makeText(
                requireContext(),
                "Profile picture saved on this device.",
                Toast.LENGTH_SHORT
            ).show()

            (requireActivity() as MainActivity).loadDrawerUserInfo()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                e.message ?: "Failed to save profile picture.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun removeProfilePicture() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(requireContext(), "Please login first.", Toast.LENGTH_SHORT).show()
            return
        }

        LocalProfileImageStore.deleteImage(requireContext(), user.uid)

        updateProfilePreview()

        Toast.makeText(
            requireContext(),
            "Profile picture removed. Avatar icon will be used.",
            Toast.LENGTH_SHORT
        ).show()

        (requireActivity() as MainActivity).loadDrawerUserInfo()
    }

    private fun updateProfilePreview() {
        val user = auth.currentUser ?: return

        val imageFile = LocalProfileImageStore.imageFile(requireContext(), user.uid)

        if (imageFile.exists()) {
            imgProfilePreview.visibility = View.VISIBLE
            btnRemoveProfilePicture.visibility = View.VISIBLE

            Glide.with(this)
                .load(imageFile)
                .circleCrop()
                .into(imgProfilePreview)
        } else {
            imgProfilePreview.visibility = View.GONE
            btnRemoveProfilePicture.visibility = View.GONE
        }
    }

    private fun setupBiometricSection() {
        updateBiometricUi()

        btnBiometricToggle.setOnClickListener {
            if (isBiometricLoginEnabled()) {
                setBiometricLoginEnabled(false)

                Toast.makeText(
                    requireContext(),
                    "Biometric login turned off.",
                    Toast.LENGTH_SHORT
                ).show()

                updateBiometricUi()
            } else {
                verifyBeforeTurningOnBiometric()
            }
        }
    }

    private fun verifyBeforeTurningOnBiometric() {
        val biometricManager = BiometricManager.from(requireContext())

        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = biometricManager.canAuthenticate(authenticators)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                requireContext(),
                "No supported fingerprint, face unlock, PIN, pattern, or password found. Please set up your phone lock first.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    setBiometricLoginEnabled(true)

                    Toast.makeText(
                        requireContext(),
                        "Biometric login enabled.",
                        Toast.LENGTH_SHORT
                    ).show()

                    updateBiometricUi()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    Toast.makeText(
                        requireContext(),
                        "Biometric setup cancelled.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    Toast.makeText(
                        requireContext(),
                        "Not recognized. Try fingerprint, face unlock, or device lock.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometric Login")
            .setSubtitle("Verify with fingerprint, face unlock, or phone lock")
            .setDescription("This confirms that verification works on this device.")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun updateBiometricUi() {
        if (isBiometricLoginEnabled()) {
            txtBiometricStatus.text = "Status: Enabled"
            btnBiometricToggle.text = "Turn Off Biometric Login"
        } else {
            txtBiometricStatus.text = "Status: Disabled"
            btnBiometricToggle.text = "Turn On Biometric Login"
        }
    }

    private fun isBiometricLoginEnabled(): Boolean {
        val prefs = requireContext().getSharedPreferences(
            securityPrefsName,
            Context.MODE_PRIVATE
        )

        return prefs.getBoolean(biometricEnabledKey, false)
    }

    private fun setBiometricLoginEnabled(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences(
            securityPrefsName,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putBoolean(biometricEnabledKey, enabled)
            .apply()
    }

    private fun goToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }
}