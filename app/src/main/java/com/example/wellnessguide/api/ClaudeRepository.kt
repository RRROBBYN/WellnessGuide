package com.example.wellnessguide.api


class ClaudeRepository {

    private val systemPrompt = """
        You are a friendly Wellness Guide assistant. Analyze the user's symptoms, 
        lifestyle, and health profile. Provide:
        1. Possible causes
        2. Wellness status: Green / Yellow / Red
        3. Specific recommendations
        4. When to seek medical help
        Always end with: "This is not a medical diagnosis. Consult a healthcare 
        professional for severe or persistent symptoms."
        Be calm, clear, and supportive.
    """.trimIndent()

    suspend fun analyzeSymptoms(userInput: String): String {
        return try {
            val response = RetrofitClient.service.sendMessage(
                ClaudeRequest(
                    system = systemPrompt,
                    messages = listOf(ClaudeMessage("user", userInput))
                )
            )
            response.content.firstOrNull()?.text ?: "Unable to analyze. Please try again."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}