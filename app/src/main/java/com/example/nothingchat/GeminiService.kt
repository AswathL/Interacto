package com.example.nothingchat

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService(private val apiKey: String) {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro", // Correct Gemini Model
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 16
            }
        )
    }

    suspend fun generateResponse(prompt: String, chatHistory: List<String>): String {
        return withContext(Dispatchers.IO) {
            try {
                val history = chatHistory.joinToString("\n") { "User: $it" }
                val fullPrompt = "$history\nUser: $prompt\nAI:"

                Log.d("GeminiService", "Sending prompt: $fullPrompt")

                val response = generativeModel.generateContent(fullPrompt)

                Log.d("GeminiService", "Received response: ${response.text}")

                response.text ?: "Sorry, I couldn't generate a response."
            } catch (e: Exception) {
                Log.e("GeminiService", "Error generating response: ${e.message}", e)
                "Error: ${e.message ?: "Unknown error"}"
            }
        }
    }
}
