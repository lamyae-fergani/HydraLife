package com.example.hydralife

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiApiService {

    private val tag = "HydraLifeGemini"
    private val perfTag = "HydraLifePerf"

    // Reuse a single optimized OkHttpClient instance with connection pooling
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun generateContent(apiKey: String, prompt: String): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val hasKey = apiKey.isNotEmpty()
        if (!hasKey) {
            throw IllegalArgumentException("Gemini API key is missing. Please configure GEMINI_API_KEY in local.properties.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/interactions"

        // 1. Try primary model: gemini-3.8-flash
        try {
            val result = callGeminiOnce(apiKey, url, "gemini-3.8-flash", prompt)
            val duration = System.currentTimeMillis() - startTime
            Log.d(perfTag, "AI Response Success | Model: gemini-3.8-flash | Latency: ${duration}ms | Streaming: false")
            return@withContext result
        } catch (e: Exception) {
            val primaryDuration = System.currentTimeMillis() - startTime
            Log.w(perfTag, "Primary model (gemini-3.8-flash) failed after ${primaryDuration}ms: ${e.localizedMessage}. Switching promptly to fallback model: gemini-3.5-flash-lite")

            // 2. Prompt automatic fallback to secondary model: gemini-3.5-flash-lite
            val fallbackStartTime = System.currentTimeMillis()
            try {
                val result = callGeminiOnce(apiKey, url, "gemini-3.5-flash-lite", prompt)
                val fallbackDuration = System.currentTimeMillis() - fallbackStartTime
                Log.d(perfTag, "AI Response Success (Fallback) | Model: gemini-3.5-flash-lite | Latency: ${fallbackDuration}ms | Streaming: false")
                return@withContext result
            } catch (fallbackEx: Exception) {
                Log.e(perfTag, "Both primary and fallback models failed. Primary error: ${e.localizedMessage}, Fallback error: ${fallbackEx.localizedMessage}", fallbackEx)
                throw IOException("Gemini AI is experiencing high demand (HTTP 503). Both primary and backup models are currently unavailable. Please try again later.")
            }
        }
    }

    private fun callGeminiOnce(apiKey: String, url: String, modelName: String, prompt: String): String {
        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("input", prompt)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val httpStatus = response.code
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                return parseResponse(jsonResponse)
            }

            val sanitizedError = if (responseBody.length > 300) responseBody.substring(0, 300) + "..." else responseBody
            Log.e(tag, "API error (HTTP $httpStatus) for model $modelName: $sanitizedError")
            throw IOException("Gemini API error (HTTP $httpStatus): $sanitizedError")
        }
    }

    private fun parseResponse(jsonResponse: JSONObject): String {
        val extractedTexts = mutableListOf<String>()

        val steps = jsonResponse.optJSONArray("steps")
        if (steps != null && steps.length() > 0) {
            for (i in 0 until steps.length()) {
                val step = steps.optJSONObject(i) ?: continue

                val content = step.opt("content")
                if (content is JSONArray) {
                    for (j in 0 until content.length()) {
                        val contentItem = content.opt(j)
                        if (contentItem is JSONObject) {
                            val text = contentItem.optString("text")
                            if (text.isNotBlank() && !isInternalMetadata(text)) {
                                extractedTexts.add(text)
                            }
                        } else if (contentItem is String && contentItem.isNotBlank() && !isInternalMetadata(contentItem)) {
                            extractedTexts.add(contentItem)
                        }
                    }
                } else if (content is JSONObject) {
                    val text = content.optString("text")
                    if (text.isNotBlank() && !isInternalMetadata(text)) {
                        extractedTexts.add(text)
                    }
                } else if (content is String && content.isNotBlank() && !isInternalMetadata(content)) {
                    extractedTexts.add(content)
                }

                val stepOutput = step.optString("output")
                if (stepOutput.isNotBlank() && !isInternalMetadata(stepOutput)) {
                    extractedTexts.add(stepOutput)
                }
                val stepText = step.optString("text")
                if (stepText.isNotBlank() && !isInternalMetadata(stepText)) {
                    extractedTexts.add(stepText)
                }
            }
        }

        if (extractedTexts.isEmpty()) {
            if (jsonResponse.has("output")) {
                val output = jsonResponse.optString("output")
                if (output.isNotBlank() && !isInternalMetadata(output)) extractedTexts.add(output)
            }
            if (jsonResponse.has("response")) {
                val resp = jsonResponse.optString("response")
                if (resp.isNotBlank() && !isInternalMetadata(resp)) extractedTexts.add(resp)
            }
            if (jsonResponse.has("text")) {
                val txt = jsonResponse.optString("text")
                if (txt.isNotBlank() && !isInternalMetadata(txt)) extractedTexts.add(txt)
            }
        }

        if (extractedTexts.isNotEmpty()) {
            return extractedTexts.joinToString("\n\n").trim()
        }

        throw IOException("Failed to parse assistant response from server output structure.")
    }

    private fun isInternalMetadata(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("token_count") ||
                lower.contains("usage_metadata") ||
                lower.contains("model_version") ||
                lower.contains("model_output") ||
                lower.startsWith("id:") ||
                lower.contains("signature:") ||
                lower.contains("thought:")
    }
}
