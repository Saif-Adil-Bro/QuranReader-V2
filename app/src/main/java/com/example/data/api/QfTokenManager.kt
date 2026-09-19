package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class QfTokenManager {
    private var accessToken: String? = null
    private var expiresAt: Long = 0L
    private val client = OkHttpClient()

    private val AUTH_URL = "https://oauth2.quran.foundation/oauth2/token"

    @Synchronized
    fun getAccessToken(): String? {
        val currentTime = System.currentTimeMillis()
        // If token is valid and not expiring in the next 30 seconds, return it
        if (accessToken != null && expiresAt > currentTime + 30000) {
            return accessToken
        }

        // Fetch new token
        val clientId = BuildConfig.QURAN_FOUNDATION_CLIENT_ID
        val clientSecret = BuildConfig.QURAN_FOUNDATION_CLIENT_SECRET

        if (clientId.isEmpty() || clientSecret.isEmpty() || 
            clientId == "your_client_id_placeholder" || clientSecret == "your_client_secret_placeholder") {
            Log.e("QfTokenManager", "Client ID or Secret is empty or has placeholder values!")
            return null
        }

        val basicAuth = Credentials.basic(clientId, clientSecret)
        val body = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("scope", "content")
            .build()

        val request = Request.Builder()
            .url(AUTH_URL)
            .post(body)
            .header("Authorization", basicAuth)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("QfTokenManager", "Failed to get token: Code ${response.code} ${response.message}")
                    return null
                }
                val responseBody = response.body?.string() ?: return null
                val json = JSONObject(responseBody)
                val token = json.getString("access_token")
                val expiresInSeconds = json.getLong("expires_in")
                
                accessToken = token
                expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000)
                Log.d("QfTokenManager", "Successfully fetched new access token. Expires in $expiresInSeconds s")
                return token
            }
        } catch (e: Exception) {
            Log.e("QfTokenManager", "Exception while fetching token", e)
            return null
        }
    }

    @Synchronized
    fun clearToken() {
        accessToken = null
        expiresAt = 0L
    }
}
