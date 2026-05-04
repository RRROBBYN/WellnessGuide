package com.example.wellnessguide.ui.assessment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.wellnessguide.api.ClaudeRepository
import com.example.wellnessguide.data.db.SymptomEntity
import com.example.wellnessguide.data.db.WellnessDatabase
import kotlinx.coroutines.launch

class AssessmentViewModel(app: Application) : AndroidViewModel(app) {

    val analysisResult = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    val wellnessStatus = MutableLiveData<String>() // Green, Yellow, Red

    private val repository = ClaudeRepository()
    private val dao = WellnessDatabase.getDatabase(app).symptomDao()

    fun runAssessment(
        symptoms: String,
        severity: String,
        duration: String,
        location: String,
        lifestyle: String,
        profile: String
    ) {
        viewModelScope.launch {
            isLoading.value = true

            val userInput = """
                Symptoms: $symptoms
                Severity: $severity
                Duration: $duration
                Pain location: $location
                Lifestyle factors: $lifestyle
                Health profile: $profile
            """.trimIndent()

            val result = repository.analyzeSymptoms(userInput)
            analysisResult.value = result

            // Determine status from result text
            wellnessStatus.value = when {
                result.contains("Red", ignoreCase = true) -> "Red"
                result.contains("Yellow", ignoreCase = true) -> "Yellow"
                else -> "Green"
            }

            // Save to history
            dao.insert(
                SymptomEntity(
                    symptomName = symptoms,
                    severity = severity,
                    duration = duration,
                    location = location,
                    notes = result,
                    status = wellnessStatus.value ?: "Green"
                )
            )

            isLoading.value = false
        }
    }
}