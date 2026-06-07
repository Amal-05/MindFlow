package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<ContentPart>,
    val systemInstruction: ContentPart? = null
)

@JsonClass(generateAdapter = true)
data class ContentPart(
    val parts: List<TextPart>
)

@JsonClass(generateAdapter = true)
data class TextPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<CandidatePart>?
)

@JsonClass(generateAdapter = true)
data class CandidatePart(
    val content: ContentPart?
)

@JsonClass(generateAdapter = true)
data class GeminiTaskSuggestions(
    val summary: String,
    val tasks: List<String>
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun analyzeImageOcr(base64Image: String, mimeType: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Gemini API key is not configured. Please add GEMINI_API_KEY in the Secrets panel."
        }
        
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        
        val json = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().apply {
                    put(JSONObject().put("text", "Perform OCR and extract all readable text, checklists, notes, names, amounts or items in this image. Organize it cleanly and output only the recognized text. Do not write a conversation or markdown headers."))
                    put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", mimeType)
                        put("data", base64Image)
                    }))
                })
            ))
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val responseJson = JSONObject(bodyString)
                    val candidates = responseJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val firstPart = parts?.optJSONObject(0)
                    firstPart?.optString("text")
                } else {
                    "Error executing Gemini scan: ${response.code} ${response.message}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Scanning error: ${e.localizedMessage}"
            }
        }
    }

    suspend fun interpretVoiceCommand(spokenText: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        val prompt = """
            You are a smart productivity assistant. Interpret this spoken voice command: "$spokenText"
            Decide if the user wants to:
            - CREATE_TASK (e.g. "remind me to...", "todo...", "schedule...")
            - CREATE_NOTE (e.g. "take a note of...", "record that...", "write down...")
            - SEARCH_ITEMS (e.g. "search for...", "find...", "show notes about...")
            - UNKNOWN
            
            Return a raw JSON string mapping this model exactly (do not output any markdown code blocks, conversational text or wrappers):
            {
              "action": "CREATE_TASK" | "CREATE_NOTE" | "SEARCH_ITEMS" | "UNKNOWN",
              "title": "A short concise title for the task/note/query",
              "details": "Extracted description or note content or query criteria",
              "priority": "High" | "Medium" | "Low",
              "tag": "Work" | "Personal" | "Study" | "Ideas"
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt)))),
            systemInstruction = ContentPart(parts = listOf(TextPart(text = "You are an intelligent NLP agent. Output strictly valid raw JSON with the specified schema.")))
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateRawText(prompt: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }
        val request = GeminiRequest(
            contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt))))
        )
        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun summarizeAndExtractTasks(noteTitle: String, noteContent: String): GeminiTaskSuggestions? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        val prompt = """
            Analyze this note and generate a concise summary and a list of actionable todo tasks (max 5 tasks).
            
            Note Title: $noteTitle
            Note Content: $noteContent
            
            Respond in this exact JSON format. Do not write any markdown code blocks or extra text outside JSON:
            {
              "summary": "Concise summary of the note.",
              "tasks": [
                "Task 1 description",
                "Task 2 description"
              ]
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(ContentPart(parts = listOf(TextPart(text = prompt)))),
            systemInstruction = ContentPart(parts = listOf(TextPart(text = "You are an intelligent note-taking companion. You output strictly raw JSON matching the requested schema.")))
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            parseGeminiOutput(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseGeminiOutput(rawText: String): GeminiTaskSuggestions? {
        var cleanText = rawText.trim()
        if (cleanText.startsWith("```json")) {
            cleanText = cleanText.substring(7)
        }
        if (cleanText.endsWith("```")) {
            cleanText = cleanText.substring(0, cleanText.length - 3)
        }
        cleanText = cleanText.trim()

        return try {
            val adapter = moshi.adapter(GeminiTaskSuggestions::class.java)
            adapter.fromJson(cleanText)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback manual regex parser in case JSON is slightly malformed or wrapped weirdly
            try {
                val summaryRegex = "\"summary\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val summary = summaryRegex.find(cleanText)?.groupValues?.getOrNull(1) ?: "No summary found."
                
                val tasksRegex = "\"tasks\"\\s*:\\s*\\[([^\\]]+)\\]".toRegex()
                val tasksMatch = tasksRegex.find(cleanText)?.groupValues?.getOrNull(1) ?: ""
                val tasksList = tasksMatch.split(",")
                    .map { it.replace("\"", "").trim() }
                    .filter { it.isNotBlank() }
                
                GeminiTaskSuggestions(summary, tasksList)
            } catch (inner: Exception) {
                inner.printStackTrace()
                null
            }
        }
    }
}
