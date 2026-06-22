package com.example.domain

import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class GenerateContentRequest(
    val systemInstruction: Content? = null,
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val thinkingConfig: ThinkingConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

@Serializable
data class GeneratedLessonData(
    val summary: String,
    val key_concepts: String,
    val important_notes: String,
    val formulas: String,
    val examples: List<GeneratedExample>,
    val practice_questions: List<GeneratedPracticeQuestion>,
    val quiz_questions: List<GeneratedQuizQuestion>
)

@Serializable
data class GeneratedExample(
    val question: String,
    val step_by_step_solution: String
)

@Serializable
data class GeneratedPracticeQuestion(
    val difficulty: String,
    val question: String,
    val correct_answer: String,
    val explanation: String
)

@Serializable
data class GeneratedQuizQuestion(
    val question: String,
    val options: List<String>,
    val correct_option_index: Int
)

class AITutor {
    suspend fun generateCompleteCurriculumForTopic(
        topicTitle: String,
        unitTitle: String,
        gradeName: String
    ): GeneratedLessonData? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
             return@withContext null
        }

        val systemPrompt = "You are an elite educational content developer for Ethiopian Grade 9 and 10 Mathematics curriculum. " +
            "Your task is to generate complete, high-quality, textbook-accurate pedagogical material for a specific topic. " +
            "You MUST respond ONLY with a clean, single JSON object containing:\n" +
            "1. 'summary': A comprehensive paragraph summarizing the core concepts of the topic.\n" +
            "2. 'key_concepts': A bulleted list or summary of key terminologies and concepts.\n" +
            "3. 'important_notes': Helpful tips, edge cases, common mistakes, or study advice.\n" +
            "4. 'formulas': Key mathematical equations, properties, or theorem statements formatted on newlines.\n" +
            "5. 'examples': A list of exactly 2 step-by-step example problems, each with 'question' and 'step_by_step_solution'.\n" +
            "6. 'practice_questions': A list of exactly 3 practice questions with 'difficulty' ('Easy', 'Medium', 'Hard'), 'question', 'correct_answer', and a step-by-step 'explanation'.\n" +
            "7. 'quiz_questions': A list of exactly 5 multiple choice questions with 'question', 'options' (exactly 4 options as a list of strings), and 'correct_option_index' (0 to 3).\n\n" +
            "Return valid JSON. Do not include any markdown format or text outside the JSON block. Do not wrap JSON in markdown block."

        val prompt = "Generate curriculum material for topic: '$topicTitle' in unit: '$unitTitle' ($gradeName Mathematics)."

        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user")),
            generationConfig = GenerationConfig(thinkingConfig = ThinkingConfig(thinkingLevel = "high"))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return@withContext null
            
            // Clean up Markdown wrapper if present
            val cleanJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<GeneratedLessonData>(cleanJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun askQuestion(topicContext: String, studentQuestion: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
             return@withContext "Error: Gemini API Key is missing. Please configure it in your Secrets panel."
        }

        val systemPrompt = "You are an expert Math tutor for Ethiopian Grade 9 and 10 students. " +
            "The current topic is: $topicContext. " +
            "Provide clear, step-by-step explanations. Encourage understanding rather than just giving the final answer."
            
        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            contents = listOf(Content(parts = listOf(Part(text = studentQuestion)), role = "user")),
            generationConfig = GenerationConfig(thinkingConfig = ThinkingConfig(thinkingLevel = "high"))
        )
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I am sorry, I couldn't formulate a response."
        } catch (e: Exception) {
            "Network Error: ${e.message}"
        }
    }
}
