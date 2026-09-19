package com.example.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun getTafsir(surahName: String, ayahNumber: Int, ayahText: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Provide a brief Tafsir (interpretation) in Bengali for Surah $surahName, Ayah $ayahNumber: \"$ayahText\". Keep it concise, respectful, and authentic based on classical Tafsir sources (e.g., Ibn Kathir)."
                val response = generativeModel.generateContent(prompt)
                if (response.text.isNullOrBlank()) {
                    Result.failure(Exception("Failed to generate Tafsir"))
                } else {
                    Result.success(response.text!!)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun askQuestionAboutAyah(surahName: String, ayahNumber: Int, ayahText: String, question: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Context: Surah $surahName, Ayah $ayahNumber: \"$ayahText\".\n\nQuestion: $question\n\nAnswer in Bengali based on Islamic teachings. Be respectful and concise."
                val response = generativeModel.generateContent(prompt)
                if (response.text.isNullOrBlank()) {
                    Result.failure(Exception("Failed to answer question"))
                } else {
                    Result.success(response.text!!)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
