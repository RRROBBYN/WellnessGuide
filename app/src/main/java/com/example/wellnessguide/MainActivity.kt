package com.example.wellnessguide

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.wellnessguide.profile.LocalProfileImageStore
import android.view.View
class MainActivity : AppCompatActivity() {

    lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navigationView.setupWithNavController(navHostFragment.navController)

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {

                // Main
                R.id.homeFragment -> {
                    navHostFragment.navController.navigate(R.id.homeFragment)
                }

                R.id.settingsFragment -> {
                    navHostFragment.navController.navigate(R.id.settingsFragment)
                }

                // Profile pages
                R.id.menu_my_profile -> {
                    navHostFragment.navController.navigate(
                        R.id.profileFeatureFragment,
                        bundleOf("pageType" to "my_profile")
                    )
                }

                R.id.menu_health_background -> {
                    navHostFragment.navController.navigate(
                        R.id.profileFeatureFragment,
                        bundleOf("pageType" to "health_background")
                    )
                }

                R.id.menu_lifestyle_profile -> {
                    navHostFragment.navController.navigate(
                        R.id.profileFeatureFragment,
                        bundleOf("pageType" to "lifestyle_profile")
                    )
                }



                // Assessment pages
                R.id.menu_start_wellness_check -> {
                    navHostFragment.navController.navigate(R.id.startWellnessCheckFragment)
                }

                R.id.menu_symptom_assessment -> {
                    navHostFragment.navController.navigate(R.id.assessmentFragment)
                }

                R.id.menu_lifestyle_check -> {
                    navHostFragment.navController.navigate(R.id.lifestyleCheckFragment)
                }

                R.id.menu_mental_wellness -> {
                    navHostFragment.navController.navigate(R.id.mentalWellnessFragment)
                }

                R.id.menu_sleep_tracker -> {
                    navHostFragment.navController.navigate(R.id.sleepTrackerFragment)
                }

                // Results / History
                R.id.menu_result_history -> {
                    navHostFragment.navController.navigate(R.id.historyFragment)
                }

                R.id.menu_latest_result -> {
                    navHostFragment.navController.navigate(
                        R.id.resultsFeatureFragment,
                        bundleOf("pageType" to "latest_result")
                    )
                }

                R.id.menu_recovery_plan -> {
                    navHostFragment.navController.navigate(
                        R.id.resultsFeatureFragment,
                        bundleOf("pageType" to "recovery_plan")
                    )
                }

                R.id.menu_health_summary -> {
                    navHostFragment.navController.navigate(
                        R.id.resultsFeatureFragment,
                        bundleOf("pageType" to "health_summary")
                    )
                }

                R.id.menu_download_report -> {
                    navHostFragment.navController.navigate(
                        R.id.resultsFeatureFragment,
                        bundleOf("pageType" to "download_report")
                    )
                }

                // Activity pages
                R.id.menu_daily_checkin -> {
                    navHostFragment.navController.navigate(
                        R.id.activityFeatureFragment,
                        bundleOf("pageType" to "daily_checkin")
                    )
                }

                R.id.menu_symptom_log -> {
                    navHostFragment.navController.navigate(
                        R.id.activityFeatureFragment,
                        bundleOf("pageType" to "symptom_log")
                    )
                }

                R.id.menu_sleep_log -> {
                    navHostFragment.navController.navigate(
                        R.id.activityFeatureFragment,
                        bundleOf("pageType" to "sleep_log")
                    )
                }

                R.id.menu_mood_stress -> {
                    navHostFragment.navController.navigate(
                        R.id.activityFeatureFragment,
                        bundleOf("pageType" to "mood_stress")
                    )
                }

                R.id.menu_physical_activity -> {
                    navHostFragment.navController.navigate(R.id.physicalActivityFragment)
                }

                R.id.menu_reminders -> {
                    navHostFragment.navController.navigate(R.id.remindersFragment)
                }

                // Safety pages
                R.id.menu_warning_signs -> {
                    navHostFragment.navController.navigate(
                        R.id.safetyInfoFragment,
                        bundleOf("pageType" to "warning")
                    )
                }

                R.id.menu_disclaimer -> {
                    navHostFragment.navController.navigate(
                        R.id.safetyInfoFragment,
                        bundleOf("pageType" to "disclaimer")
                    )
                }

                R.id.menu_consult_professional -> {
                    navHostFragment.navController.navigate(
                        R.id.safetyInfoFragment,
                        bundleOf("pageType" to "consult")
                    )
                }
                R.id.menu_credits -> {
                    navHostFragment.navController.navigate(
                        R.id.safetyInfoFragment,
                        bundleOf("pageType" to "credits")
                    )
                }
                // Other drawer items
                else -> {
                    openDynamicPage(
                        item.title.toString(),
                        getDrawerDescription(item.title.toString())
                    )
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        loadDrawerUserInfo()
    }

    private fun openDynamicPage(title: String, description: String) {
        navHostFragment.navController.navigate(
            R.id.dynamicDrawerFragment,
            bundleOf(
                "title" to title,
                "description" to description
            )
        )
    }

    private fun getDrawerDescription(title: String): String {
        return when (title) {
            "My Profile" -> "View and manage your personal wellness profile."
            "Health Background" -> "Add health conditions, allergies, medication, and risk background."
            "Lifestyle Profile" -> "Track sleep, water intake, stress, screen time, and activity habits."


            "Start Wellness Check" -> "Begin a guided wellness check."
            "Symptom Assessment" -> "Answer symptom questions such as severity, duration, location, and triggers."
            "Lifestyle Check" -> "Review lifestyle factors that may affect your wellness."
            "Mental Wellness" -> "Check mood, stress level, energy, and emotional wellness."
            "Sleep Tracker" -> "Track sleep duration, sleep quality, and sleep consistency."

            "Latest Result" -> "View your most recent wellness assessment result."
            "Assessment History" -> "Review previous assessments, wellness logs, statuses, tips, and recommendations."
            "Result History" -> "Review previous results and wellness status."
            "Recovery Plan" -> "Track recovery progress and follow-up actions."
            "Health Summary" -> "Generate a summary of symptoms, lifestyle factors, recommendations, and status."
            "Download Report" -> "Export or download your wellness summary report."

            "Daily Check-in" -> "Log your daily wellness condition."
            "Symptom Log" -> "View and add symptom logs."
            "Sleep Log" -> "Review sleep records and patterns."
            "Mood & Stress" -> "Track mood, stress, anxiety, tiredness, or overwhelm."
            "Physical Activity" -> "Monitor steps, daily movement, weekly summary, and route tracking."
            "Reminders" -> "See wellness reminders and follow-up suggestions."

            "Warning Signs" -> "Learn symptoms that may need urgent medical attention."
            "Disclaimer" -> "This app provides general wellness guidance only and is not a medical diagnosis."
            "Consult a Professional" -> "Know when it is safer to contact a healthcare professional."

            else -> "This section will be added soon."
        }
    }

    override fun onResume() {
        super.onResume()
        loadDrawerUserInfo()
    }

    fun openDrawer() {
        loadDrawerUserInfo()
        drawerLayout.openDrawer(GravityCompat.START)
    }

    fun loadDrawerUserInfo() {
        val headerView = navigationView.getHeaderView(0)

        val imgDrawerAvatar = headerView.findViewById<ImageView>(R.id.imgDrawerAvatar)
        val txtDrawerAvatar = headerView.findViewById<TextView>(R.id.txtDrawerAvatar)
        val txtDrawerName = headerView.findViewById<TextView>(R.id.txtDrawerName)

        val user = auth.currentUser

        if (user == null) {
            imgDrawerAvatar.visibility = View.GONE
            txtDrawerAvatar.visibility = View.VISIBLE
            txtDrawerAvatar.text = "🌿"
            txtDrawerName.text = "User"
            return
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val name = cleanFullName(
                    document.getString("name"),
                    user.email
                ) ?: cleanFullName(
                    document.getString("fullName"),
                    user.email
                ) ?: cleanFullName(
                    user.displayName,
                    user.email
                ) ?: "User"

                val avatar = document.getString("avatar") ?: "🌿"
                val localImageFile = LocalProfileImageStore.imageFile(this, user.uid)

                txtDrawerName.text = name

                if (localImageFile.exists()) {
                    txtDrawerAvatar.visibility = View.GONE
                    imgDrawerAvatar.visibility = View.VISIBLE

                    Glide.with(this)
                        .load(localImageFile)
                        .circleCrop()
                        .into(imgDrawerAvatar)
                } else {
                    imgDrawerAvatar.visibility = View.GONE
                    txtDrawerAvatar.visibility = View.VISIBLE
                    txtDrawerAvatar.text = avatar
                }
            }
            .addOnFailureListener {
                val name = cleanFullName(user.displayName, user.email) ?: "User"

                imgDrawerAvatar.visibility = View.GONE
                txtDrawerAvatar.visibility = View.VISIBLE
                txtDrawerName.text = name
                txtDrawerAvatar.text = "🌿"
            }
    }

    private fun cleanFullName(
        value: String?,
        email: String?
    ): String? {
        val text = value?.trim().orEmpty()

        if (text.isBlank()) return null
        if (text.contains("@")) return null

        val emailUsername = email?.substringBefore("@")?.trim()

        if (!emailUsername.isNullOrBlank() && text.equals(emailUsername, ignoreCase = true)) {
            return null
        }

        return text
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
